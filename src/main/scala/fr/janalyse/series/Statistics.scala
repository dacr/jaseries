/*
 * Copyright 2011 David Crosson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.janalyse.series

import math._
import collection.immutable.TreeSet
import scala.collection.immutable.TreeMap

case class LinearApproximation(initial: Double = 0, slope: Double = 0) {
  def secondSlope = slope * 1000
  def minuteSlope = slope * 1000 * 60
  def hourSlope = slope * 1000 * 3600
  def daySlope = slope * 1000 * 3600 * 24
}

/**
 * Statistics datastore
 * @author David Crosson
 */
case class Statistics(
  count: Long,
  min: Double,
  max: Double,
  avg: Double,
  weightAvg: Double, // Moyenne pondérée par le temps séparant cette cellule de la suivante
  sum: Double,
  sd: Double,
  scaleFactor: Double,
  range: Double, // max - min
  delta: Double, // close - open
  open: Double,
  close: Double,
  percentile90: Double,
  alive: Long, // lastTime - firstTime
  linearApproximation: LinearApproximation)

case class StatisticsFragment(
  min: Double,
  max: Double,
  count: Long,
  sum: Double,
  avg: Double,
  weightAvg: Double,
  previousCell: Cell,
  openCell: Cell
)

trait StatisticsProvider {
  def emptyStatFragment: StatisticsFragment
  def adjust(fragment: StatisticsFragment): StatisticsFragment
}

/**
 * Series statistics data computation - TODO TO BE IMPROVED...
 * @author David Crosson
 */
object Statistics {

  val empty = Statistics(
    count = 0l, min = 0d, max = 0d,
    avg = 0d, weightAvg = 0d, sum = 0d,
    sd = 0d, range = 0d, scaleFactor = 0d,
    delta = 0, open = 0d, close = 0d,
    percentile90 = 0d, alive = 0l,
    linearApproximation = LinearApproximation())

  def apply(series: Series[Cell]): Statistics = {
    series.headOption match {
      case None => empty
      case Some(cell) =>
        //val frag = (cell.emptyStatFragment /: series.tail) {
        //  (statfrag, cell) => cell.adjust(statfrag)
        //}
        var orderedPercentileValueContainers:TreeMap[Double, Int]= new TreeMap[Double, Int]()
        var frag=cell.emptyStatFragment
        for(cell<-series.tail) {
          frag = cell.adjust(frag)
          orderedPercentileValueContainers = 
            orderedPercentileValueContainers + (cell.value -> (orderedPercentileValueContainers.getOrElse(cell.value, 0) + 1))
        }
        
        val closeCell = frag.previousCell
        val alive = closeCell.time - frag.openCell.time

        var n = ((frag.count - 1) * 90 / 100) + 1
        var percentile90 = 0d
        for ((value, count) <- orderedPercentileValueContainers if (n > 0)) {
          n -= count
          percentile90 = value
        }

        val (linearApproximation, sd) = computeLinearRegressionAndStandardDeviation(series, frag.avg)

        new Statistics(
          count = frag.count,
          min = frag.min,
          max = frag.max,
          sum = frag.sum,
          avg = frag.avg,
          weightAvg = frag.weightAvg,
          range = frag.max - frag.min,
          delta = closeCell.value - frag.openCell.value,
          open = frag.openCell.value,
          close = closeCell.value,
          percentile90 = percentile90,
          alive = alive,
          sd = sd,
          linearApproximation = linearApproximation,
          scaleFactor = 0d)

    }
  }

  private def computeLinearRegressionAndStandardDeviation(series: Series[Cell], avg: Double): Tuple2[LinearApproximation, Double] = {
    if (series.size == 0) return (LinearApproximation(), 0d)

    // DONE : Better precision because the time is a big value, as it is the ellapsed ms since seventies !
    val mc = new java.math.MathContext(50, java.math.RoundingMode.HALF_DOWN)

    var sumxx = BigDecimal(0)
    var sumyy = BigDecimal(0)
    var sumxy = BigDecimal(0)
    var sumx = BigDecimal(0)
    var sumy = BigDecimal(0)

    var sumdiff = 0d

    for (cell <- series) {
      val x = BigDecimal(cell.time)
      val y = BigDecimal(cell.value)
      sumx += x
      sumy += y
      sumxx += x * x
      sumyy += y * y
      sumxy += x * y
      sumdiff += (cell.value - avg) * (cell.value - avg)
    }

    val sd = if (series.size > 1) sqrt(sumdiff / (series.size - 1.0d)) else 0d

    val n = BigDecimal(series.size.toDouble);

    val Sxx = sumxx - (sumx * sumx)(mc) / n;
    val Syy = sumyy - (sumy * sumy)(mc) / n;
    val Sxy = sumxy - (sumx * sumy)(mc) / n;

    if (Sxx.toDouble == 0d) return (LinearApproximation(), sd)

    val bigB = Sxy(mc) / Sxx
    val bigA = (sumy - bigB * sumx)(mc) / n

    (LinearApproximation(bigA.toDouble, bigB.toDouble), sd)
  }
}



