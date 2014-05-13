/*
 * Copyright 2011 David Crosson
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
 
package fr.janalyse.series.csv

import java.io._
import scala.io._
import scala.util._

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import java.net.URL
import fr.janalyse.series._

import com.typesafe.scalalogging.slf4j.LazyLogging


import scala.language.implicitConversions



class CSVFormat(
  val separator:String,
  val lineSeparator:String,
  val numberFormat:NumberFormat,
  val dateTimeFormat:Option[SimpleDateFormat]
) {
  def number[T<%Number](n:T):String = numberFormat.format(n)
  def timestamp(date:Date):String = dateTimeFormat map {_.format(date.getTime)} getOrElse date.getTime.toString
  def timestamp[T<%Number](time:Long):String = dateTimeFormat map {_.format(time)} getOrElse time.toString
}
 

object CSVFormat {
  implicit val defaultCSVFormat:CSVFormat = CSVFormat()

  def apply(
	  separator:String=";",
	  lineSeparator:String="\n",
	  locale:Locale = Locale.ENGLISH,
	  dateTimeFormatPattern:Option[String]=Some("yyyy/MM/dd HH:mm:ss.SSS")
  ) = {
	val nf = NumberFormat.getNumberInstance(locale)
	nf.setGroupingUsed(false)
    nf.setMaximumIntegerDigits(Int.MaxValue)
	nf.setMinimumFractionDigits(0)
    nf.setMaximumFractionDigits(3) //Int.MaxValue
	new CSVFormat(separator, lineSeparator, nf, dateTimeFormatPattern map {new SimpleDateFormat(_, locale)})
  }
}



abstract sealed class PatternAndIndex {val index:Int; val pattern:String}

case class DatePatternAndIndex(index:Int, pattern:String) extends PatternAndIndex
case class TimePatternAndIndex(index:Int, pattern:String) extends PatternAndIndex
case class DateTimePatternAndIndex(index:Int, pattern:String) extends PatternAndIndex

 

case class CSVInputFormat(
  separator:Char='\t',
  datePatterns:List[PatternAndIndex],
  locale:Locale) {

  val numberFormat = NumberFormat.getNumberInstance(locale)
  
  // TODO : Warning SimpleDateFormat is not thread safe
  private var formatterMap = Map.empty[String, SimpleDateFormat]
  def getDateFormatter(pattern:String) = {
    formatterMap.get(pattern) getOrElse {
      val formatter = new SimpleDateFormat(pattern, locale)
      formatterMap += pattern -> formatter
      formatter
    }
  }
  
  def getDate(cells:Array[String]) : Date = {
    datePatterns match {
      case DatePatternAndIndex(i,pat)::Nil =>
          getDateFormatter(pat).parse(cells(i))
      case DateTimePatternAndIndex(i,pat)::Nil =>
           getDateFormatter(pat).parse(cells(i))
      case DatePatternAndIndex(id,pd)::TimePatternAndIndex(it,pt)::Nil =>
           getDateFormatter(pd+" "+pt).parse(cells(id)+" "+cells(it))
      case TimePatternAndIndex(it,pt)::DatePatternAndIndex(id,pd)::Nil =>
           getDateFormatter(pd+" "+pt).parse(cells(id)+" "+cells(it))
      case TimePatternAndIndex(it,pat)::Nil =>
           getDateFormatter(pat).parse(cells(it))
      case _ =>
        throw new RuntimeException("datetime not recognized : '%s'".format(this))
    }
  }

  def getDate(row:String) : Date = getDate(row2cells(row, separator))
  

  private val inquotere="""^"([^"]*)"$""".r
  
  private def row2cells(row:String, separator:Char):Array[String] = {
    row.split(separator.toString, -1)       // -1 to say split all do not trim anything!!
      .map(_.trim).map {
      case inquotere(in) => in
      case x => x
    }
  }

  
 }


 
/**
  * CSV extractor with automatic CSV Format guess
  * @author David Crosson
  */

