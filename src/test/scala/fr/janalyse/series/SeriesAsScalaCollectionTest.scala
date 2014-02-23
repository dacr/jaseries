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

import java.io.File
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._
import fr.janalyse.series._
import fr.janalyse.unittools._
import fr.janalyse.tools.DurationTools._

import fr.janalyse.series.csv._

/**
  * Test cases
  * @author David Crosson
  */
@RunWith(classOf[JUnitRunner])
class SeriesAsScalaCollectionTest  extends FunSuite with ShouldMatchers {

  // ---------------------------------------------------------------------------
  test("Equalities tests") {
    val s1 = Series[AddCell]("x")  <<< Seq(10->2, 20->5, 30 -> 8)
    
    val s2 = s1.filter {c => c.value > 3}
    s2 should be equals ( Series[AddCell]("x") <<< Seq(20->5, 30 -> 8) )
    s2.getClass.getName should include("Series")
    
    val s3 = s1.map {c => CountCell(c.time, c.value)}
    s3 should be equals ( Series[AddCell]("x") <<< Seq(10-> 3, 20->6, 30 -> 9) )
    s3.getClass.getName should include("Series")
    info("s3 is now a Series[CountCell]")
    
    val s4 = s1.map {c => c.time -> (c.value+1)}
    s4 should be equals ( Series[AddCell]("x") <<< Seq(10-> 3, 20->6, 30 -> 9) )
    info("s4 is not any more a series, as map is not returning cells...")
  }
  
  
}

