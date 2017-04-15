/*
 * Copyright 2011-2017 David Crosson
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
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import scala.collection.JavaConversions._
import fr.janalyse.series._
import fr.janalyse.unittools._
import DurationTools._

import fr.janalyse.series.csv._

/**
  * Test cases
  * @author David Crosson
  */
class PerfSeriesTest  extends FunSuite {
  // ---------------------------------------------------------------------------  
  ignore("some performances test") {
    var x = Series[CalcCell]("X", "5s")

    val (buildDuration, _) = durationAsDesc { (1 to 1000) foreach {i => x<<=CalcCell(i*10,i)} }
    info("Build duration : %s - size=%d".format(buildDuration, x.size))

    val (computeDuration, _) = durationAsDesc { (1 to 100) foreach {i =>  x += i.toDouble } }
    info("compute duration : %s - size=%d".format(computeDuration, x.size))

    val (compositionDuration, _) = durationAsDesc { (1 to 10) foreach {i =>  x<<<=x } }
    info("composition duration : %s - size=%d".format(compositionDuration, x.size))

    val (oneBigCompositionDuration, _) = durationAsDesc { x<<<= ((1 to 3) map {_ => x}).toList }
    info("oneBig composition duration : %s - size=%d".format(oneBigCompositionDuration, x.size))
  }
 
  
  // ---------------------------------------------------------------------------
  val t1howmany=1000000L
  ignore(s"more performances test - $t1howmany cells") {
    val i0 = howlong() {
      var series = Series[CalcCell]("X", 1000L)
      (1L to t1howmany) foreach {t=> series <<=  t -> 1d}
    }
    info("With Series[CalcCell] :"+i0)

    val i1 = howlong() {
      var series = Series[StatCell]("X", 1000L)
      (1L to t1howmany) foreach {t=> series <<=  t -> 1d}
    }
    info("With Series[StatCell] :"+i1)

    val i2 = howlong() {
      var series = Series[AddCell]("Y", 1000L)
      (1L to t1howmany) foreach {t => series <<= t->1d}
    }
    info("With Series[AddCell] : "+i2)

    val i3 = howlong() {
      var series = Series[CountCell]("Z", 1000L)
      (1L to t1howmany) foreach {t => series <<= t->1d}
    }
    info("With Series[CountCell] : "+i3)
  }

  // ---------------------------------------------------------------------------
  val t2howmany=1000000
  ignore(s"basic performance test : $t2howmany cells added, period=1") {
    
    var x = Series[CalcCell]("X")
    val (buildDuration, _) = durationAsDesc { (1 to t2howmany) foreach {i => x<<=CalcCell(i*10,i)} }
    info("Reference duration (using Series)      : %s - reached size=%d".format(buildDuration, x.size))

    var refcoll = Vector.empty[CalcCell]
    val (refDuration, _) = durationAsDesc { (1 to t2howmany) foreach {i => refcoll:+=CalcCell(i*10,i)} }
    info("Reference duration (using Vector)      : %s - reached size=%d".format(refDuration, refcoll.size))

    /*var refbuf = collection.mutable.ArrayBuffer.empty[CalcCell]
    val (refDuration2, _) = durationAsDesc { (1 to t2howmany) foreach {i => refbuf:+=CalcCell(i*10,i)} }
    info("Reference duration (using ArrayBuffer) : %s - reached size=%d".format(refDuration2, refbuf.size))*/

  }
  
  // ---------------------------------------------------------------------------
  def doubler(start:Int):Stream[Int]=start#::doubler(2*start)  
  def mydoubler=doubler(1250)
  
  import java.util.{ArrayList=>JavaColl}
  import scala.collection.mutable.ArrayBuffer
  
