package common
import data._

object SparkImplicits {

  implicit val repositoryCassyJavaDriver = new CassandraRepositoryJavaDriver

}
