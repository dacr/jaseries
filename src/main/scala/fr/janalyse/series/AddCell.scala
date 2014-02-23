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
  * AddCell is used in order to add a new value to the current cell value
  * when combined, the new value is added to the previous one
  */
case class AddCell(time:Long, value:Double) extends CellWithDefaultStatisticsProvider

object AddCell    extends CellOrdering[AddCell] {
  implicit val cellBuilder = new CellBuilder[AddCell] {
      def buildFrom[T<%Number, V<%Number](t:T, v:V) = AddCell(t.longValue,v.doubleValue)
      def merge[X<:Cell](cellA:AddCell, cellB:X) = AddCell(cellA.time, cellA.value + cellB.value)
      
      override def buildFrom[X <: Cell](cell: X): AddCell = {
        cell match {
          case c:AddCell => c
          case c => AddCell(c.time, c.value)
        }
      }
      override def buildFrom[T <% Number, X <: Cell](time: T, cell: X): AddCell = {
        cell match {
          case c:AddCell if time.longValue() == c.time => c
          case c => AddCell(time.longValue(), c.value)
        }        
      }

  }  
}

