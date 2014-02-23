#!/bin/sh
exec java -jar jaseries.jar -nocompdaemon -usejavacp -savecompiled "$0" "$@"
!#

import io.Source
import fr.janalyse.series._
import java.util.{Date, Locale}
import java.text.SimpleDateFormat



// Define a default date conversion scheme using this format : "yyyy-MM-dd" as input string
implicit def stringToOptionDate(datestr:String):Option[Date] = {
  val dateparser = new SimpleDateFormat("yyyy-MM-dd")
  try {
    Some(dateparser.parse(datestr))
  } catch {
    case _:Exception => None
  }
}

//def get(code:String) = CSV2Series.fromURL("http://ichart.finance.yahoo.com/table.csv?s="+code)

def stockFromGoogle(code:String, from:Option[Date]=None, to:Option[Date]=None) = {
  val datefmt = new SimpleDateFormat("MMM+d,yyyy", Locale.US)
  val startqry  = from map {d => "&startdate=%s".format(datefmt.format(d))} getOrElse ""
  val endqry    = to map {d => "&enddate=%s".format(datefmt.format(d))} getOrElse ""
  val urlstr = "http://www.google.com/finance/historical?output=csv&q=%s%s%s".format(code, startqry, endqry)
  CSV2Series.quoteFromURL(code, urlstr)
}

val aapl = stockFromGoogle("nasdaq:AAPL", from="1990-01-01")
