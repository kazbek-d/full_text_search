//import java.util.UUID
//
//import com.datastax.driver.core.Cluster
//import common.Settings.cassandraAddress
//import model._
//import java.lang.System.nanoTime
//import scala.annotation.tailrec
//
//object QueryTest extends App {
//
//  def profile[R](code: => R, t: Long = nanoTime): (R, Long) = (code, nanoTime - t)
//
//  val queryText = "дерево"
//  val queryText1 = "деревоо"
//
//  val cluster = Cluster.builder()
//    .withClusterName("fullTextSearch")
//    .addContactPoint(cassandraAddress.mkString(","))
//    .build()
//
//  val session = cluster.connect
//
//  def Process = {
//    val rs = session.execute(
//      s"""
//         |SELECT * FROM file_io.chunks_text WHERE expr(chunks_text_index, '{
//         |   filter: {
//         |      type: "boolean",
//         |      must: [
//         |         {type: "wildcard", field: "txt", value: "$queryText"}
//         |      ]
//         |   }
//         |}')
//         |limit 100;
//    """.stripMargin).iterator()
//
//    @tailrec
//    def loop(xs: List[ChunkText]): List[ChunkText] =
//      if (rs.hasNext) {
//        val row = rs.next()
//        loop(ChunkText(UUID.fromString(row.getString("pk")), row.getInt("chunk_index"), row.getString("txt")) :: xs)
//      }
//      else xs
//
//    println(s"Data.count = ${loop(List.empty[ChunkText]).length}")
//    loop(List.empty[ChunkText]).foreach(println)
//  }
//
//  val (_, depTime1) = profile {
//    Process
//  }
//  println(depTime1)
//
//  val (_, depTime2) = profile {
//    Process
//  }
//  println(depTime2)
//
//  val (_, depTime3) = profile {
//    Process
//  }
//  println(depTime3)
//
//  val (_, depTime4) = profile {
//    Process
//  }
//  println(depTime4)
//
//}
