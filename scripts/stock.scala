#!/bin/sh
exec java -jar jaseries.jar -nocompdaemon -usejavacp -savecompiled "$0" "$@"
!#

import fr.janalyse.series._

val allSeries = jaseries.CSV2Series.fromURL[StatCell]("http://ichart.finance.yahoo.com/table.csv?s=GOOG")
val closeSeries = allSeries("Close")

println("GOOGLE stock summary")
println("Higher : "+closeSeries.max)
println("Lowest : "+closeSeries.min)
println("Week Trend : "+closeSeries.stat.linearApproximation.daySlope*7)
println("Latest : "+closeSeries.last)

