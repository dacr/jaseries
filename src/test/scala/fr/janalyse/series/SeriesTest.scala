/*
 * Copyright 2011-2014 David Crosson
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
import org.scalatest.ShouldMatchers
import scala.collection.JavaConversions._
import fr.janalyse.series._
import fr.janalyse.unittools._
import DurationTools._

import fr.janalyse.series.csv._

/**
  * Test cases
  * @author David Crosson
  */
class SeriesTest  extends FunSuite with ShouldMatchers {

  // ---------------------------------------------------------------------------
  test("Equalities tests") {
    val s1 = Series[AddCell]("x")  <<< List(10->2, 20->13)
    val s2 = Series[AddCell]("x")  <<< List(10->2, 20->13)
    val s3 = Series[AddCell]("y")  <<< List(10->2, 20->13)
    val s4 = Series[StatCell]("x") <<< List(10->2, 20->13)
    val s5 = s1 << 10->0
    
    s1 == s2 should equal(true)
    s1 != s3 should equal(true)
    s1 != s4 should equal(true)
    s1 == s5 should equal(true)
  }
  // ---------------------------------------------------------------------------
  test("Empty test") {
    val series = Series[StatCell]("x")
    series.size should equal(0)
    series.stat.sum   should equal(0)
    series.stat.max   should equal(0)
    series.stat.min   should equal(0)
    series.stat.avg   should equal(0)
    series.stat.count should equal(0)
  }
  // ---------------------------------------------------------------------------
  test("StatCell test") {
    val series = Series[StatCell]("x")  << 1-> 10 << 1->5 << 1->15
    val StatCell(time, value, count, min, max, sum) = series(0)
    time  should equal(1)
    value should equal(10)
    count should equal(3)
    min   should equal(5)
    max   should equal(15)
    sum   should equal(30)
  }

  // ---------------------------------------------------------------------------
  
  test("AddContent series test") {
    var s = Series[AddCell]("A", 1L)
    s = s << 1L->10d << 1L->20d << 2L->2d << 2L->4d << 2L->8d << 2L->6d
    s should have size (2)
    s map {_.value} should equal(List(30d, 20d))

    var s2 = s / 2 + 1
    s2 map {_.value} should equal(List(16d, 11d))
  }
  // ---------------------------------------------------------------------------
  
  test("AddContent series test (variant)") {
    val s = Series[AddCell]("A", 1L) <<< List(1->10, 1->20, 2->2, 2->4 , 2->8, 2->6)
    s.values should equal(List(30d, 20d))

    val s2 = s / 2 + 1
    s2.values should equal(List(16d, 11d))
  }

  // ---------------------------------------------------------------------------  
  test("Check the behavior with cell mutations : CountCell to CalcCell") {
      var w:Series[Cell] = Series[CountCell]("W") << 1->0
      
      w <<= 1->0
      w <<= 1->0
      w = w + 0
      
      w(0) should equal(CalcCell(1,3))
  }

  // ---------------------------------------------------------------------------  
  test("usage test 1") {
      var x = Series[CalcCell]("X")
      var y = Series[CountCell]("Y")

      x <<= CalcCell(1L, 10d)
      x <<= CalcCell(5L, 15)
      x <<= CalcCell(3L, 2)
      x <<= CalcCell(5, 8)
      x <<= 3->20

      x = x*2

      y <<= CountCell(3,20)
      y <<= CountCell(4,18)
      y <<= CountCell(4,2)

      val z:Series[CalcCell] = x<<<y

      val mySeries = List(x,y)

      mySeries match {
        case x:List[Series[Cell]] => 
        case _ => fail("Wrong type returned")
      }
  }

  // ---------------------------------------------------------------------------  
  test("more usage tests") {
    var series = Series[StatCell]("A", 1L)
    series = series << 1L->10d << 1L->20d << 2L->2d << 2L->4d << 2L->8d << 2L->6d
    info(series.toString)

    var seriesMax = series.extract(_.max)
    info(seriesMax.toString)
    seriesMax.values should equal(List(20, 8))

    var s = Series[AddCell]("B",10L)
    s = s << 1L->10d << 2L->20d << 10L->1d << 19L->5d
    var s2 = (s <<< s) << 22L->100d
    info(s2.toString)

    var s3 = s2 * 2 + 10
    info(s3.toString)
    info("max=%s min=%s sum=%f".format(s3.max, s3.min, 0d /*s3.sum*/))
    info("max=%s min=%s sum=%f".format(s3.stat.max, s3.stat.min, 0d/*s3.sum*/))

    s3.max.value should equal(s3.stat.max)
    s3.min.value should equal(s3.stat.min)
    
    s3 = -s3 / 2
    info(s3.toString)
  }
    
  // ---------------------------------------------------------------------------
  test("Checking cell type conversion #1") {
    var w:Series[Cell] = Series[CountCell]("W") << 1->0
    w <<= 1->0
    w <<= 1->0
    w = w + 0  // To check the behavior because here the CountCell become a CalcCell
    w.size should equal(1)
    w.values should equal(List(3))
    assert(w(0).isInstanceOf[CalcCell])
  }
  
