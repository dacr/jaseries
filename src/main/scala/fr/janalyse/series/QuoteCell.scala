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

import java.text.SimpleDateFormat




/**
  * QuoteCell store stock quote data  
  */
case class QuoteCell(time:Long, close:Double, open:Double, low:Double, high:Double, volume:Option[Double]) extends Cell {
  def this(time:Long, close:Double) = this(time, close, close, close, close, None)
  def this(time:Long, close:Double, volume:Double) = this(time, close, close, close, close, Some(volume))
  val value = close
  override def toString =  "(%s -> %.2f-%.2f <%.2f >%.2f #%d)".format(new SimpleDateFormat("yy-MM-dd HH:mm:ss").format(time), open, close, low, high, volume map {_.toLong} getOrElse 0L)
  
  def emptyStatFragment:StatisticsFragment =
    StatisticsFragment(
          min          = low,
          max          = high,
          count        = 1,
          sum          = value,
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
          min   = if (f.min > low) low else f.min,
          max   = if (f.max < high) high else f.max,
          count = f.count + 1,
          sum   = f.sum + value,
          avg   =  f.avg*f.count/(f.count+1) + value/(f.count+1),
          weightAvg = f.weightAvg*prevTotWeight/curTotWeight + value*curWeight/curTotWeight,
          previousCell = this
          )
  }  
}

object QuoteCell  extends CellOrdering[QuoteCell] {
  implicit val cellBuilder = new CellBuilder[QuoteCell] {
      def buildFrom[T<%Number, V<%Number](t:T, v:V):QuoteCell =  throw new RuntimeException("Not Allowed for QuoteCell") //new QuoteCell(t.longValue, v.doubleValue)
      //override def buildFrom[X<:Cell](cell:X):QuoteCell = buildFrom(cell.time, cell)
      override def buildFrom[T<%Number, X<:Cell](t:T, cell:X) = cell match {
        case c:QuoteCell if t.longValue() == c.time => c
        case that:QuoteCell =>
          QuoteCell(time   = t.longValue,
                    close  = that.close,
                    open   = that.open,
                    low    = that.low,
                    high   = that.high,
                    volume = that.volume)
        case that =>
          throw new RuntimeException("Couldn't build a QuoteCell with an ordinary cell...")
      }
      override def buildFrom[X <: Cell](cell: X): QuoteCell = {
        cell match {
          case c:QuoteCell => c
          case c => throw new RuntimeException("Couldn't build a QuoteCell with an ordinary cell...")
        }
      }
      
      def merge[X<:Cell](cellA:QuoteCell, cellB:X):QuoteCell = cellB match {
        case that:QuoteCell =>
          QuoteCell(time   = cellA.time,
                    close  = if (that.time > cellA.time) that.close else cellA.close,
                    open   = if (that.time < cellA.time) that.open else cellA.open,
                    low    = if (that.low < cellA.low) that.low else cellA.low,
                    high   = if (that.high > cellA.high) that.high else cellA.high,
                    volume = for(s1 <-cellA.volume ; s2 <- that.volume) yield s1+s2 )
        case that =>
          throw new RuntimeException("Couldn't merge a QuoteCell with an ordinary cell...")
      }
    }  
}
