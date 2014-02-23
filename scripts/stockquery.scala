#!/bin/sh
exec java -jar jaseries.jar -nocompdaemon -usejavacp -savecompiled "$0" "$@"
!#

import io.Source
import fr.janalyse.series._
import fr.janalyse.unittools._

// Get nasdaq codes
val codes = 
  Source.fromURL("http://www.nasdaqtrader.com/dynamic/SymDir/nasdaqlisted.txt")
    .getLines
    .map(_.split("[|]",2).head)
    .toList.tail.init.take(50)

// Get nasdaq stocks history from yahoo finance
val stocks = codes.flatMap( code => CSV2Series.quoteFromURL("http://ichart.finance.yahoo.com/table.csv?s="+code, code))

val last20daysTopIncr =
  stocks
    .filter(_.size>0)
    .map(_.takeRight("20d"))
    .sortBy(- _.stat.linearApproximation.slope)
    .take(50)
    
// print highest slope top20, but remember that all history data is taken into
// account, so the last value may not be quiter higher than the first one.
for(stock <- last20daysTopIncr) {
  println("%s : %f -> %f  (%+f)".format(stock.name, stock.head.value, stock.last.value, stock.stat.linearApproximation.daySlope))
}
