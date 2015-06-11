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
class CSVTest  extends FunSuite with ShouldMatchers {

  // ---------------------------------------------------------------------------
  
  test("Basic") {
    val csv="  date        \t time     \t A    \n"+
            "   2009/03/31 \t 02:00:00 \t 30.0 \n"
    val seriesList = CSV2Series.fromString[CalcCell](csv).values
    seriesList.size should equal(1)
  }

  // ---------------------------------------------------------------------------

  test("First") {
    val csv="""date, A
              |2009/03/31 02:00:00.100, 2
              |2009/03/31 03:00:00.100, 6
              |2009/03/31 04:00:00.100, 10""".stripMargin
    val seriesList = CSV2Series.fromString[CalcCell](csv).values
    seriesList.size should equal(1)
  }
  
  // ---------------------------------------------------------------------------

  test("Second") {
    val csv="""date; A
              |2009/03/31 02:00:00; 2
              |2009/03/31 03:00:00; 6
              |2009/03/31 04:00:00; 10""".stripMargin
    val seriesList = CSV2Series.fromString[CalcCell](csv).values
    seriesList.size should equal(1)
    seriesList.head.stat.sum should equal(18)
  }

  // ---------------------------------------------------------------------------

  test("bis") {
    val csv="""date; A
              |20-03-2011 02:00; 2
              |20-03-2011 03:00; 6
              |20-03-2011 04:00; 10""".stripMargin
    val seriesList = CSV2Series.fromString[CalcCell](csv).values
    seriesList.size should equal(1)
    seriesList.head.stat.sum should equal(18)
  }

  // ---------------------------------------------------------------------------

  test("empty cell") {
    val csv="""date; A
              |20-03-2011 02:00; 2
              |20-03-2011 03:00;
              |20-03-2011 04:00; 10""".stripMargin
    val seriesList = CSV2Series.fromString[CalcCell](csv).values
    seriesList.size should equal(1)
    seriesList.head.stat.count should equal(2)
    seriesList.head.stat.sum should equal(12)
  }


  // ---------------------------------------------------------------------------

  test("more-complex") {
    val csv="""  date ;  name ;   X
              |  2009/03/31 02:00:00 ; A; 2
              |2009/03/31 02:00:00 ; B; 2
              |2009/03/31 03:00:00; A; 6
              | 2009/03/31 03:00:00; B; 6
              |2009/03/31 04:00:00; A ; 10
              |2009/03/31 04:00:00; B ; 10
              |
              |""".stripMargin
    val seriesMap = CSV2Series.fromString[CalcCell](csv)
    seriesMap.size should equal(2)
    val ax=seriesMap("A-X")
    val bx=seriesMap("B-X")
    ax.stat.sum should equal(18)
    bx.stat.sum should equal(18)
  }
  
  // ---------------------------------------------------------------------------
  test("csv with quotes and separator at line end") {
    val csv=""""Date";"Fermeture";"Ouverture";"Valeur Haute";"Valeur Basse";"Volume";
              |"11/05/2007";"9,94";"9,93";"10,15";"9,58";"81679800";
              |"18/05/2007";"10,06";"10,02";"10,12";"9,73";"79926096";""".stripMargin
    val seriesMap = CSV2Series.fromString[CalcCell](csv)
    seriesMap should have size(5)
    seriesMap.values.head should have size(2)
  }
  
  // ---------------------------------------------------------------------------
  test("csv with variable column count") {
    val csv="""
      |date;             A
      |20-03-2011 02:00; 2
      |20-03-2011 03:00;
      |20-03-2011 04:00; 8
      |date;             A; B; C
      |20-03-2011 05:00; 1; 2; 3
      |20-03-2011 06:00; 2;  ; 5
      |date;                   C; D; E
      |20-03-2011 07:00;       7; 0;
      |
      |""".trim.stripMargin
      
    val series = CSV2Series.fromString[CalcCell](csv)
    series.size should equal(5)
    series.get("A").map(_.size) should equal(Some(4))
    series.get("A").map(_.stat.sum) should equal(Some(13))
    series.get("B").map(_.stat.sum) should equal(Some(2))
    series.get("C").map(_.stat.sum) should equal(Some(15))
    series.get("D").map(_.stat.sum) should equal(Some(0))
    series.get("E").map(_.size) should equal(Some(0))
  }
  
