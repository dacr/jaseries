package fr.janalyse.series.view

import fr.janalyse.series.{Series,Cell}


import java.text.NumberFormat
import java.text.FieldPosition
import java.text.ParsePosition
import java.io.InputStream
import java.io.File

import scala.xml.Elem
import scala.xml.XML
import scala.util.matching.Regex


class ScaleNumberFormat(scale:Scale) extends NumberFormat  {
    val defaultNumberFormat:NumberFormat = NumberFormat.getInstance()
	defaultNumberFormat.setGroupingUsed(false)
    defaultNumberFormat.setMaximumFractionDigits(1)
    
    override def format(number:Double, toAppendTo:StringBuffer, pos:FieldPosition):StringBuffer =
        defaultNumberFormat.format(scale.convert(number), toAppendTo, pos)

    override def format(number:Long, toAppendTo:StringBuffer, pos:FieldPosition):StringBuffer =
        defaultNumberFormat.format(scale.convert(number), toAppendTo, pos)

    override def parse(source:String, parsePosition:ParsePosition) = 
        defaultNumberFormat.parse(source, parsePosition)
}


object ScaleManager {
  def apply() = new ScaleManager(None)
  def apply(data:String) = new ScaleManager(Some(lessStrictXML.load(data)))
  def apply(in:InputStream) = new ScaleManager(Some(lessStrictXML.load(in)))
  def apply(file:File) = new ScaleManager(Some(lessStrictXML.loadFile(file)))
  def apply(root:Elem) = new ScaleManager(Some(root))
  def lessStrictXML = {
	val f = javax.xml.parsers.SAXParserFactory.newInstance()
	f.setNamespaceAware(false)
	f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    XML.withSAXParser(f.newSAXParser())
  }
}

case class ScaleConfig(
    group:String,
    name:String,
    pattern:Regex,
    scaletype:String,
    normalize:Double=1d,
    unitName:Option[String]=None,
    unitDesc:Option[String]=None

    )

case class ScaleManager(xmlcfg:Option[Elem]) {
  val scalesSpec = xmlcfg map {xroot =>
    for (xgroup <- xroot \ "scalegroup";
         xscale <- xgroup \ "scale") yield {
      ScaleConfig(
          group     = (xgroup \ "@name").text,
          name      = (xscale \ "@name").text,
          pattern   = ("(?i)"+(xscale \ "@pattern").text).r,
          scaletype = (xscale.attribute("type")).map(_.text).getOrElse("scale"),
          normalize = xscale.attribute("normalize").map(_.text.toDouble).getOrElse(1d),
          unitName  = xscale.attribute("unitname").map(_.text),
          unitDesc  = xscale.attribute("unitdesc").map(_.text)
      )
    }
  }

  var rangeAxisCount=0
  
  def nextRangeAxisNumber() = {
    rangeAxisCount+=1
    rangeAxisCount-1
  }
  
  lazy val default = new ScaleDefault(nextRangeAxisNumber())
  
  var currentScales = Map.empty[ScaleConfig, Scale]
  
  def updateScalesFromSeriesView(seriesViewList:Iterable[SeriesView]) {
    val seriesViewByScales = seriesViewList groupBy {_.scale}
    for((scale, svList)<-seriesViewByScales) {
    	val mins = svList map {_.getMin()}
	    val maxs = svList map {_.getMax()}
	    scale.bounds = Some(ScaleBounds(mins.min, maxs.max))
	  }  
  }
  
  def scalesFor(series:Series[Cell]) : Scale = {
    val found = scalesSpec flatMap {sp =>
      sp find {sc => (sc.pattern findFirstIn series.name).isDefined }
    }
    for(sc <- found if (! currentScales.contains(sc))) {
      currentScales += sc -> {
        sc.scaletype.toLowerCase() match {
        	case "scaleautobytessize"|"autobytessize"|"scaleautobytescount"|"autobytescount" =>
        	  ScaleAutoBytesCount(nextRangeAxisNumber(), Some(sc.name), sc.normalize)
        	case "scaleautobytesrate"|"autobytesrate" =>
        	  ScaleAutoBytesRate(nextRangeAxisNumber(), Some(sc.name), sc.normalize)
        	case "scaleautocount"|"autocount" =>
        	  ScaleAutoCount(nextRangeAxisNumber(), Some(sc.name), sc.normalize)
        	case "scaleautorate"|"autorate" =>
        	  ScaleAutoRate(nextRangeAxisNumber(), Some(sc.name), sc.normalize)
        	case "scaleautoduration"|"autoduration" =>
        	  ScaleAutoDuration(nextRangeAxisNumber(), Some(sc.name), sc.normalize)
        	case "scale" =>
        	  val nsc = ScaleDefault(nextRangeAxisNumber(), Some(sc.name), normalize=sc.normalize)
        	  nsc.unitName = sc.unitName    // TODO - BAD make scales immutable
        	  nsc.unitDesc = sc.unitDesc    // TODO - BAD make scales immutable
        	  nsc
        }
      }
    }
    found map {sc => currentScales.getOrElse(sc, default) } getOrElse default
  }
}

case class ScaleBounds(lower:Double, upper:Double)

trait Scale {
  val rangeAxisIndex:Int
  val name:Option[String]=None
  val normalize:Double=1d
  var unit:Double=1d                 // TODO - BAD make scales immutable
  var unitName:Option[String]=None   // TODO - BAD make scales immutable
  var unitDesc:Option[String]=None   // TODO - BAD make scales immutable
  def fullName = name map {_+unitDesc.map(" (%s)".format(_)).getOrElse("")}
  def convert[N<%Number](value:N) = value.doubleValue() * normalize / unit
  protected var currentBounds:Option[ScaleBounds]=None
  def bounds:Option[ScaleBounds] = currentBounds
  def bounds_=(newBounds:Option[ScaleBounds]) { currentBounds = newBounds}
}


