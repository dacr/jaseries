/*
 * Copyright 2011-2012 David Crosson
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

package fr.janalyse.series.view

import collection.JavaConversions._
import fr.janalyse.series.{Series,Cell}

import java.io.File

import java.awt.Color
import java.awt.BasicStroke
import java.awt.geom.Ellipse2D

import org.jfree.chart.plot.Plot
import org.jfree.chart.JFreeChart
import org.jfree.chart.{ChartFactory=>CF}
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.axis.AxisLocation
import org.jfree.data.xy.XYDataset
import org.jfree.chart.labels.XYToolTipGenerator
import org.jfree.data.DomainOrder
import org.jfree.data.general.DatasetChangeListener
import org.jfree.data.general.DatasetGroup
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.chart.renderer.xy.XYAreaRenderer


// =========================================================================================================================


case class SeriesView(series:Series[Cell], datasetIndex:Int, scale:Scale, color:Color=Color.blue)  extends XYDataset {
  
	val basicStroke = new BasicStroke(1f)
	val markedStroke = new BasicStroke(3f)
	val outlineBasicStroke = new BasicStroke(1.0f)
	val outlineMarkedStroke = new BasicStroke(2.0f)
    val shape = new Ellipse2D.Double(-2D, -2D, 4D, 4D)
	
	val renderer = {
	  new XYLineAndShapeRenderer() {
	    setDrawOutlines(true)
	    setUseFillPaint(true)
	    setSeriesShapesVisible(0, true)
        setSeriesShapesFilled(0, true)
        setSeriesFillPaint(0, Color.white)
        setSeriesPaint(0, color)
        setSeriesStroke(0, basicStroke)
        setSeriesOutlineStroke(0, outlineBasicStroke)
        setSeriesShape(0, shape)
	  }
	}
  
    var datasetGroup:DatasetGroup=_

    override def getDomainOrder() = DomainOrder.NONE

    override def getItemCount(seriesIndex:Int):Int = series.size

    override def getX(seriesIndex:Int, item:Int):Number = series.get(item).time

    override def getXValue(seriesIndex:Int, item:Int):Double = series.get(item).time

    override def getY(seriesIndex:Int, item:Int):Number = getValue(item)

    override def getYValue(seriesIndex:Int, item:Int):Double = getValue(item)
    
    override def getSeriesCount():Int = 1

    override def getSeriesKey(seriesIndex:Int):Comparable[String] = series.alias

    override def indexOf(seriesKey:Comparable[_]):Int = 0

    override def addChangeListener(listener:DatasetChangeListener ) {}

    override def removeChangeListener(listener:DatasetChangeListener ) {}

    override def getGroup():DatasetGroup = datasetGroup
    
    override def setGroup(group:DatasetGroup) {this.datasetGroup = group}

    def getValue(item:Int):Double = series.get(item).value

    def getMin():Double = series.stat.min
    
    def getMax():Double = series.stat.max
}


// =========================================================================================================================



object SimpleChart extends ChartFactory[SimpleChart] {
  def apply(seriesArray:Series[Cell]*)(implicit chartConfig:ChartConfig) =  new SimpleChart(seriesArray.toIterable)(chartConfig)
  def apply(seriesList:Iterable[Series[Cell]])(implicit chartConfig:ChartConfig) = new SimpleChart(seriesList)(chartConfig)
  def apply(title:String, seriesArray:Series[Cell]*)(implicit chartConfig:ChartConfig) = new SimpleChart(seriesArray.toIterable, Some(title))(chartConfig)
  def apply(title:String, seriesList:Iterable[Series[Cell]])(implicit chartConfig:ChartConfig) = new SimpleChart(seriesList, Some(title))(chartConfig)
}



class SimpleChart(val seriesList:Iterable[Series[Cell]], 
            val title:Option[String]=None)
       (implicit val chartConfig:ChartConfig) extends Chart with ChartUsingJFreeChart {
  
  private val jchart = {
    import org.jfree.chart.plot.PlotOrientation
    CF.createTimeSeriesChart(null, null, null, null, false, false, false)
	  /* TODO : ICI */
    //ChartFactory.createStackedXYAreaChart(null, null, null, null, PlotOrientation.VERTICAL, false, false, false)
  }
  private val jplot  = jchart.getPlot().asInstanceOf[XYPlot]
  protected val jfree = new JFreeChart(null,null,jplot,chartConfig.showLegend)
  
  title foreach {jfree.setTitle(_)}
  
  val scaleManager = chartConfig.scaleManager()
  
  jplot.setBackgroundPaint(new Color(220,220,220));
  jplot.setDomainGridlinePaint(Color.white);
  jplot.setRangeGridlinePaint(Color.white);
          
  for( (k,v) <- hints) jchart.getRenderingHints().put(k,v)
  jchart.setAntiAlias(true)
  jchart.setTextAntiAlias(true)
  
  private val seriesViewList     = seriesList.zipWithIndex map {case (s,i) => SeriesView(s,i, scaleManager.scalesFor(s), colorPool.take())}
  private val seriesViewByScales = seriesViewList groupBy {sv => sv.scale}
  private val scaleLocation      = seriesViewByScales.keys.zipWithIndex.map({case (sv,i) => sv -> (if (i%2==0) leftSide else rightSide)}).toMap
  private val seriesViewScale    = seriesViewList.map(sv => sv -> sv.scale).toMap
  
  // --- Initializing scales
  scaleManager.updateScalesFromSeriesView(seriesViewList)
  
  // --- Creating JFreeChart Scales
  for(scale <- seriesViewByScales.keys) {
	val axis = scale.fullName map {n => new NumberAxis(n)} getOrElse new NumberAxis()
	axis.setNumberFormatOverride(new ScaleNumberFormat(scale))
	axis.setAutoRange(true)
	axis.setAutoRangeIncludesZero(false)
	jplot.setRangeAxis(scale.rangeAxisIndex, axis)
	jplot.setRangeAxisLocation(scale.rangeAxisIndex, scaleLocation.get(scale) getOrElse leftSide)
  }

  // --- Adding datasets to JFreeChart
  for(sv <- seriesViewList) {
	jplot.setRenderer(sv.datasetIndex, sv.renderer)
	jplot.setDataset(sv.datasetIndex, sv)
	jplot.mapDatasetToRangeAxis(sv.datasetIndex, seriesViewScale.get(sv).map(_.rangeAxisIndex) getOrElse 0)
	jplot.mapDatasetToDomainAxis(sv.datasetIndex, 0);
  }
  
  val seriesColorMap = seriesViewList.map(sv => sv.series->sv.color).toMap
  val seriesScaleMap =  for { (sv,scale) <- seriesViewScale} yield sv.series->scale
  // ----------------------------------------------------------------------------------------------------
  
  /**
   * returns series chosen scales
   * @return the rate series
   */
  def scales():Map[Series[Cell], Scale] = seriesScaleMap
  
  /**
   * returns series chosen colors
   * @return series, color map
   */
  def colors():Map[Series[Cell], Color] = seriesColorMap
}