  // ---------------------------------------------------------------------------
  test("csv with variable column count - just get series names") {
    val csv="""
      |date;             A
      |20-03-2011 02:00; 2
      |20-03-2011 03:00;
      |20-03-2011 04:00; 8
      |date;             A; B; C
      |20-03-2011 05:00; 1; 2; 3
      |20-03-2011 06:00; 2;  ; 5
      |date;                   C; D; E
      |20-03-2011 07:00;       7; 0;
      |
      |""".trim.stripMargin
      
    val series = CSV2Series.namesFromString(csv)
    series.size should equal(5)
  }
  
  // ---------------------------------------------------------------------------
  test("CSV date format test") {
    import java.util.{GregorianCalendar, Calendar, Date}
    import Calendar._
    def cal(t:Long) = {
      val c=new GregorianCalendar
      c.setTime(new Date(t))
      c
    }
    val daycheck = {(t:Long) => cal(t).get(DAY_OF_MONTH) should equal(20)}
    val monthcheck = {(t:Long) => cal(t).get(MONTH) should equal(MARCH)}
    val yearcheck = {(t:Long) => cal(t).get(YEAR) should equal(2011)}
    val hourcheck = {(t:Long) => cal(t).get(HOUR_OF_DAY) should equal(17)}
    val minutecheck = {(t:Long) => cal(t).get(MINUTE) should equal(27)}
    val secondcheck = {(t:Long) => cal(t).get(SECOND) should equal(37)}
    val millicheck =  {(t:Long) => cal(t).get(MILLISECOND) should equal(456)}
    val dates2test=List(
       "2011-03-20" -> List(daycheck,monthcheck,yearcheck), 
       "20-03-2011" -> List(daycheck,monthcheck,yearcheck),
       "2011_03_20 17:27" -> List(daycheck,monthcheck,yearcheck,hourcheck,minutecheck),
       "20-03-2011 17:27:37" -> List(daycheck,monthcheck,yearcheck,hourcheck,minutecheck,secondcheck),
       "20/03/2011 17:27:37.456" -> List(daycheck,monthcheck,yearcheck,hourcheck,minutecheck,secondcheck,millicheck),
       "17:27" -> List(hourcheck,minutecheck),
       "17:27:37" -> List(hourcheck,minutecheck, secondcheck),
       "17:27:37.456" -> List(hourcheck,minutecheck, secondcheck, millicheck)
    )
    dates2test foreach {case (d2t, checks)=>
      val csv="date;x\n%s;123".format(d2t)
      info("Checking %s".format(d2t))
      CSV2Series.fromString[CalcCell](csv).get("x") match {
        case None =>
        case Some(x) => 
          x.size should equal(1)
          x.values should equal(List(123))
          checks foreach {chk => chk(x.head.time)}
      }
    }
  }
  
  // ---------------------------------------------------------------------------
  test("sample1") {
    val seriesMap = CSV2Series.fromFile[CalcCell]("samples/1.csv")
    seriesMap.size should equal(1)
    val (name, memUsage) = seriesMap.head
    memUsage.size should equal(3)
    memUsage.max.value  should equal(1.99d)
    memUsage.min.value  should equal(0.18d)
    memUsage.stat.sum   should equal(3d)
    memUsage.stat.max   should equal(1.99d)
    memUsage.stat.min   should equal(0.18d)
    memUsage.stat.avg   should equal(1d)
    memUsage.stat.count should equal(3L)
  }

  // ---------------------------------------------------------------------------
  test("sample2") {
    val seriesMap = CSV2Series.fromFile[CalcCell]("samples/2.csv")
    seriesMap.size should equal(9)
    val okSeries = seriesMap("www status 200 hit count")
    okSeries.size            should equal(255)
    okSeries.max.value       should equal(6845d)
    okSeries.min.value       should equal(1d)
    okSeries.stat.sum        should equal(154275d)
    okSeries.stat.max        should equal(6845d)
    okSeries.stat.min        should equal(1d)
    okSeries.stat.avg.floor  should equal(605d)
    okSeries.stat.count      should equal(255d)
    okSeries.stat.open       should equal(1d)
    okSeries.stat.close      should equal(260d)
  }
  
  // ---------------------------------------------------------------------------  
  test("stats with standard deviation") {
    val seriesMap = CSV2Series.fromFile[CalcCell]("samples/2.csv")
    seriesMap.size should equal(9)
    val okSeries = seriesMap("www status 200 hit count")
    okSeries.stat.sd.floor   should equal(655d)
  }
  
