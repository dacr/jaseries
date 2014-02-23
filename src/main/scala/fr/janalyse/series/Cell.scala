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
  * Series's cell root class, a cell is just a timestamp and a double value
  * @author David Crosson
  */
abstract class Cell extends StatisticsProvider {
  /**
    * time value, in most case will correspond to the java time, number of milliseconds ellapsed since seventies
    */
  def time:Long
  /**
    * the value of the cell
    */
  def value:Double
  /**
    * String representation of the cell
    */
  override def toString =  "(%s -> %.2f)".format(new SimpleDateFormat("yy-MM-dd HH:mm:ss").format(time), value)
}


object Cell   extends CellOrdering[Cell] 
