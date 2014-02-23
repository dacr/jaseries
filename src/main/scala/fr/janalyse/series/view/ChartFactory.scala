package fr.janalyse.series.view

import fr.janalyse.series.{Cell,Series}

trait ChartFactory[C<:Chart] {
  def apply(seriesArray:Series[Cell]*)(implicit chartConfig:ChartConfig):C
  def apply(seriesList:Iterable[Series[Cell]])(implicit chartConfig:ChartConfig):C
  def apply(title:String, seriesArray:Series[Cell]*)(implicit chartConfig:ChartConfig):C
  def apply(title:String, seriesList:Iterable[Series[Cell]])(implicit chartConfig:ChartConfig):C  
}
