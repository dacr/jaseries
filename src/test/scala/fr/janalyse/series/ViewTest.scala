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
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._
import fr.janalyse.series._
import fr.janalyse.series.view._
import fr.janalyse.tools.DurationTools._
import scala.swing.Frame
import java.awt.FlowLayout
import scala.swing.FlowPanel
import java.awt.Color
import java.awt.Dimension
import scala.swing.Label
import scala.swing.SimpleSwingApplication
import scala.swing.MainFrame
import fr.janalyse.unittools._

/**
  * Chart View Test cases
  * @author David Crosson
  */
@RunWith(classOf[JUnitRunner])
class ViewTest extends FunSuite with ShouldMatchers {

  // ---------------------------------------------------------------------------
  test("simple chart test") {
    var s1 = Series[CalcCell]("Series-1") << 10->200 << 20->270 << 30->280 << 40->260 << 50->190 //<< 45->210
    var s2 = Series[CalcCell]("Series-2") << 10->500 << 20->250 << 30->300 << 40->450 << 50->430
    
    s1 *= 100d
    s2 *= 100d
    
    SimpleChart(s1,s2).toFile("test-1.jpg")
    info("JPEG Chart file has been generated : test-1.jpg")
    
    StackedChart(s1,s2).toFile("test-stacked-1.png")
    info("PNG stacked Chart file has been generated : test-stacked-1.png")
  }

  // ---------------------------------------------------------------------------
  test("ScaleConfig tests") {
    val xmlcfg = 
      <scaleconfig name="default">

         <scalegroup name="vmstat">
           <scale name      = "True Free Memory"
                  pattern   = "(TrueMemFree)|(TrueFreeMem)"
                  type      = "autoBytesSize"
                  normalize = "1"  />

           <scale name      = "Memory usage"
                  pattern   = "(VirtualMemory)|(IdleMemory)|(BufferMemory)|(InactiveMemory)|(ActiveMemory)"
                  type      = "autoBytesSize"
                  normalize = "1024"  />

           <scale name     = "CPU usage"
                  pattern  = "(UserTime)|(SystemTime)|(IdleTime)|(WaitIOTime)|(StolenTime)|(UsedTime)"
    			  unitname = "%"
                  unitdesc = "percent"
                  type     = "Scale"  />

           <scale name     = "Process state count"
                  pattern  = "(RunQueue)|(UninterruptibleSleep)"
                  type     = "autoCount"  />

           <scale name     = "Interrupts rate"
                  pattern  = "(Interrupts)"
                  type     = "autoRate"  />
         </scalegroup>


         <scalegroup name="Generics">
           <scale name     = "Bytes"
                  pattern  = "bytes"
                  type     = "autoBytesSize"  />

           <scale name     = "Count"
                  pattern  = "count"
                  type     = "Scale"  />

           <scale name     = "Rate"
                  pattern  = "rate"
                  type     = "Scale"  />
         </scalegroup>

      </scaleconfig>
    
    val sm = ScaleManager(xmlcfg)
    implicit val chartCFG = ChartConfig(()=>sm)

    val userTimeSeries   = Series[CalcCell]("server X UserTime") << 10 -> 30 << 20 -> 70 << 30 -> 60 
    val virtualMemSeries = Series[CalcCell]("server X VirtualMemory") << 10 -> 1d*1024*1024 << 20 -> 23d*1024*1024 << 45 -> 12d*1024*1024
    // virtualMemSeries apply a normalize factor of 1024, because it assumes that VirtualMemory metric is in kilobytes, not bytes.
    
    val userTimeScale   = sm.scalesFor(userTimeSeries)
    val virtualMemScale = sm.scalesFor(virtualMemSeries)
    
    userTimeScale.name   should equal(Some("CPU usage"))
    virtualMemScale.name should equal(Some("Memory usage"))
    virtualMemScale.fullName should equal(Some("Memory usage"))   // No update has been call so no scale unit adaptation...

    // Check scale manager used with chart, so scale unit adaptation is wired and taken into account
    
    val chart = SimpleChart("System behavior", userTimeSeries.realias("CPU"), virtualMemSeries.realias("MEM"))
    
    chart.toFile("test-2.jpg")
    info("JPEG Chart file has been generated : test-2.jpg")

    chart.scales() should have size (2)

    val scales = chart.scales
      
    scales.get(virtualMemSeries).get should be theSameInstanceAs(virtualMemScale)
    scales.get(userTimeSeries).get should be theSameInstanceAs(userTimeScale)

    scales.get(userTimeSeries).get.fullName should equal (Some("CPU usage (percent)"))
    scales.get(virtualMemSeries).get.fullName should equal (Some("Memory usage (gigabytes)"))
    
    val virtualMemInGb = virtualMemSeries.map(_.value) map {v => virtualMemScale.convert(v)}
    
    virtualMemInGb should equal(List(1d, 23d, 12d))
  }

