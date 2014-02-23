package fr.janalyse.series.view

import java.io.File


object ChartConfig {
  implicit def defaultChartConfig:ChartConfig = ChartConfig()
}

case class ChartConfig(
    scaleManager:()=>ScaleManager=()=>ScaleManager(),
    destDir:Option[File]=None,
    width:Int=800,
    height:Int=450,
    showLegend:Boolean=true)
