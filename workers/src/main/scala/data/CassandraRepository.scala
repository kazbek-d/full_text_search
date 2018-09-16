//package data
//
//import java.util.UUID
//
//import common.Settings.cassandraKeyspace
//import kamon.trace.Tracer
//import model.RestApi.FileNamesQL
//import model._



//class CassandraRepository extends RepositoryCassandraable {
//
//  // TODO: Move to JavaDriver (don't use Spark collection Api)
//  def getFileName(user: UUID, file_name: String, folder_name: String, destination_type: Int): Array[FileNameCassandra] = {
//    val data = sc.cassandraTable[FileNameCassandra](cassandraKeyspace, FileName.tableName)
//      .select(FileName.cols.map(ColumnName(_)): _*)
//      .where("user=?", user)
//      .where("file_name=?", file_name)
//      .where("folder_name=?", folder_name)
//      .where("destination_type=?", destination_type)
//      .collect()
//
//    data
//      .groupBy(_.modified)
//      .toList
//      .sortWith((x, y) => x._1.isAfter(y._1))
//      .headOption
//      .map(_._2.sortBy(_.chunk_pk_index))
//      .getOrElse(Array.empty)
//  }
//
//  // TODO: Move to JavaDriver (don't use Spark collection Api)
//  def getChunk(pk: UUID): Array[Chunk] =
//    sc.cassandraTable[Chunk](cassandraKeyspace, Chunk.tableName)
//      .select(Chunk.cols.map(ColumnName(_)): _*)
//      .where("pk=?", pk)
//      .collect()
//      .sortBy(_.chunk_index)
//
//  val emptyClient = new UUID(0L, 0L)
//  // TODO: Move to JavaDriver (don't use Spark collection Api)
//  // TODO: use all method's params
//  def getFileNames(pks: Seq[UUID]): FileNamesQL =
//  FileNamesQL {
//    pks.flatMap { pk =>
//      sc.cassandraTable[FileNameCassandra](cassandraKeyspace, FileName.tableName)
//        .select(FileName.cols.map(ColumnName(_)): _*)
//        .where("user=?", emptyClient)
//        .distinct
//        .collect().filter(_.chunk_pk == pk).map(_.toFileNameGraphQL)
//    }
//  }
//
//}