  // ---------------------------------------------------------------------------
  test("Checking cell type conversion #2") {
    val x=Series[StatCell]("x") << 1L->1 << 2L->2
    val y =Series[StatCell]("y") << 1L->4 << 2L->10
    
    val z=Series[AddCell]("z") <<< List(x,y)
    z(0) should equal (AddCell(1,5d))
    z(1) should equal (AddCell(2,12d))
  }
  // ---------------------------------------------------------------------------
  test("delta & cumulate") {
    val x=Series[CalcCell]("x") <<< List(1->10, 2-> 15, 3->20)
    val d=x.delta
    val c=x.cumulate
    
    d.values should equal(List(5d,5d))
    d.times should equal(List(2L,3L))
    
    c.values should equal(List(10,25,45))
    c.times should equal(List(1,2,3))
  }
  // ---------------------------------------------------------------------------
  test("compact") {
    val data=List(10, 10, 20, 10, 10, 10, 30, 30, 20, 20, 20).zipWithIndex map {case (x,y) => y->x}
    val x=Series[CalcCell]("x") <<< data
    val cx=x.compact
    
    cx.values should equal(List(10,20,10,30,20))
    cx.times should equal(List(0,2,3,6,8))
  }
  
  // ---------------------------------------------------------------------------
  test("using foldleft") {
    val i = howlong(2L) { () =>
      var series = Series[AddCell]("Y", 1000L)
      (1L to 100000L) foreach {t => series <<= t->2d}
      val sum = ((0d /: series) {(sum, tup) => sum + tup.value})
      sum should equal(200000d)
    }
    info(i)
  }

  // ---------------------------------------------------------------------------
  test("UnikSeries") {
    case class FirstName(name:String)
    var unik = UnikSeriesMaker[FirstName]("Unik first names", 10)
    unik = unik.manage(1, FirstName("Toto"))
    unik = unik.manage(1, FirstName("Toto"))
    unik = unik.manage(1, FirstName("Tata"))
    unik = unik.manage(11, FirstName("Tata"))
    unik = unik.manage(12, FirstName("Tata"))
    unik.series.values should equal (List(2,1))
  }
  // ---------------------------------------------------------------------------
  test("BusySeriesMaker (Parallel tasks) tests") {
    var mk = BusySeriesMaker("workers", 10L)
    
    def toTuples = (mk.series.times zip mk.series.values)
    
    mk = mk.manage(15,15)
    mk.series should have size(2)
    toTuples should equal(List( 10->0.5d, 20->1d))

    mk = mk.manage(12,5)
    mk.series should have size(2)
    toTuples should equal(List( 10->1d, 20->1d))
    
    mk = mk.manage(5,35)
    mk.series should have size(4)
    toTuples should equal(List(0->0.5d, 10->2d, 20->2d, 30->1d))
    
    mk = mk.manage(18,10)
    mk.series should have size(4)
    toTuples should equal(List(0->0.5d, 10->2.2d, 20->2.8d, 30->1d))
    
    mk = mk.manage(25,2)
    mk.series should have size(4)
    toTuples should equal(List(0->0.5d, 10->2.2d, 20->3d, 30->1d))
    
  }

  // ---------------------------------------------------------------------------
  def seriesToTuples(mk:DistributionSeriesMaker) = (mk.series.times zip mk.series.values)
  def seriesToLongTuples(mk:DistributionSeriesMaker) = (mk.series.times zip mk.series.values.map(_.toLong))
  // ---------------------------------------------------------------------------
  test("DistributionSeriesMaker tests 1") {
    var mk = DistributionSeriesMaker("xyz", 10L)
	mk = mk.manage(10,20,1000) // 1000 to sample on  20ms
	mk.series should have size(2)
    seriesToTuples(mk) should equal(List(10->500, 20->500))
    mk = mk.manage(8,10,100)  // 100 to sample on  10ms
    mk.series should have size(3)
    seriesToTuples(mk) should equal(List(0->20, 10->580, 20->500))
  }
  // ---------------------------------------------------------------------------
  test("DistributionSeriesMaker tests 2") {
    var mk = DistributionSeriesMaker("xyz", "60s")	
	mk = mk.manage(120000,130000d,12288) // 12288 to sample on 130s
	mk.series should have size(3)
    seriesToLongTuples(mk) should equal(List(120000->5671,  180000->5671, 240000->945))    
  }
  // ---------------------------------------------------------------------------
  test("DistributionSeriesMaker tests 3") {
    var mk = DistributionSeriesMaker("xyz", "60s")
	mk = mk.manage(110000,130000d,12288) // 12288 to sample on  130s
	mk.series should have size(3)
    seriesToLongTuples(mk) should equal(List(60000->945, 120000->5671,  180000->5671))
  }
  // ---------------------------------------------------------------------------
  test("DistributionSeriesMaker tests 4") {
    var mk = DistributionSeriesMaker("xyz", "60s")
	mk = mk.manage(110000,150000d,987654321) // 987654321 to sample on  150s
	mk.series should have size(4)
    seriesToLongTuples(mk) should equal(List(60000->65843621, 120000->395061728, 180000->395061728, 240000->131687242))
  }
  // ---------------------------------------------------------------------------
  test("howlongFor tests") {
    val series = Series[StatCell]("x")  <<< 
    		List(10 -> 10,
    		     20 -> 5,
    		     30 -> 5,
    		     40 -> 10,
    		     50 -> 10,
    		     55 -> 10,
    		     60 -> 4
    		    )
    series.howlongFor(_>5) should equal(15)
    series.howlongFor(_>4) should equal(45)
  }
  