class CSV2Series(source: => Source) extends LazyLogging {
  // ---------------------------------------------------------------------------
  private val inquotere="""^"([^"]*)"$""".r
  
  private def row2cells(row:String, separator:Char):Array[String] = {
    row.split(separator.toString, -1)       // -1 to say split all do not trim anything!!
      .map(_.trim).map {
      case inquotere(in) => in
      case x => x
    }
  }
  // ---------------------------------------------------------------------------
  /*private def guessSeparator(samples:List[String]):Option[Char] = {
    val candidates="\t;,"
    val counters = candidates map { sep => (sep -> (samples.map {_ count {_ == sep} } toSet) ) }

    counters find {
      case (sep, counts) if (counts.size==1 && counts.head > 0) => true
      case _ => false
    } match {
      case None => None
      case Some((sep,_)) => Some(sep)
    }
  }*/
  // ---------------------------------------------------------------------------
  private def guessSeparator(samples:List[String]):Option[Char] = {
    val candidates="\t;,"
    val sample = samples.head
    val counters = candidates map { sep => sep -> sample.count(_==sep) }
    val results = counters collect {
      case (sep, count) if (count > 0) => sep
    } 
    results.headOption
  }
  // ---------------------------------------------------------------------------
  private def guessDatePatterns(samples:List[String], separator:Char):List[PatternAndIndex] = {
    if (samples.size<=1) throw new RuntimeException("Not enough data to guess CSV file format")
    val time1RE="""\d{2}:\d{2}(:\d{2})?(?:([., ])\d+)?""".r
    val date1RE="""\d{4}([-/_])\d{2}\1\d{1,2}""".r
    val date2RE="""\d{1,2}([-/_])\d{2}\1\d{4}""".r
    val date3RE="""\d{1,2}([-/_])\d{2}\1\d{2}""".r
    val date4RE="""\d{1,2}([-/_])\w+\1\d{2}""".r
    val datetime1RE="""\d{4}([-/_])\d{2}\1\d{1,2}([ :])\d{2}:\d{2}(:\d{2})?(?:([.,])\d+)?""".r
    val datetime2RE="""\d{1,2}([-/_])\d{2}\1\d{4}([ :])\d{2}:\d{2}(:\d{2})?(?:([.,])\d+)?""".r
    val datetime3RE="""\d{1,2}([-/_])\d{2}\1\d{2}([ :])\d{2}:\d{2}(:\d{2})?(?:([.,])\d+)?""".r
    val datetime4RE="""\d{1,2}([-/_])\w+\1\d{2}([ :])\d{2}:\d{2}(:\d{2})?(?:([.,])\d+)?""".r
    val sample=samples.tail.head // 1ere ligne de donnees
    val cells=row2cells(sample, separator).toList
    def checkS(withS:String) = withS match { case null => "" case _ => ":ss"}
    // Round 1/2, because of https://issues.scala-lang.org/browse/SI-1133   ==> Waiting for scala 2.10 &  -Yvirtpatmat
    val patterns = cells.zipWithIndex flatMap {
      case (time1RE(withS,null), i)           => Some(TimePatternAndIndex(i,"HH:mm%s".format(checkS(withS))))
      case (time1RE(withS,ms),i)              => Some(TimePatternAndIndex(i,"HH:mm%s%sS".format(checkS(withS),ms)))
      case (date1RE(ds),i)                    => Some(DatePatternAndIndex(i,"yyyy%sMM%sd".format(ds,ds)))
      case (date2RE(ds),i)                    => Some(DatePatternAndIndex(i,"d%sMM%syyyy".format(ds,ds)))
      case (date3RE(ds),i)                    => Some(DatePatternAndIndex(i,"d%sMM%syy".format(ds,ds)))
      case (date4RE(ds),i)                    => Some(DatePatternAndIndex(i,"d%sMMM%syy".format(ds,ds)))
      case _ => None
    }
    // Round 2/2, because of https://issues.scala-lang.org/browse/SI-1133   ==> Waiting for scala 2.10 &  -Yvirtpatmat
    if (patterns.isEmpty) {
      cells.zipWithIndex flatMap {
	      case (datetime1RE(ds,ts,withS,null),i)  => Some(DateTimePatternAndIndex(i,"yyyy%sMM%sd%sHH:mm%s".format(ds,ds,ts,checkS(withS))))
	      case (datetime1RE(ds,ts,withS,ms),i)    => Some(DateTimePatternAndIndex(i,"yyyy%sMM%sd%sHH:mm%s%sS".format(ds,ds,ts,checkS(withS),ms)))
	      case (datetime2RE(ds,ts,withS,null),i)  => Some(DateTimePatternAndIndex(i,"d%sMM%syyyy%sHH:mm%s".format(ds,ds,ts,checkS(withS))))
	      case (datetime2RE(ds,ts,withS,ms),i)    => Some(DateTimePatternAndIndex(i,"d%sMM%syyyy%sHH:mm%s%sS".format(ds,ds,ts,checkS(withS),ms)))
	      case (datetime3RE(ds,ts,withS,null),i)  => Some(DateTimePatternAndIndex(i,"d%sMM%syy%sHH:mm%s".format(ds,ds,ts,checkS(withS))))
	      case (datetime3RE(ds,ts,withS,ms),i)    => Some(DateTimePatternAndIndex(i,"d%sMM%syy%sHH:mm%s%sS".format(ds,ds,ts,checkS(withS),ms)))
	      case (datetime4RE(ds,ts,withS,null),i)  => Some(DateTimePatternAndIndex(i,"d%sMMM%syy%sHH:mm%s".format(ds,ds,ts,checkS(withS))))
	      case (datetime4RE(ds,ts,withS,ms),i)    => Some(DateTimePatternAndIndex(i,"d%sMMM%syy%sHH:mm%s%sS".format(ds,ds,ts,checkS(withS),ms)))
          case _ => None
      }
    } else patterns
  }
  // ---------------------------------------------------------------------------
  private val N1="""\d+""".r
  private val N2="""\d+(?:[eE]\d+)?""".r
  private val N3="""\d+([,.])\d*(?:[eE]\d+)?""".r
  private val N4="""([,.])\d+(?:[eE]\d+)?""".r
  
