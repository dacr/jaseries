#JASeries : scala API for time numerical series operations.#

Supports read / write CSV files or strings. Pivot-columns are automatically taken into account. CSV file format is automatically guessed. Chart generation is now supported ! <b> NEW : Performance enhancements & Series is now almost a true scala collection</b>

The aim of this library is to make simple series summaries generation, using sampling and various kind of cells merging.

Processing very large CSV data doesn't afraid this library.

A standalone packaging, "[http://code.google.com/p/janalyse-series/downloads/detail?name=jaseries.jar jaseries.jar]", is provided, for quick console or scripting usage. It embeds all dependencies, and comes with all scala libraries. Otherwise for library usage, just add the dependency and the repository to your sbt configuration file (build.sbt). Just run "java -jar jaseries.jar -usejavacp" to start the console.

[API Scaladoc](http://www.janalyse.fr/scaladocs/janalyse-series)

[JAnalyse software maven repository](http://www.janalyse.fr/repository/)

[Use cases can be found on my blog](http://www.crosson.org/)

**Current release** : **1.6.1** (for scala 2.10)   1.4.0  (for scala 2.9.1, 2.9.2)

**Declare dependency in SBT as follow** :
```libraryDependencies += "fr.janalyse"   %% "janalyse-series" % "1.6.0" % "compile"```

**Add JAnalyse repository in SBT as follow** :
```resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"```

##Examples##

###Google stock quote trend###
```scala
#!/bin/sh
exec java -jar jaseries.jar -nocompdaemon -usejavacp -savecompiled "$0" "$@"
!#
 
import fr.janalyse.series._
 
val allSeries = CSV2Series.fromURL[StatCell]("http://ichart.finance.yahoo.com/table.csv?s=GOOG")
val closeSeries = allSeries("Close")
 
println("GOOGLE stock summary")
println("Higher : "+closeSeries.max)
println("Lowest : "+closeSeries.min)
println("Week Trend : "+closeSeries.stat.linearApproximation.daySope*7)
println("Latest : "+closeSeries.last)
```
Which gives :
```
$ ./stock.scala 
GOOGLE stock summary
Higher : (07-11-06 00:00:00 -> 741,79)
Lowest : (04-09-03 00:00:00 -> 100,01)
Week Trend : 0.9271503465158119
Latest : (11-03-25 00:00:00 -> 579,74)
```


###Google stock quote trend chart generation###

```scala
#!/bin/sh
exec java -jar jaseries.jar -nocompdaemon -usejavacp -savecompiled "$0" "$@"
!#

import fr.janalyse.series.CSV2Series
import fr.janalyse.series.view.SimpleChart

val allSeries = CSV2Series.fromURL[StatCell]("http://ichart.finance.yahoo.com/table.csv?s=GOOG")
val closeSeries = allSeries("Close").rename("Google stock value")    
SimpleChart(closeSeries).toFile("googleStockTrend.jpg")
```

which gives :

<img src="http://dnld.crosson.org/googleStockTrend.jpg"/>


###Compute hitrate series from hitcount series###

```
import fr.janalyse.series._
import fr.janalyse.tools.UnitTools._

val loadedSeries = CSV2Series.fromFile[CalcCell]("samples/2.csv")
val hitcount = loadedSeries("www status 200 hit count")
val hitcountSampled = Series[AddCell]("www status 200 hit rate", "10m") <<< hitcount
val hitrate  = hitcountSampled * 1000 / "10m".toDuration

CSV2Series.toFile(hitrate, "hitrate.csv")

```
The generated series, gives you hitrate each 10 minutes period.



