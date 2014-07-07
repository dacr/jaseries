package fr.janalyse.series

case class TimeRange(
  from: Long,
  to: Long) {
  def size = to-from
}