  // ---------------------------------------------------------------------------  
  test("series read write 1") {
    val csv="""DateTime;A
              |2011/03/20 02:00:00;2
              |2011/03/20 03:00:00;6
              |2011/03/20 04:00:00;10
              |""".stripMargin
    val seriesList = CSV2Series.fromString[CalcCell](csv).values.toList
    seriesList.head.toList.map {_.value} should equal (List(2d,6d,10d))
  }

  // ---------------------------------------------------------------------------  
  test("series read write 2") {
    val csv="""DateTime;A; B; C
              |2011/03/20 02:00:00;2; ; 3
              |2011/03/20 03:00:00;6; ;
              |2011/03/20 04:00:00;10; 9; 8
              |""".stripMargin
    val seriesMap = CSV2Series.fromString[CalcCell](csv)

    seriesMap.get("A").get.toList.map {_.value} should equal (List(2d,6d,10d))
    seriesMap.get("B").get.toList.map {_.value} should equal (List(9d))
    seriesMap.get("C").get.toList.map {_.value} should equal (List(3d,8d))
  }
 
  
  // ---------------------------------------------------------------------------  
  test("output format ketchup, euhhhh, checkup") {
    var series = Series[StatCell]("A", 1L)
    series = series << 1L->10d << 1L->20d << 2L->2d << 2L->4d << 2L->8d << 2L->6d
    
    CSV2Series.toString(series) should include (";")
    
    implicit val fmt = CSVFormat(separator="\t", dateTimeFormatPattern=None)

    CSV2Series.toString(series) should include ("\t")

    info(CSV2Series.toString(series))
  }
  
  // ---------------------------------------------------------------------------  
  ignore("load series from web - use case") {
    val allSeries = CSV2Series.fromURL[CalcCell]("http://ichart.finance.yahoo.com/table.csv?s=GOOG")
    val closeSeries = allSeries("Close")
    info("GOOGLE stock summary")
    // Get higher quote value
    info("Higher : "+closeSeries.max)
    // Get lowest quote value
    info("Lowest : "+closeSeries.min)
    // Get overall quote trend
    info("Week Trend : "+closeSeries.stat.linearApproximation.daySlope*7)
    // Latest quote close value
    info("Latest : "+closeSeries.last)
  }

  // ---------------------------------------------------------------------------
  test("stock series csv parsing tests") {
    case class CSVStockInfo(filename:String, code:String, lineCount:Int, keys:Option[QuoteKeys]=None)
    val frQuoteKeys = QuoteKeys(
		    close  = "Fermeture",
		    open   = "Ouverture",
		    low    = "Valeur Basse",
		    high   = "Valeur Haute",
		    volume = "Volume"
    )
    val samples =List(
    		CSVStockInfo("samples/stockFromGoogle.csv", "GOOG", 252),
    		CSVStockInfo("samples/stockFromStocklytics.csv", "?", 6975),
    		CSVStockInfo("samples/stockFromCA-ALU.csv", "ALU", 262, Some(frQuoteKeys)),  
    		CSVStockInfo("samples/stockFromYahoo.csv", "?", 6978)
    )
    for( CSVStockInfo(filename, code, count, keysopt) <- samples ) {
      val seriesopt = CSV2Series.quoteFromFile(filename, code, keysopt)
      
      seriesopt.isDefined should equal(true)
      
      for(series <- seriesopt) {
    	  series should have size(count-1)
      }
    }
  }
  // ---------------------------------------------------------------------------
  test("compressed csv ressources support") {
    val s0 = CSV2Series.fromFile[CalcCell]("samples/modstatus-totalaccesses.jxtcsv").values.head
    val s1 = CSV2Series.fromFile[CalcCell]("samples/modstatus-totalaccesses.jxtcsv.gz").values.head
    val s2 = CSV2Series.fromFile[CalcCell]("samples/modstatus-totalaccesses.jxtcsv.bz2").values.head
    
    s1.size should equal(s0.size)
    s2.size should equal(s0.size)
    s1.stat.avg should equal(s0.stat.avg)
    s2.stat.avg should equal(s0.stat.avg)
  }
  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------
  test("On the fly csv sampling (AddCell sampling)") {
    val csv="""date, A
              |2009/03/31 01:00:00.100, 2
              |2009/03/31 01:00:00.100, 6
              |2009/03/31 01:00:00.100, 10""".stripMargin
    val seriesList = CSV2Series.fromString[AddCell](csv).values
    seriesList.size should equal(1)
    seriesList.head.size should equal(1)
    seriesList.head.head.value should equal (18)
  }
}