  def genericIntensiveTest(testValues:Stream[Int])
                          (cellBuilder: (Int,Int) => CalcCell)
                          (vectorBuilder: (Vector[CalcCell], Int) => Vector[CalcCell])
                          (bufferBuilder: (ArrayBuffer[CalcCell], Int) => Unit)
                          (jvmCollBuilder: (JavaColl[CalcCell], Int) => Unit) {
    for {howmany <- testValues} {
      var x=Series[CalcCell]("X")
      val (buildDuration, _) = durationAsDesc { (1 to howmany) foreach {i => x<<=cellBuilder(howmany, i)} }
      info("Reference duration (using SERIES)            : %s - reached size=%d (immutable)".format(buildDuration, x.size))
    }
    
    
    // Compare with a immutable scala collection
    var refcoll = Vector.empty[CalcCell]
    val (refDuration, _) = durationAsDesc { (1 to testValues.last) foreach {i => 
      	refcoll = vectorBuilder(refcoll, i)
      }
    }
    info("Reference duration (using scala VECTOR)      : %s - reached size=%d (immutable)".format(refDuration, refcoll.size))

    
    // Compare with a mutable scala collection
    val mutcoll = new ArrayBuffer[CalcCell]()
    val (mutDuration, _) = durationAsDesc { (1 to testValues.last) foreach {i => 
       bufferBuilder(mutcoll, i)
      }
    }
    info("Reference duration (using scala ARRAYBUFFER) : %s - reached size=%d (but mutable, so not fair)".format(mutDuration, mutcoll.size))    


    // Compare with a mutable java collection
    val jvmcoll = new JavaColl[CalcCell]()
    val (jvmDuration, _) = durationAsDesc { (1 to testValues.last) foreach {i => 
      	jvmCollBuilder(jvmcoll, i)
      }
    }
    info("Reference duration (using java ARRAYLIST)    : %s - reached size=%d (but mutable, so not fair)".format(jvmDuration, jvmcoll.size))    
    
  }
  
  // ---------------------------------------------------------------------------
  ignore(s"prepend test") {
    //val testValues = mydoubler.take(14)
    val testValues = mydoubler.take(9)
    genericIntensiveTest(testValues) {
      (howmany, i) => CalcCell(howmany-i, i) } {
        (v, i) => CalcCell(i,i)+:v } {
          (v, i) => v.prepend(CalcCell(i,i)) } {
            (v, i) => v.add(0, CalcCell(i,i)) }
  }
  // ---------------------------------------------------------------------------
  ignore(s"middle insertion test") {
    // smaller, because of complexity and time increase too fast
    //val testValues = mydoubler.take(6)
    val testValues = mydoubler.take(6)
    genericIntensiveTest(testValues) {
      (howmany, i) => CalcCell(if (i%2==0) i else howmany-i, i) } {
        (v, i) => (v.take(i/2):+CalcCell(i,i))++v.drop(i/2) }  {
        //(v, i) => val (left,right) = v.splitAt(i/2) ; (left:+CalcCell(i,i))++right }  {
          (v, i) => v.insert(i/2, CalcCell(i,i)) } {
            (v, i) => v.add(i/2, CalcCell(i,i)) }
  }  
  // ---------------------------------------------------------------------------
  ignore(s"append test") {
    //val testValues = mydoubler.take(14)
    val testValues = mydoubler.take(14)
    genericIntensiveTest(testValues) { 
      (_, i) => CalcCell(i, i) } {
        (v, i) => v:+CalcCell(i,i) }  {
          (v, i) => v.append(CalcCell(i,i)) } {
            (v, i) => v.add(CalcCell(i,i))
        }
  }
  // ---------------------------------------------------------------------------
  ignore("csv read perf test") {
    def lgen:Stream[String] = "2011/03/20 04:00:00;10; 9; 8"#::lgen 
    def csvgen = "DateTime;A; B; C"#::lgen
    val howmany = 500000
    val csv = csvgen.take(howmany).mkString("\n")
    
    val (duration, s) = durationAsDesc { CSV2Series.fromString[AddCell](csv) }
    info("csv parse duration : %s - size=%d".format(duration, howmany))
  }
}
