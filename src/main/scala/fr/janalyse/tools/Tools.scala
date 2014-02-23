package fr.janalyse.tools

import fr.janalyse.unittools._

object DurationTools {

  def duration[T](proc : =>T) = {
    val started = System.currentTimeMillis
    val result = proc
    (System.currentTimeMillis - started, result)
  }
  
  def durationAsDesc[T](proc : =>T) = duration[T](proc) match {
    case (dur,ret) => (dur.toDurationDesc, ret)
  }

  def howlong(howmany:Long=3)(proc: =>Unit) = {
    val durs = (1L to howmany) map {i =>
      val (dur,_) = duration { proc }
      dur
    }
    "Duration : avg=%s min=%s max=%s all=%s".format( 
          (durs.sum/durs.size).toDurationDesc,
          durs.min.toDurationDesc,
          durs.max.toDurationDesc,
          durs map {_.toDurationDesc})
  }

}