  // ---------------------------------------------------------------------------
  test("takeRight tests") {
    for(qs <- CSV2Series.quoteFromFile("samples/stockFromYahoo.csv")) {
      val last20d = qs.takeRight("20d")
      last20d.size should be < (qs.size)
      (last20d.last.time - last20d.head.time) should be <= ("20d".toDuration) 
    }
  }
  // ---------------------------------------------------------------------------
  test("computation with number no order test") {
    val s=Series[AddCell]("s") <<< List(1->2, 2->4, 3->6, 4->8)
    1+s should equal(s+1)
  }


  // ---------------------------------------------------------------------------
  test("Ordering test") {
    val x=Series[CalcCell]("x") <<< List(10->1, 20->1, 30->1, 50->1)
    val y=Series[CalcCell]("y") <<< List(40->2, 60->2, 30->1, 70->2)
    
    (x <<< y).map(_.time) should equal(List(10, 20, 30, 40, 50, 60, 70))
    (y <<< x).map(_.time) should equal(List(10, 20, 30, 40, 50, 60, 70))
    
    val z=Series[CalcCell]("z") <<< List(70->3, 20->3, 30->3, 10->3, 90->4)
     
    z.map(_.time) should equal(List(10, 20, 30, 70, 90))
    
    val w=Series[CalcCell]("w")
    (z <<< w).map(_.time) should equal(z.map(_.time))
    (w <<< z).map(_.time) should equal(z.map(_.time))
    (w <<< w) should have size(0)
    
    (x << 10->1).map(_.time) should equal(List(10,20,30,50))
    (x << 5->1).map(_.time)  should equal(List(5, 10,20,30,50))
    (x << 15->1).map(_.time) should equal(List(10,15,20,30,50))
    (x << 25->1).map(_.time) should equal(List(10,20,25,30,50))
    (x << 55->1).map(_.time) should equal(List(10,20,30,50,55))
    
    (z << 10->1).map(_.time) should equal(List(10, 20, 30, 70, 90))
    (z << 5->1).map(_.time)  should equal(List(5,  10, 20, 30, 70, 90))
    (z << 15->1).map(_.time) should equal(List(10, 15, 20, 30, 70, 90))
    (z << 25->1).map(_.time) should equal(List(10, 20, 25, 30, 70, 90))
    (z << 35->1).map(_.time) should equal(List(10, 20, 30, 35, 70, 90))
    (z << 75->1).map(_.time) should equal(List(10, 20, 30, 70, 75, 90))
    (z << 95->1).map(_.time) should equal(List(10, 20, 30, 70, 90, 95))


    val a = (x::y::z::Nil).reduceLeft(_ <<< _)
    val b = (z::x::y::Nil).reduceLeft(_ <<< _)
    val c = (y::z::x::Nil).reduceLeft(_ <<< _)
    val r = List(10, 20, 30, 40, 50, 60, 70, 90)

    a.map(_.time) should equal(r)
    b.map(_.time) should equal(r)
    c.map(_.time) should equal(r)
  }
  
  // ---------------------------------------------------------------------------
  
  test("true collection test ========> MORE WORK TO DO !!!!!!!!") {
    val sample = List(1L->2d, 2L->4d, 3L->6d, 4L->8d)
    val s1:Series[AddCell] = Series[AddCell]("s", 5) <<< sample
    val s2:Series[AddCell] = Series[AddCell]("s", 5) :+ AddCell(1L, 2D)
    //val s3:Series[AddCell] = Series[AddCell]("s", 5) ++  sample
    //val s4:Series[AddCell] = (sample :\ Series[AddCell]("s", 5)) { (next, acc) => acc :+ next}
    
    info(s1.toString)
    info(s2.toString)
    //info(s3.toString)
    //info(s4.toString)
  }
  
  // ---------------------------------------------------------------------------
  
  test("bestTimeRange tests") {
	  val sample=Series[CalcCell]("x") <<< List(
	      1L  -> 10d,
	      2L  -> 20d,
	      3L  -> 30d,
	      4L  -> 50d, 
	      5L  -> 75d,   // --> 
	      6L  -> 120d,  // Best time range for higher average (for sizeGoal=3)
	      7L  -> 75d,   // <--
	      8L  -> 50d,
	      9L  -> 30d,
	      10L -> 20d,
	      11L -> 10d
	      )
	  def greater(x:BestTimeRange, y:BestTimeRange) = if (x.value > y.value) x else y
	  
	  sample.bestTimeRange(3, _.stat.avg, greater, (_,_) => 1) should equal(Some(BestTimeRange(90d, TimeRange(5,7))))
  }  
}

