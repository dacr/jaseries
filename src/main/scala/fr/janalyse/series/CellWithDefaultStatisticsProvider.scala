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


abstract class CellWithDefaultStatisticsProvider extends Cell {
  def emptyStatFragment:StatisticsFragment =
    StatisticsFragment(
          min          = value,
          max          = value,
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
          min   = if (f.min > value) value else f.min,
          max   = if (f.max < value) value else f.max,
          count = f.count + 1,
          sum   = f.sum + value,
          avg   = f.avg*f.count/(f.count+1) + value/(f.count+1),
          weightAvg = f.weightAvg*prevTotWeight/curTotWeight + value*curWeight/curTotWeight,
          previousCell = this
          )
  }
}

object CellWithDefaultStatisticsProvider   extends CellOrdering[Cell] 
