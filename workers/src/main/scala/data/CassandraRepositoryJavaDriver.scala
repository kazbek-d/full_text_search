package data

import java.lang.System.nanoTime
import java.util.UUID

import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import common.Settings.{cassandraAddress, cassandraKeyspace}
import kamon.trace.Tracer
import model.RestApi.FileNamesQL
import model.{Chunk, ChunkText, FileName, FileNameCassandra}
import org.joda.time.{DateTime, DateTimeZone}

import scala.annotation.tailrec
import scala.collection.JavaConverters._


class CassandraRepositoryJavaDriver {

  private def profile[R](code: => R, t: Long = nanoTime): (R, Long) = (code, nanoTime - t)

  val nrOfCacheEntries: Int = 100
  val poolingOptions = new PoolingOptions
  val cluster: Cluster = Cluster.builder()
    .addContactPoints(cassandraAddress: _*)
    .withPoolingOptions(poolingOptions)
    .build()
  val session: Session = cluster.newSession()
  val cache: LoadingCache[String, PreparedStatement] =
    CacheBuilder.newBuilder().
      maximumSize(nrOfCacheEntries).
      build(
        new CacheLoader[String, PreparedStatement]() {
          def load(key: String): PreparedStatement = session.prepare(key.toString)
        }
      )


  def luceneChunksText(queryText: String): Seq[UUID] = {
    val rs = session.execute(
      s"""
         |SELECT DISTINCT pk FROM file_io.chunks_text WHERE expr(chunks_text_index, '{
         |   filter: {
         |      type: "boolean",
         |      must: [
         |         {type: "wildcard", field: "txt", value: "$queryText"}
         |      ]
         |   }
         |}')
         |limit 100;
    """.stripMargin).iterator()

    @tailrec
    def loop(xs: List[UUID]): List[UUID] =
      if (rs.hasNext) {
        val row = rs.next()
        loop(UUID.fromString(row.getString("pk")) :: xs)
      }
      else xs

    loop(List.empty[UUID])
  }


  object CassandraObject {

    def toDateTime(row: Row, col: String): Option[DateTime] =
      if (row.getObject(col) == null) None
      else Some(new DateTime(row.getTimestamp(col).getTime, DateTimeZone.getDefault))

    def toUUID(row: Row, col: String): Option[UUID] =
      if (row.getObject(col) == null) None
      else Some(row.getUUID(col))

    def toString(row: Row, col: String): Option[String] =
      if (row.getObject(col) == null) None
      else Some(row.getString(col))

    def toBigDecimal(row: Row, col: String): Option[BigDecimal] =
      if (row.getObject(col) == null) None
      else Some(row.getDecimal(col))


    def getFileName(cache: LoadingCache[String, PreparedStatement], session: Session)
                   (user: UUID, fileName: String, folderName: String, destinationType: Int): Array[FileNameCassandra] = {
      val query: Statement =
        QueryBuilder.select().
          all().
          from(cassandraKeyspace, FileName.tableName).
          where(QueryBuilder.eq("user", QueryBuilder.bindMarker())).
          and(QueryBuilder.eq("file_name", QueryBuilder.bindMarker())).
          and(QueryBuilder.eq("folder_name", QueryBuilder.bindMarker())).
          and(QueryBuilder.eq("destination_type", QueryBuilder.bindMarker()))

      session.execute(cache.get(query.toString).bind(user, fileName, folderName, destinationType.asInstanceOf[AnyRef])).all()
        .asScala.map(row => FileNameCassandra(
        row.getUUID("user"),
        row.getString("file_name"),
        row.getString("folder_name"),
        row.getInt("chunk_pk_index"),
        row.getUUID("chunk_pk"),
        toDateTime(row, "modified").get,
        row.getInt("destination_type"),
        row.getLong("content_length"),
        row.getInt("crc"))).to[Array]
        .groupBy(_.modified)
        .toList
        .sortWith((x, y) => x._1.isAfter(y._1))
        .headOption
        .map(_._2.sortBy(_.chunk_pk_index))
        .getOrElse(Array.empty)
    }

    val getFileName: (UUID, String, String, Int) => Array[FileNameCassandra] = getFileName(cache, session)


    def getChunk(cache: LoadingCache[String, PreparedStatement], session: Session)(pk: UUID): Array[Chunk] = {
      val query: Statement =
        QueryBuilder.select().
          all().
          from(cassandraKeyspace, Chunk.tableName).
          where(QueryBuilder.eq("pk", QueryBuilder.bindMarker()))

      session.execute(cache.get(query.toString).bind(pk)).all().asScala.map(row => Chunk(
        row.getUUID("pk"),
        row.getInt("chunk_index"),
        row.getInt("length"),
        row.getBytes("bytes").array()))
        .sortBy(_.chunk_index).to[Array]
    }

    val getChunk: (UUID) => Array[Chunk] = getChunk(cache, session)


    def getFileNames(cache: LoadingCache[String, PreparedStatement], session: Session)(user: UUID): Array[FileNameCassandra] = {
      val query: Statement =
        QueryBuilder.select().
          all().
          from(cassandraKeyspace, FileName.tableName).
          where(QueryBuilder.eq("user", QueryBuilder.bindMarker()))

      session.execute(cache.get(query.toString).bind(user)).all().asScala.map(row => FileNameCassandra(
        row.getUUID("user"),
          row.getString("file_name"),
          row.getString("folder_name"),
          row.getInt("chunk_pk_index"),
          row.getUUID("chunk_pk"),
          toDateTime(row, "modified").get,
          row.getInt("destination_type"),
          row.getLong("content_length"),
          row.getInt("crc")))
        .distinct.to[Array]
    }

    val getFileNames: (UUID) => Array[FileNameCassandra] = getFileNames(cache, session)
  }


  def setChunk(chunk: Chunk): Unit =
    Tracer.withNewContext("CassandraRepository___setChunk", autoFinish = true) {
      session.execute(QueryBuilder.insertInto(cassandraKeyspace, Chunk.tableName).values(Chunk.cols, chunk.values))
    }

  def setChunkText(chunkText: ChunkText): Unit =
    Tracer.withNewContext("CassandraRepository___setChunkText", autoFinish = true) {
      session.execute(QueryBuilder.insertInto(cassandraKeyspace, ChunkText.tableName).values(ChunkText.cols, chunkText.values))
    }

  def setFileName(fileName: FileNameCassandra): Unit =
    Tracer.withNewContext("CassandraRepository___setFileName", autoFinish = true) {
      session.execute(QueryBuilder.insertInto(cassandraKeyspace, FileName.tableName).values(FileName.cols, fileName.values))
    }

  def getFileName(user: UUID, fileName: String, folderName: String, destinationType: Int): Array[FileNameCassandra] =
    Tracer.withNewContext("CassandraRepositoryJavaDriver___getFileName", autoFinish = true) {
      CassandraObject.getFileName(user, fileName, folderName, destinationType)
    }

  def getChunk(pk: UUID): Array[Chunk] =
    Tracer.withNewContext("CassandraRepositoryJavaDriver___getChunk", autoFinish = true) {
      CassandraObject.getChunk(pk)
    }

  // TODO: use all method's params
  def getFileNames(uuids: Seq[UUID]): FileNamesQL =
    Tracer.withNewContext("CassandraRepositoryJavaDriver___getFileNames", autoFinish = true) {
      FileNamesQL {
        uuids.flatMap(pk => CassandraObject.getFileNames(new UUID(0L, 0L)).filter(_.chunk_pk == pk)).distinct.map(_.toFileNameGraphQL)
      }
    }

}
