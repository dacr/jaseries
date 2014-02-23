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
  * CalcCell is the cell type resulting of any mathematical operation
  * when combined, the new value replace the previous one
  */
case class CalcCell(time:Long, value:Double) extends CellWithDefaultStatisticsProvider

object CalcCell   extends CellOrdering[CalcCell] {
  implicit val cellBuilder = new CellBuilder[CalcCell] {
      def merge[X<:Cell](cellA:CalcCell, cellB:X) = CalcCell(cellA.time, cellB.value)
      def buildFrom[T<%Number, V<%Number](t:T, v:V) = CalcCell(t.longValue,v.doubleValue)
      
      override def buildFrom[X <: Cell](cell: X): CalcCell = {
        cell match {
          case c:CalcCell => c
          case c => CalcCell(c.time, c.value)
        }
      }
      override def buildFrom[T <% Number, X <: Cell](time: T, cell: X): CalcCell = {
        cell match {
          case c:CalcCell if time.longValue() == c.time => c
          case c => CalcCell(time.longValue(), c.value)
        }        
      }
  }
}