  private def guessLocale(samples:List[String], separator:Char, indexesRemaining:List[Int]) : Locale = {
    var numberIndexes=Set[Int]()
    var decSep=Set.empty[String]

    for( sample:String <- samples;
        (cell, index) <- row2cells(sample, separator).zipWithIndex if (indexesRemaining contains index)
    ) cell.trim match {
      case N1()   => numberIndexes+=index
      case N2()   => numberIndexes+=index
      case N3(ds) => numberIndexes+=index ; decSep+=ds
      case N4(ds) => numberIndexes+=index ; decSep+=ds
      case ""     => numberIndexes+=index   // Empty means no value
      case _ =>
    }

    if (decSep.size>1) throw new RuntimeException("Couln't compute used decimal separator")

    if (decSep contains ".") Locale.US else Locale.FRANCE
  }
  
  // ---------------------------------------------------------------------------
  private def computeValuesIndexes(cells:Array[String], indexesRemaining:List[Int]):Array[Int] = {
    def isNum(str:String) = str match {
      case N1()|N2()|"" => true
      case N3(_)  => true
      case N4(_)  => true
      case _      => false
      } 
    for( (cell,index) <- cells.zipWithIndex if isNum(cell) && indexesRemaining.contains(index)) yield  index
  }
  private def computeDateIndexes(datePatterns:List[PatternAndIndex]):List[Int] = {
    datePatterns  map {
      case DatePatternAndIndex(i,_)=>i
      case TimePatternAndIndex(i,_)=>i
      case DateTimePatternAndIndex(i,_)=>i
    }
  }
  // ---------------------------------------------------------------------------
  private def guessFormat(samples:List[String]): CSVInputFormat = {
    guessSeparator(samples) match {
      case None => throw new Exception("Couln't guess csv cell separator")
      case Some(separator) =>
        val cellCount = row2cells(samples.head, separator).size
        val datePatterns = guessDatePatterns(samples, separator)
        val dateIndexes = computeDateIndexes(datePatterns)
        var remainingIndexes = (0 until cellCount).filterNot(dateIndexes contains _).toList
        val locale = guessLocale(samples,  separator, remainingIndexes)
        CSVInputFormat(separator, datePatterns, locale)
    }
  }

