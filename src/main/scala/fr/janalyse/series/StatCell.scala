/*
 * Copyright 2011-2012-2013 David Crosson
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



/**
  * StatCell saved statistics about concerning all values added to the same cell
  * Average, maximum, minimum, slop, ... 
  */
case class StatCell(time:Long, value:Double, count:Long, min:Double, max:Double, sum:Double) extends Cell {
  def this(time:Long, value:Double) = this(time, value, 1, value, value, value)
  def avg = sum/count
  
  def emptyStatFragment:StatisticsFragment =
    StatisticsFragment(
          min          = min,
          max          = max,
          count        = count,
          sum          = sum,
          avg          = value,
          weightAvg    = value,
          openCell     = this,
          previousCell = this
    )
  def adjust(fragment:StatisticsFragment):StatisticsFragment = {
    val f=fragment
    val prevTotWeight = f.previousCell.time - f.openCell.time
    val curTotWeight  = time - f.openCell.time
    val curWeight     = time - f.previousCell.time
    fragment.copy(
          min   = if (f.min > min) min else f.min,
          max   = if (f.max < max) max else f.max,
          count = f.count + count,
          sum   = f.sum + sum,
          avg   = f.avg*f.count/(f.count+count) + value*count/(f.count+count),
          weightAvg = f.weightAvg*prevTotWeight/curTotWeight + value*curWeight/curTotWeight,
          previousCell = this
          )
  }
}

object StatCell   extends CellOrdering[StatCell] {
  implicit val cellBuilder = new CellBuilder[StatCell] {
      def buildFrom[T<%Number, V<%Number](t:T, v:V) = new StatCell(t.longValue,v.doubleValue)
      
      override def buildFrom[T<%Number, X<:Cell](t:T, cell:X) = cell match {
        case c:StatCell if t.longValue() == c.time => c
        case that:StatCell => StatCell(t.longValue,value=that.value, count=that.count, min=that.min, max=that.max, sum=that.sum)
        case that => new StatCell(t.longValue,that.value)
      }
      
      override def buildFrom[X <: Cell](cell: X): StatCell = {
        cell match {
          case c:StatCell => c
          case c => new StatCell(c.time, c.value)
        }
      }
      
      def merge[X<:Cell](cellA:StatCell, cellB:X):StatCell = cellB match {
        case that:StatCell =>
          StatCell(cellA.time,
                   (that.value+cellA.value*cellA.count)/(that.count+cellA.count),
                   that.count+cellA.count,
                   if (that.min < cellA.min) that.min else cellA.min,
                   if (that.max > cellA.max) that.max else cellA.max,
                   cellA.sum + that.sum)
        case that =>
          new StatCell(cellA.time,
                       (that.value+cellA.value*cellA.count)/(cellA.count+1),
                        cellA.count+1,
                        if (that.value < cellA.min) that.value else cellA.min,
                        if (that.value > cellA.max) that.value else cellA.max,
                        cellA.sum + that.value)
      }
    }  
}
