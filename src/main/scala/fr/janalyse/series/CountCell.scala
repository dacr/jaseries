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
  * CountCell type is to be use when you're not really interested by the cell
  * value but by how many time you've combined a new cell to the current one
  */
case class CountCell(time:Long, value:Double) extends CellWithDefaultStatisticsProvider

object CountCell  extends CellOrdering[CountCell] {
  implicit val cellBuilder = new CellBuilder[CountCell] {
      def buildFrom[T<%Number, V<%Number](t:T, v:V) = CountCell(t.longValue, 1)
      def merge[X<:Cell](cellA:CountCell, cellB:X) = cellB match {
        case e:CountCell => CountCell(cellA.time, cellA.value+cellB.value)
        case x => CountCell(cellA.time, cellA.value+1)
      }
      
      override def buildFrom[X <: Cell](cell: X): CountCell = {
        cell match {
          case c:CountCell => c
          case c => CountCell(c.time, c.value)
        }
      }

      override def buildFrom[T<%Number, X<:Cell](t:T,cell:X) = cell match {
        case c:CountCell if t.longValue() == c.time => c
        case e:CountCell => CountCell(t.longValue, cell.value)
        case x => CountCell(t.longValue, 1)
      }

  }  
}