  private def takeSample(howmany:Int=25) = getLines().take(howmany).toList
  
  private def getLines() =  source.getLines.toStream.filter(_.trim.size>0)

  
  /**
   * CSV series names (very fast) extractor
   * JUST series names
   * TODO: does not manage CSV pivot format 
   */
  def names() : Set[String] = {
    val headerOpt = getLines().headOption
    headerOpt map { header =>
      val res = for(separator <- guessSeparator(header::Nil) ) yield {
        val key = header.split(separator).head
        getLines()
          .filter(_ startsWith key)
          .map(_.split(separator).tail)
          .flatten
          .map(_.trim)
          .toSet
      }
      res.getOrElse(Set.empty)
    } getOrElse Set.empty
  }
  
  /**
   * CSV series time ranges (very fast) extractor
   * JUST time ranges
   * TODO: does not manage CSV pivot format 
   */
  def timeRanges() : Option[TimeRange] = {
    Try(guessFormat(takeSample())) match {
      case Failure(e) => None
      case Success(fmt) =>
        try {
	        val lines = getLines
	        for {
	          headers <- lines.headOption
	          first <- lines.tail.headOption
	          last = lines.last
	        } yield {
	          val from = fmt.getDate(first).getTime
	          val to = fmt.getDate(last).getTime
	          TimeRange(from, to)
	        }
	      } catch {
	        case e:Exception =>
	          logger.warn("Exception while browsing CSV file to compute series time range", e)
	          None
	      }
    }
  }
  
  
  /**
   * CSV extractor with pivot support
   */

  def extract[C<:Cell](tm:TimeModel = 1L)(implicit builder:CellBuilder[C]) : Map[String, Series[C]] = {
    var seriesMap = Map.empty[String, Series[C]]
    doextract { (date, names, values) =>
    //new CSV2Series(source) extract { (date, names, values) =>
      for( (name, valueopt) <- names zip values if name.size > 0) {
        var series = seriesMap.getOrElse(name, Series[C](name, tm))
        for (value <- valueopt) series <<= date.getTime -> value
        seriesMap += name -> series
      }
    }
    seriesMap
  }
  
  
  /**
   * Generic CSV extractor with pivot support and variable columns
   */
  def doextract(builder:(Date, Iterable[String], Iterable[Option[Double]]) => Unit) {
    val samples = takeSample()
    val format = guessFormat(samples)
    val firstHeader=row2cells(samples.head, format.separator)(0)
    
    val dateIndexes = computeDateIndexes(format.datePatterns)
    
    var headers:Array[String]    = Array.empty
    var valuesIndexes:Array[Int] = Array.empty
    var pivotsIndexes:Array[Int] = Array.empty
    var rebuildIndexes=true

    def makeSeriesName(cells:Array[String], header:String) = {
      val basename = pivotsIndexes map {cells(_)} mkString("-")
      if (basename.size>0) basename+"-"+header else header
    }
    
    for (line <- getLines()) {
      val cells = row2cells(line, format.separator)
      if (cells.head == firstHeader) { // Then headers and indexes need to be refreshed
        headers = cells
        rebuildIndexes=true
      } else {
	      if (rebuildIndexes) {
	        val remainingIndexes = (0 until cells.size).filterNot(dateIndexes contains _).toList
	    	valuesIndexes = computeValuesIndexes(cells, remainingIndexes)
	        pivotsIndexes = remainingIndexes.filterNot(valuesIndexes contains _).toArray // Si >0 alors presence de pivots
	        rebuildIndexes=false
	      }
	      try {
	        val date = format.getDate(cells)
	        val values = valuesIndexes 
	                .map(cells(_))
	                .map(c => if (c.length==0) None else Some(c))
	                .map(_.map(format.numberFormat.parse(_).doubleValue))
	        val names  = valuesIndexes
	                .map(i=> makeSeriesName(cells, headers(i)))
	        builder(date, names, values)
	      } catch {
	        case e:Exception =>
	          logger.warn("exception while parsing CSV file, for headers = "+headers.mkString(", "), e)
	      }
      }
    }
  }

}


