#!/bin/sh
exec java -jar jaseries.jar -nocompdaemon -usejavacp -savecompiled "$0" "$@"
!#
import fr.janalyse.series._

val allSeries = jaseries.CSV2Series.fromURL[StatCell]("http://ichart.finance.yahoo.com/table.csv?s=AAPL")
val closeSeries = allSeries("Close").realias("Apple stock value")    
jaseries.SimpleChart(closeSeries).toFile("AppleStockTrend.jpg")