case class ScaleDefault(
    override val rangeAxisIndex:Int,
    override val name:Option[String]=None,
    override val normalize:Double=1d) extends Scale {
}


case class ScaleAutoCount(
    override val rangeAxisIndex:Int,
    override val name:Option[String]=Some("Count"), 
    override val normalize:Double=1d) extends Scale {
  
    override def bounds_=(newBounds:Option[ScaleBounds]) {
        super.bounds = newBounds        
        bounds map {b => (b.upper - b.lower)*normalize match {
          	case x if x < 1000d =>                           unitDesc=None ; unitName=None ;          unit=1d
          	case x if x < 1000d*1000 =>          unitDesc=Some("thousands"); unitName=Some("x10^3");  unit=1000d
          	case x if x < 1000d*1000*1000 =>      unitDesc=Some("millions"); unitName=Some("x10^6");  unit=1000d*1000
          	case x if x < 1000d*1000*1000*1000 => unitDesc=Some("billions"); unitName=Some("x10^9");  unit=1000d*1000*1000
          	case _ =>                            unitDesc=Some("trillions"); unitName=Some("x10^12"); unit=1000d*1000*1000*1000
        	}
        }
    }
}

case class ScaleAutoDuration(
    override val rangeAxisIndex:Int,
    override val name:Option[String]=Some("Duration"), 
    override val normalize:Double=1d) extends Scale {
  
    override def bounds_=(newBounds:Option[ScaleBounds]) {
        super.bounds = newBounds        
        bounds map {b => (b.upper - b.lower)*normalize match {
          	case x if x < 1000d =>    unitDesc=Some("milliseconds"); unitName=Some("ms"); unit=1d
          	case x if x < 60d*1000 =>      unitDesc=Some("seconds"); unitName=Some("s") ; unit=1000d
          	case x if x < 60d*60*1000 =>   unitDesc=Some("minutes"); unitName=Some("mn"); unit=60d*1000
          	case x if x < 60d*60*1000*24 =>  unitDesc=Some("hours"); unitName=Some("h") ; unit=60d*60*1000
          	case x if x < 60d*60*1000*24*7 => unitDesc=Some("days"); unitName=Some("d") ; unit=60d*60*1000*24
          	case _ =>                        unitDesc=Some("weeks"); unitName=Some("w") ; unit=60d*60*1000*24*7
        	}
        }
    }
}

case class ScaleAutoRate(
    override val rangeAxisIndex:Int,
    override val name:Option[String]=Some("Rate"), 
    override val normalize:Double=1d) extends Scale {
  
    override def bounds_=(newBounds:Option[ScaleBounds]) {
        super.bounds = newBounds        
        bounds map {b => (b.upper - b.lower)*normalize match {
          	case x if x > 1000d =>      unitDesc=Some("each millisecond"); unitName=Some("/ms"); unit=1000d
          	case x if x > 1d/60/1000d =>     unitDesc=Some("each second"); unitName=Some("/s") ; unit=1d
          	case x if x > 1d/60/60/1000 =>   unitDesc=Some("each minute"); unitName=Some("/mn"); unit=1d/60/1000
          	case x if x > 1d/60/60/1000/24 =>  unitDesc=Some("each hour"); unitName=Some("/h") ; unit=1d/60/60/1000
          	case x if x > 1d/60/60/1000/24/7 => unitDesc=Some("each day"); unitName=Some("/d") ; unit=1d/60/60/1000/24
          	case _ =>                          unitDesc=Some("each week"); unitName=Some("/w") ; unit=1d/60/60/1000/24/7
        	}
        }
    }
}

case class ScaleAutoBytesCount (
    override val rangeAxisIndex:Int,
    override val name:Option[String]=Some("BytesCount"), 
    override val normalize:Double=1d) extends Scale {
  
    override def bounds_=(newBounds:Option[ScaleBounds]) {
        super.bounds = newBounds        
        bounds map {b => (b.upper - b.lower)*normalize match {
          	case x if x < 1024d =>               unitDesc=Some("bytes"); unitName=Some("b") ;  unit=1d
          	case x if x < 1024d*1024 =>      unitDesc=Some("kilobytes"); unitName=Some("kb") ; unit=1024d
          	case x if x < 1024d*1024*1024 => unitDesc=Some("megabytes"); unitName=Some("mb") ; unit=1024d*1024
          	case _ =>                        unitDesc=Some("gigabytes"); unitName=Some("gb") ; unit=1024d*1024*1024
        	}
        }
    }
}

case class ScaleAutoBytesRate (
    override val rangeAxisIndex:Int,
    override val name:Option[String]=Some("BytesRate"), 
    override val normalize:Double=1d) extends Scale {
  
    override def bounds_=(newBounds:Option[ScaleBounds]) {
        super.bounds = newBounds        
        bounds map {b => (b.upper - b.lower)*normalize match {
          	case x if x < 1024d =>               unitDesc=Some("bytes/second"); unitName=Some("b/s") ;  unit=1d
          	case x if x < 1024d*1024 =>      unitDesc=Some("kilobytes/second"); unitName=Some("kb/s") ; unit=1024d
          	case x if x < 1024d*1024*1024 => unitDesc=Some("megabytes/second"); unitName=Some("mb/s") ; unit=1024d*1024
          	case _ =>                        unitDesc=Some("gigabytes/second"); unitName=Some("gb/s") ; unit=1024d*1024*1024
        	}
        }
    }
}

