package model

import java.nio.file.{Path, Paths}
import java.util.UUID

import org.joda.time.DateTime

// https://github.com/Stratio/cassandra-lucene-index
// https://github.com/Stratio/cassandra-lucene-index/blob/branch-3.0.14/doc/documentation.rst



// CREATE KEYSPACE "file_io" WITH REPLICATION = { 'class' : 'SimpleStrategy' , 'replication_factor' :1 };

// CREATE TABLE file_io.chunks (pk uuid, chunk_index int, length int, bytes blob, PRIMARY KEY (pk, chunk_index));
case class Chunk(pk: UUID, chunk_index: Int, length: Int, bytes: Array[Byte]) extends Cassandraable {
  override def toString: String = s"Chunk: pk:$pk, index:$chunk_index, length:$length, data:${bytes.mkString(".")}"

  def toChunkText = ChunkText(pk, chunk_index, new String(bytes.take(length), java.nio.charset.Charset.forName("UTF-8")))

  override def values = Array[AnyRef](pk, chunk_index.asInstanceOf[AnyRef], length.asInstanceOf[AnyRef], java.nio.ByteBuffer.wrap(bytes))
}
object Chunk {
  val tableName = "chunks"
  val cols = Array("pk", "chunk_index", "length", "bytes")
}


/**

CREATE TABLE file_io.chunks_text (pk varchar, chunk_index int, txt varchar, PRIMARY KEY (pk, chunk_index));

DROP INDEX chunks_text_index

CREATE CUSTOM INDEX chunks_text_index ON file_io.chunks_text ()
USING 'com.stratio.cassandra.lucene.Index'
WITH OPTIONS = {
    'refresh_seconds' : '1',
    'schema' : '{
        fields : {
            pk   : {type : "string"},
            chunk_index : {type : "integer"},
            txt : {type : "text",  analyzer : "english"}
        }
    }'
};


SELECT * FROM file_io.chunks_text WHERE expr(chunks_text_index, '{
   query: {type: "phrase", field: "txt", value: "24,0", slop: 1}
}') LIMIT 100;

SELECT * FROM file_io.chunks_text WHERE expr(chunks_text_index, '{
   filter: [
      {type: "prefix", field: "txt", value: "specificity"}
   ]
}') LIMIT 100;



SELECT DISTINCT pk FROM file_io.chunks_text WHERE expr(chunks_text_index, '{
   filter: {
      type: "boolean",
      must: [
         {type: "wildcard", field: "txt", value: "first"}
      ]
   }
}')
limit 100;

  */
case class ChunkText(pk: UUID, chunk_index: Int, txt: String) extends Cassandraable {
  override def toString: String = s"ChunkText: pk:$pk, index:$chunk_index, data:$txt"

  override def values = Array[AnyRef](pk.toString, chunk_index.asInstanceOf[AnyRef], txt)
}
object ChunkText {
  val tableName = "chunks_text"
  val cols = Array("pk", "chunk_index", "txt")
}




/**

CREATE TABLE file_io.file_names (user uuid, file_name varchar, folder_name varchar, chunk_pk_index int, chunk_pk uuid, modified timestamp, destination_type int, content_length bigint, crc int, PRIMARY KEY (user, file_name, folder_name, destination_type, modified, chunk_pk));
Array[AnyRef](pk, chunk_index, txt)
DROP INDEX file_names_index

CREATE CUSTOM INDEX file_names_index ON file_names ()
USING 'com.stratio.cassandra.lucene.Index'
WITH OPTIONS = {
   'refresh_seconds': '1',
   'schema': '{
      fields: {
         user: {type: "string"},
         file_name: {type: "text", analyzer: "english"},
		     folder_name: {type: "text", analyzer: "english"},
         modified: {type: "date", pattern: "yyyy/MM/dd HH:mm:ss.SSS"},
         destination_type: {type: "integer"}
      }
   }'
};

SELECT * FROM file_names WHERE expr(file_names_index, '{
   filter: {
      type: "boolean",
      must: [
         {type: "wildcard", field: "user", value: "*000"},
         {type: "wildcard", field: "file_name", value: "js*"}
      ]
   },
   sort: {
      type: "date",
      field: "modified",
      reverse: true
   }
}');

  */
case class FileName(user: UUID, file_name: String, folder_name: String, destination_type: Int, chunks: Map[Int,UUID], content_length: Long, crc: Int) {
  override def toString: String = s"FileName: user:$user, fileName:$file_name, folderName:$folder_name, destination_type: $destination_type, chunks:${chunks.map(x => s"${x._1}->${x._2}").mkString(".")}, content_length:$content_length, crc:$crc"

  def toCassandraable = {
      val modified = DateTime.now()
      if(destination_type == FileName.saveToDataBase || destination_type == FileName.saveToDataBaseAndInTextFormat)
        chunks.map(row => FileNameCassandra(user, file_name, folder_name, row._1, row._2, modified, destination_type, content_length, crc))
      else if(destination_type == FileName.saveToFileSystem)
        FileNameCassandra(user, file_name, folder_name, 0, new UUID(0L, 0L), modified, destination_type, content_length, crc) :: Nil
      else
        Nil
    }
}
case class FileNameCassandra(user: UUID, file_name: String, folder_name: String, chunk_pk_index: Int, chunk_pk: UUID, modified: DateTime, destination_type: Int, content_length: Long, crc: Int) extends Cassandraable {

  def toFileNameMetaData = FileName(user, file_name, folder_name, destination_type, Map.empty, content_length, crc)
  def toFileNameGraphQL = FileNameGraphQL(file_name, destination_type, content_length)

  override def values = Array[AnyRef](user, file_name, folder_name,
    chunk_pk_index.asInstanceOf[AnyRef], chunk_pk, modified.getMillis.asInstanceOf[AnyRef], destination_type.asInstanceOf[AnyRef], content_length.asInstanceOf[AnyRef], crc.asInstanceOf[AnyRef])
}
case class FileNameGraphQL(file_name: String, destination_type: Int, content_length: Long)
object FileName {
  val tableName = "file_names"
  val cols = Array("user", "file_name", "folder_name", "chunk_pk_index", "chunk_pk", "modified", "destination_type", "content_length", "crc")

  val saveToDataBase = 0
  val saveToFileSystem = 1
  val saveToDataBaseAndInTextFormat = 3

  def dir(user: UUID, folderName: String) =
    Paths.get(s"${common.Settings.filesystemPath}/$user/${if (folderName.length == 0) "_" else folderName}")

  def path(dir: Path, fileName: String) =
    Paths.get(s"$dir/$fileName")

}