/**
  * CSV2Series companion object
  * @author David Crosson
  */

case class FileContainer(file:File)
object FileContainer {
	implicit def file2FileContainer(f:File)     = FileContainer(f)
	implicit def string2FileContainer(s:String) = FileContainer(new File(s))
}

  
case class URLContainer(url:URL)
object URLContainer {
	implicit def url2URLContainer(url:URL)     = URLContainer(url)
	implicit def string2URLContainer(s:String) = URLContainer(new URL(s))
}


case class QuoteKeys(
    close:String="close",
    open:String="open",
    low:String="low",
    high:String="high",
    volume:String="volume"
)
object QuoteKeys {
  implicit def optionQuoteKeysToQuoteKeys(opt:Option[QuoteKeys]):QuoteKeys = opt match {
    case None => QuoteKeys()
    case Some(qk) => qk
  }
}

object CSV2Series {
  
  def streamfilter(resource:String, input:InputStream):InputStream = {
    resource.toLowerCase match {
      case r if r.endsWith(".gz")  => new java.util.zip.GZIPInputStream(input)
      case r if r.endsWith(".bz2") => new BZip2CompressorInputStream(input) // new CBZip2InputStream(input)
      case _ => input
    }
  }

  def fileInputStream(file:File):InputStream = streamfilter(file.getName,new FileInputStream(file))  
  def urlInputStream(url:URL):InputStream = streamfilter(url.getFile, url.openStream)  
    
  
  // returns series from various source
  private def fromSource[C<:Cell](source : => Source, tm:TimeModel) (implicit builder:CellBuilder[C]) : Map[String, Series[C]] =
    new CSV2Series(source).extract[C](tm)
  
  def fromFile[C<:Cell](fc:FileContainer, tm:TimeModel=1L) (implicit builder:CellBuilder[C]) : Map[String, Series[C]] = 
    fromSource(Source.fromInputStream(fileInputStream(fc.file)), tm)
    
  def fromURL[C<:Cell](uc:URLContainer, tm:TimeModel=1L) (implicit builder:CellBuilder[C]) : Map[String, Series[C]] = 
    fromSource(Source.fromInputStream(urlInputStream(uc.url)), tm)
    
  def fromString[C<:Cell](data:String, tm:TimeModel=1L) (implicit builder:CellBuilder[C]) : Map[String, Series[C]] = 
    fromSource(Source.fromString(data), tm)

  // returns just series names from various source
  def namesFromFile(fc:FileContainer) = new CSV2Series(Source.fromInputStream(fileInputStream(fc.file))).names()
  def namesFromURL(uc:URLContainer)   = new CSV2Series(Source.fromInputStream(urlInputStream(uc.url))).names()
  def namesFromString(data:String)    = new CSV2Series(Source.fromString(data)).names()

  def timeRangesFromFile(fc:FileContainer) = new CSV2Series(Source.fromInputStream(fileInputStream(fc.file))).timeRanges()
  def timeRangesFromURL(uc:URLContainer)   = new CSV2Series(Source.fromInputStream(urlInputStream(uc.url))).timeRanges()
  def timeRangesFromString(data:String)    = new CSV2Series(Source.fromString(data)).timeRanges()

  
  def quoteFromFile(fc:FileContainer, name:String="default", keys:QuoteKeys = QuoteKeys()) = quoteFromSource(Source.fromInputStream(fileInputStream(fc.file)), name, keys)
  def quoteFromURL(uc:URLContainer, name:String="default", keys:QuoteKeys = QuoteKeys()) = quoteFromSource(Source.fromInputStream(urlInputStream(uc.url)), name, keys)
    
