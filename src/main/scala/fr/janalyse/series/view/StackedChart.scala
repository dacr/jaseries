package fr.janalyse.series.view

import fr.janalyse.series._
import collection.JavaConversions._

import java.io.File

import java.awt.Color
import java.awt.BasicStroke
import java.awt.geom.Ellipse2D

import org.jfree.chart.plot.Plot
import org.jfree.chart.JFreeChart
import org.jfree.chart.{ ChartFactory => CF }
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

case class StackedSeriesDataSet(series: Array[Series[Cell]]) extends org.jfree.data.xy.TableXYDataset {

  var datasetGroup: DatasetGroup = _

  // TODO : Temporary hack, aligned series cells is mandatory for this kind of charts !!!
  override def getItemCount(): Int = series.map(_.size).min

  override def getDomainOrder() = DomainOrder.NONE

  override def getItemCount(seriesIndex: Int): Int = series(seriesIndex).size

  override def getX(seriesIndex: Int, item: Int): Number = series(seriesIndex).get(item).time

  override def getXValue(seriesIndex: Int, item: Int): Double = series(seriesIndex).get(item).time

  override def getY(seriesIndex: Int, item: Int): Number = getValue(seriesIndex, item)

  override def getYValue(seriesIndex: Int, item: Int): Double = getValue(seriesIndex, item)

  override def getSeriesCount(): Int = series.size

  override def getSeriesKey(seriesIndex: Int): Comparable[String] = series(seriesIndex).alias

  override def indexOf(seriesKey: Comparable[_]): Int = series.indexWhere(_.alias == seriesKey)

  override def addChangeListener(listener: DatasetChangeListener) {}

  override def removeChangeListener(listener: DatasetChangeListener) {}

  override def getGroup(): DatasetGroup = datasetGroup

  override def setGroup(group: DatasetGroup) { this.datasetGroup = group }

  def getValue(seriesIndex: Int, item: Int): Double = series(seriesIndex).get(item).value

}

case class StackedSeriesInfo(
  series: Series[Cell], // The reference series
  alignedSeries: Series[Cell], // The modified "aligned" series associated with "series"
  index: Int,
  color: Color,
  scale: Scale)

object StackedChart extends ChartFactory[StackedChart] {
  def apply(seriesArray: Series[Cell]*)(implicit config: ChartConfig) = new StackedChart(seriesArray.toIterable, chartConfig = config)
  def apply(seriesList: Iterable[Series[Cell]])(implicit config: ChartConfig) = new StackedChart(seriesList, chartConfig = config)
  def apply(title: String, seriesArray: Series[Cell]*)(implicit config: ChartConfig) = new StackedChart(seriesArray.toIterable, Some(title), chartConfig = config)
  def apply(title: String, seriesList: Iterable[Series[Cell]])(implicit config: ChartConfig) = new StackedChart(seriesList, Some(title), chartConfig = config)
}

/* TODO : Series must are internally aligned !
 *    fill empty cells with zero
 */
class StackedChart(
  val seriesList: Iterable[Series[Cell]],
  val title: Option[String] = None,
  val chartConfig: ChartConfig = ChartConfig()) extends Chart with ChartUsingJFreeChart {

  
  def align[C <: Cell](list: Iterable[Series[C]], timeNorm: Long => Long = x => x): Iterable[Series[C]] = {
    // We'll only take common normalized time for all given series
    val times2alignwith =
      list
        .map(_.map(c => timeNorm(c.time)).toSet)
        .reduce(_ intersect _)

    list
      .map(_.take(times2alignwith.head, times2alignwith.last))
      //        .map(_.sample(timeNorm)) // TODO
      .map(_.filter(c => times2alignwith contains c.time))
  }
  
  def alignFillHoles[C <: Cell] (list: Iterable[Series[C]], holeValue:Double = 0d) : Iterable[Series[C]] = {
    val possibleTimes =
      list
        .map(_.map(_.time).toSet)
        .reduce(_ union _)
  
    for {series <- list} yield {
      val seriesTimes = series.map(_.time).toSet
      val missingTimes = possibleTimes -- seriesTimes
      series <<< missingTimes.map( _ -> holeValue)
    }
  }
  

  private val jchart = CF.createTimeSeriesChart(null, null, null, null, false, false, false)
  private val jplot = jchart.getPlot().asInstanceOf[XYPlot]
  protected val jfree = new JFreeChart(null, null, jplot, chartConfig.showLegend)

  title foreach { jfree.setTitle(_) }

  val scaleManager = chartConfig.scaleManager()

  jplot.setBackgroundPaint(new Color(220, 220, 220));
  jplot.setDomainGridlinePaint(Color.white);
  jplot.setRangeGridlinePaint(Color.white);

  for ((k, v) <- hints) jchart.getRenderingHints().put(k, v)
  jchart.setAntiAlias(true)
  jchart.setTextAntiAlias(true)

  val preparedSeries = alignFillHoles(seriesList)
  //val preparedSeries = seriesList

  private val scale = seriesList.headOption.map(scaleManager.scalesFor _) getOrElse scaleManager.default

  private val seriesInfoList: Iterable[StackedSeriesInfo] =
    for {
      ((series, color), index) <- seriesList.map(_ -> colorPool.take).zipWithIndex
    } yield {
      val alignedSeries = series // TODO
      StackedSeriesInfo(series, alignedSeries, index, color, scale)
    }

  private val seriesColorMap = seriesInfoList.map(s => s.series -> s.color).toMap
  private val seriesScaleMap = seriesInfoList.map(s => s.series -> s.scale).toMap

  val renderer = new org.jfree.chart.renderer.xy.StackedXYAreaRenderer2() {
    for { s <- seriesInfoList } {
      setSeriesFillPaint(s.index, Color.white)
      setSeriesPaint(s.index, s.color)
      setSeriesStroke(s.index, basicStroke)
      setSeriesOutlineStroke(s.index, outlineBasicStroke)
      setSeriesShape(s.index, shape)
    }
  }
  val dataset = StackedSeriesDataSet(preparedSeries.toArray)

  jplot.setRenderer(0, renderer)
  jplot.setDataset(0, dataset)

  /**
   * returns series chosen scales
   * @return the rate series
   */
  def scales(): Map[Series[Cell], Scale] = seriesScaleMap

  /**
   * returns series chosen colors
   * @return series, color map
   */
  def colors(): Map[Series[Cell], Color] = seriesColorMap
}
  
