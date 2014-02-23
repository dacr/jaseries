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

import java.io.File

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.BasicStroke
import java.awt.geom.Ellipse2D
import java.awt.Color

import org.jfree.chart.ChartUtilities
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.AxisLocation
import fr.janalyse.series.{Series,Cell}


trait Chart {  
  val seriesList:Iterable[Series[Cell]] 
  val title:Option[String]
  val chartConfig: ChartConfig
  
  /**
   * Write the chart into a buffered image
   * @param width specified image width
   * @param height specified image height
   * @return the buffered image
   */
  def toBufferedImage(width: Int, height: Int): BufferedImage

  /**
   * Write the chart into a buffered image
   * @return the buffered image
   */
  def toBufferedImage(): BufferedImage = toBufferedImage(chartConfig.width, chartConfig.height)

  /**
   * Write the chart into an image file, format is guessed from the filename extension
   * @param filename chosen image filename
   * @param width  generated image width
   * @param height generated image height
   */
  def toFile(filename: String, width: Int, height: Int)

  /**
   * Write the chart into an image file, format is guessed from the filename extension
   * @param filename chosen image filename
   */
  def toFile(filename: String) { toFile(filename, chartConfig.width, chartConfig.height) }
  
  /**
   * returns series chosen scales
   * @return the rate series
   */
  def scales():Map[Series[Cell], Scale]
  
  /**
   * returns series chosen colors
   * @return series, color map
   */
  def colors():Map[Series[Cell], Color]
}





trait ChartUsingJFreeChart extends {
  import RenderingHints._

  val seriesList:Iterable[Series[Cell]] 
  val title:Option[String]
  
  protected val leftSide = AxisLocation.BOTTOM_OR_LEFT
  protected val rightSide = AxisLocation.BOTTOM_OR_RIGHT

  val basicStroke = new BasicStroke(1f)
  val markedStroke = new BasicStroke(3f)
  val outlineBasicStroke = new BasicStroke(1.0f)
  val outlineMarkedStroke = new BasicStroke(2.0f)
  val shape = new Ellipse2D.Double(-2D, -2D, 4D, 4D);

  protected val hints =
    Map(KEY_ANTIALIASING -> VALUE_ANTIALIAS_ON,
      KEY_ALPHA_INTERPOLATION -> VALUE_ALPHA_INTERPOLATION_SPEED,
      KEY_COLOR_RENDERING -> VALUE_COLOR_RENDER_SPEED,
      KEY_DITHERING -> VALUE_DITHER_DISABLE,
      KEY_INTERPOLATION -> VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
      KEY_RENDERING -> VALUE_RENDER_SPEED,
      KEY_TEXT_ANTIALIASING -> VALUE_TEXT_ANTIALIAS_OFF,
      KEY_STROKE_CONTROL -> VALUE_STROKE_PURE)

  protected val colorPool = new ColorPool()
  protected val jfree: JFreeChart

  val chartConfig: ChartConfig

  /**
   * Write the chart into a buffered image
   * @param width specified image width
   * @param height specified image height
   * @return the buffered image
   */
  def toBufferedImage(width: Int, height: Int): BufferedImage = jfree.createBufferedImage(width, height)


  /**
   * Write the chart into an image file, format is guessed from the filename extension
   * @param filename chosen image filename
   * @param width  generated image width
   * @param height generated image height
   */
  def toFile(filename: String, width: Int, height: Int) {
    val file = chartConfig.destDir map { new File(_, filename) } getOrElse new File(filename)
    file.getName.split("[.]").last.toLowerCase match {
      case "png" => ChartUtilities.saveChartAsPNG(file, jfree, width, height)
      case "jpg" | "jpeg" | _ => ChartUtilities.saveChartAsJPEG(file, jfree, width, height)
    }
  }


}