  def quoteFromSource(source: => Source, name:String, keys:QuoteKeys):Option[Series[QuoteCell]] = {
    try {
	    var series = Series[QuoteCell](name)
	    new CSV2Series(source) doextract { (date, names, values) =>
	      val map = (names.map(_.toLowerCase) zip values).toMap
	      for(closeFound  <- map.get(keys.close.toLowerCase)  ; close  <- closeFound;
	          openFound   <- map.get(keys.open.toLowerCase)   ; open   <- openFound;
	          lowFound    <- map.get(keys.low.toLowerCase)    ; low    <- lowFound;
	          highFound   <- map.get(keys.high.toLowerCase)   ; high   <- highFound;
	          volumeFound <- map.get(keys.volume.toLowerCase)
	          ) {        
	          val cell = 
	            QuoteCell(time   = date.getTime,
	                     close  = close,
	                     open   = open,
	                     low    = low,
	                     high   = high,
	                     volume = volumeFound)
	          series <<= cell 
	      }
	    }
	    Some(series)
    } catch {
      case x:java.io.FileNotFoundException => None // Can't find stock quote 
    }
  }
  
  def toString[C<:Cell](seriesMap:Map[String, Series[C]])(implicit format:CSVFormat):String = toString(seriesMap.values)  
  def toString[C<:Cell](series:Series[C])(implicit format:CSVFormat):String = toString(series::Nil)
  def toString[C<:Cell](seriesList:Iterable[Series[C]])(implicit format:CSVFormat) = {
    val output = new StringWriter()
    toWriter(seriesList, output)
    output.toString
  }
  
  def toFile[C<:Cell](seriesMap:Map[String,Series[C]], dest:FileContainer)(implicit format:CSVFormat) = toWriter(seriesMap.values, new FileWriter(dest.file))
  def toFile[C<:Cell](seriesList:Iterable[Series[C]], dest:FileContainer)(implicit format:CSVFormat) = toWriter(seriesList, new FileWriter(dest.file))
  def toFile[C<:Cell](series:Series[C], dest:FileContainer)(implicit format:CSVFormat) = toWriter(series::Nil, new FileWriter(dest.file))

  def toWriter[C<:Cell](seriesList:Iterable[Series[C]], output:Writer)(implicit format:CSVFormat) = {
    val pout = new PrintWriter(output)
    val sep  = format.separator
    val nl   = format.lineSeparator

    var current = seriesList map { _.toList}
    val headers = seriesList map { _.name}
    pout.print("DateTime")
    pout.print(headers.mkString(sep,sep,""))
    pout.print(nl)
    var moretodo=true
    do {
      val heads   = current map {_.headOption}
      val cells   = heads filter {_.isDefined} map {_.get}
      
      if (cells.isEmpty) moretodo = false
      else {
        var emptyline=true
        val minTime = (cells map {_.time}).min
        val headsAsStrings = heads map {head =>
          if (head.isDefined) {
            val value = head.get.value
            if (head.get.time == minTime && !value.isNaN && !value.isInfinite) {
              emptyline=false
              format.number(value)
            } else {
              ""
            }
          } else {
            ""
          }
        }
        current = current map {s => 
            s.headOption match {
               case Some(head) if (head.time==minTime) => s.tail
               case _ => s
            }
        }
        if (!emptyline) {
          pout.print(format.timestamp(minTime))
          pout.print(headsAsStrings.mkString(sep,sep,""))
          pout.print(nl)
        }
      }
    } while(moretodo)
    pout.close
  }
}



// ==============================================================================================================
