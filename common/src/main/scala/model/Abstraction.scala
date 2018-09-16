package model

trait SinkableType

trait Sinkable {
  def toCassandraable : Cassandraable
}

trait Cassandraable {
  def values: Array[AnyRef]
}