  // ---------------------------------------------------------------------------
  test("Chart scales tests") {
    val sc1 = ScaleAutoCount(1)
    sc1.bounds = Some(ScaleBounds(1,2000))
    sc1.convert(1500) should equal(1.5)
    sc1.bounds = Some(ScaleBounds(1200,1900))
    sc1.convert(1500) should equal(1500)
    sc1.bounds = None
    sc1.convert(1500) should equal(1500)
  }
  
  // ---------------------------------------------------------------------------
  ignore("view chart color pool grid") {
    val cp = new ColorPool()
    val labels = for (i <- 1 to 200) yield {
    	new Label() {
	        text ="  %d  ".format(i)
	        font = font.deriveFont(18f)
	        opaque     = true
	        background = cp.take()
	        foreground = Color.white
    	}
    }
    val frame = new Frame() {
      title      = "ViewColorMap"
      contents   = new FlowPanel(labels:_*)
      background = new Color(220,220,220)
      size       = new Dimension(600,600)
      visible    = true
      pack()
    }
    Thread.sleep(60*1000)
  }
  // ---------------------------------------------------------------------------
  ignore("Chart - Google stock trend chart - In 3 lines !!") {
    import fr.janalyse.series.CSV2Series
    import fr.janalyse.series.view.Chart
    val allSeries = CSV2Series.fromURL[CalcCell]("http://ichart.finance.yahoo.com/table.csv?s=GOOG")
    val closeSeries = allSeries("Close").realias("Google stock value")    
    SimpleChart(closeSeries).toFile("test-3.jpg")
    info("JPEG Chart file has been generated : test-3.jpg")
  }
  // ---------------------------------------------------------------------------
  test("Several series with separate scales") {
    val xmlcfg = 
      <scaleconfig name="default">

         <scalegroup name="examples">

    		<scale name  = "Request rate"
    			pattern  = "((Request)|(Hit)) rate"
             	type     = "Scale"
             	unitname = "rq/s"
             	unitdesc = "requests/second"  />

    		<scale name  = "Duration"
    			pattern  = "duration"
             	type     = "ScaleAutoDuration"  />

    		<scale name  = "Bytes rate"
             	pattern  = "bytes (returned )?rate"
             	type     = "ScaleAutoBytesRate"  />

         </scalegroup>

      </scaleconfig>
    
    implicit val chartCFG = ChartConfig(()=>ScaleManager(xmlcfg))
    
    val responseTime=CSV2Series.fromFile[CalcCell]("samples/TrendResponseTime.csv").get("web1 dynamic requests duration average").get
    val hitRate=CSV2Series.fromFile[CalcCell]("samples/TrendHitRate.csv").values.head
    val bytesRate=CSV2Series.fromFile[CalcCell]("samples/TrendBytesRate.csv").values.head
    
    val seriesList = List(
        responseTime.realias("response time"),
        hitRate.realias("hit rate"),
        bytesRate.realias("bytes rate")
        )
    
    val syncSeriesList = seriesList map {_.sync(responseTime)} map {_.take(2.h)}
    
    SimpleChart("Correlation chart", syncSeriesList).toFile("test-4.jpg")
    
    info("JPEG Chart file has been generated : test-4.jpg")
    
  }
  
}


