package fr.janalyse.series.view

import java.awt.Color
import collection.JavaConversions._

class ColorPool {
  private def color(r:Int, g:Int, b:Int) = new Color(r,g,b)
  private val basecolors = {
    val (d,m,f)=(80, 160, 220)
    List(
		color(  d, d, d), // 0
        color(  m, 0, 0), // 1
        color(  0, m, 0), // 2
        color(  0, 0, m), // 3
        color(200,100,0), // 4
        color(  m, 0, m), // 5
        color(  0, m, m), // 6
        color(  m, m, 0), // 7
        color(  100, 190, 100), // 8
        color(  0, d, f), // 9
        color(  f, 0, d), // 10
        color(  m, d, m), // 11
        color(  d, 0, f) // 12
    )
  }
  private var generatedCounter=0
  private def generateColor() = {
    val rest = generatedCounter%basecolors.size
    val base = generatedCounter/basecolors.size
    var color = basecolors.get(rest)
    if (base%3==0) color=color.brighter
    if (base%3==2) color=color.brighter.brighter
    generatedCounter+=1
    color
  }

  var colorCache=List.empty[Color]
  
  def take():Color = {
    colorCache match {
      case Nil => generateColor()
      case h::t => colorCache = t ; h
    }
  }
  
  def giveback(c:Color) = colorCache = c :: colorCache

}
