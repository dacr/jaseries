package fr.janalyse

import scala.collection.generic.CanBuildFrom
import scala.language.implicitConversions

package object series {

  type CSV2Series = fr.janalyse.series.csv.CSV2Series
  val CSV2Series = fr.janalyse.series.csv.CSV2Series

  type CSVFormat = fr.janalyse.series.csv.CSVFormat
  val CSVFormat = fr.janalyse.series.csv.CSVFormat

  final class RaisedForSeries[N <% Number, C <: Cell](n: N) {
    def *(x: Series[C]): Series[CalcCell] = x * n
    def +(x: Series[C]): Series[CalcCell] = x + n
    def /(x: Series[C]): Series[CalcCell] = x / n
    def -(x: Series[C]): Series[CalcCell] = x - n
  }
  implicit def raiseToSeries[N <% Number, C <: Cell](n: N): RaisedForSeries[N, C] = new RaisedForSeries[N, C](n)

  

}
