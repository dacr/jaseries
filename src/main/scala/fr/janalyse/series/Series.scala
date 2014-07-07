/*
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * Copyright 2011-2012-2013 David Crosson
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.janalyse.series

import fr.janalyse.unittools._
import java.text.SimpleDateFormat
import scala.language.implicitConversions
import scala.collection._
import scala.collection.generic.GenericTraversableTemplate
import collection.mutable.Builder
import collection.generic.CanBuildFrom
import collection.immutable.{ Vector, VectorBuilder }
import scala.annotation.tailrec
import scala.collection.generic.GenericCompanion

/**
 * TimeModel describes how cell's time is sampled
 * @author David Crosson
 */
trait TimeModel {
  def referenceTime(time: Long): Long
  def granularityAt(time: Long): Long
  def period: Long // TODO - TO REMOVE
}

case class TimeModelWithPeriod(period: Long = 1) extends TimeModel {
  def referenceTime(time: Long) = time / period * period
  def granularityAt(time: Long) = period
  override def toString = "" + period + "ms"
}
case class TimeModelCustom(normalizer: Long => Long) extends TimeModel {
  def referenceTime(time: Long) = normalizer(time)
  def granularityAt(time: Long) = {
    // Hummmm problably not the best approache, refactorying will be required...
    var d = 0
    val ref = referenceTime(time)
    var cur = time
    while (normalizer(cur + 1) == ref) {
      cur += 1
      d += 1
    }
    cur = time
    while (normalizer(cur - 1) == ref) {
      cur -= 1
      d += 1
    }
    d
  }
  override def toString = "customTimeModel"
  val period = 1L // TODO - NON SENS !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! IN THAT CASE
}

object TimeModel {
  implicit def long2TimeModel[P <% Number](period: P): TimeModel = new TimeModelWithPeriod(period.longValue)
  implicit def string2TimeModel(desc: String): TimeModel = new TimeModelWithPeriod(desc2Duration(desc))
  implicit def tranform2TimeModel(normalizer: Long => Long) = new TimeModelCustom(normalizer)
}

protected trait CellOrdering[C <: Cell] {
  implicit object defaultCellOrdering extends Ordering[C] {
    final def compare(c1: C, c2: C) = c1.value compare c2.value
  }
}

trait CellBuilder[C <: Cell] {
  def buildFrom[T <% Number, V <% Number](time: T, value: V): C
  def buildFrom[X <: Cell](cell: X): C = buildFrom(cell.time, cell)
  def buildFrom[T <% Number, X <: Cell](time: T, cell: X): C = buildFrom(time, cell.value)
  def merge[X <: Cell](cellA: C, cellB: X): C
}

object Series {
  def apply[X <: Cell](name: String)(implicit builder: CellBuilder[X]): Series[X] = new Series(name, 1, name)
  def apply[X <: Cell](name: String, alias: String)(implicit builder: CellBuilder[X]): Series[X] = new Series(name, 1, alias)
  def apply[X <: Cell](name: String, timeModel: TimeModel)(implicit builder: CellBuilder[X]): Series[X] = new Series(name, timeModel, name)
  def apply[X <: Cell](name: String, timeModel: TimeModel, alias: String)(implicit builder: CellBuilder[X]): Series[X] = new Series(name, timeModel, alias)
  def apply[X <: Cell](name: String, series: Series[Cell])(implicit builder: CellBuilder[X]): Series[X] = new Series(name, series.tm, name) <<< series
  def apply[X <: Cell](name: String, alias: String, series: Series[Cell])(implicit builder: CellBuilder[X]): Series[X] = new Series(name, series.tm, alias) <<< series
  def apply[X <: Cell](series: Series[Cell])(implicit builder: CellBuilder[X]): Series[X] = new Series(series.name, series.tm, series.alias) <<< series

  private def fromVector[C <: Cell](name: String, tm: TimeModel, alias: String, buf: Vector[C])(implicit builder: CellBuilder[C]): Series[C] = {
    new Series[C](name, tm, alias) <<< buf
  }

  def newBuilder[C <: Cell](name: String, tm: TimeModel, alias: String)(implicit builder: CellBuilder[C]): Builder[C, Series[C]] = {
    new VectorBuilder mapResult { x: Vector[C] => fromVector(name, tm, alias, x) }
  }

  implicit def canBuildFrom[C <: Cell](implicit builder: CellBuilder[C]): CanBuildFrom[Series[_], C, Series[C]] =
    new CanBuildFrom[Series[_], C, Series[C]] {
      def apply(): Builder[C, Series[C]] = newBuilder("default", 1L, "default")
      def apply(from: Series[_]): Builder[C, Series[C]] = newBuilder(from.name, from.tm, from.alias)
    }
  
}

/**
 * Series is a special kind of Cells collection. Cells are time ordered, and
 * when two cells shares the same time (or time period) a combination operation
 * take place.
 * @author David Crosson
 */

class Series[+C <: Cell](
  val name: String,
  val tm: TimeModel,
  val alias: String,
  protected val backend: Vector[C] = Vector.empty[C])
  (implicit cellBuilder: CellBuilder[C])
  extends immutable.IndexedSeq[C]
//  with GenericTraversableTemplate[C, Series]
  with IndexedSeqLike[C, Series[C]]
{
  import collection.mutable.Builder
  override protected[this] def newBuilder: Builder[C, Series[C]] = Series.newBuilder(name, tm, alias)
  
//  override def companion:GenericCompanion[Series] = Series
  
  override lazy val hashCode: Int =
    41 * (41 * (41 + name.hashCode) + tm.hashCode) + backend.hashCode // alias is volontary ignored;

  override def equals(that: Any): Boolean = { // alias not taken into account
    that.hashCode == hashCode && that.isInstanceOf[Series[C]] && {
      val thatSeries = that.asInstanceOf[Series[C]]
      thatSeries.name == name &&
        thatSeries.tm == tm &&
        thatSeries.backend == backend
    }
  }
  
  override def apply(index: Int): C = {
    if (index < 0 || index >= length) throw new IndexOutOfBoundsException
    backend(index)
  }
  override def length = backend.length
  override def last = backend.last //backend(length-1)
  override def head = backend.head
  override def headOption = backend.headOption

  /**
   * get series global statistics such as Average, Standard Deviation, ...
   * @return Series statistics
   */
  lazy val stat = Statistics(this)

  /**
   * Combine a new cell and the current series into a new series
   * @param cell The new cell to combine
   * @return the resulting series
   */
  def <<[X <: Cell](cell: X): Series[C] = new Series[C](name, tm, alias, combine(cellBuilder.buildFrom(cell)))

  /**
   * Combine a new number tuple and the current series into a new series
   * @param cell the new cell, generated from the given tuple, to combine with
   * @return the resulting series
   */
  def <<[T <% Number, V <% Number](cell: Tuple2[T, V]): Series[C] =
    new Series[C](name, tm, alias, combine(cellBuilder.buildFrom(cell._1, cell._2)))

  private def combine[X >: C <: Cell](cell: X) = {
    val reftime = tm.referenceTime(cell.time)
    if (length == 0 || reftime > last.time) backend :+ cellBuilder.buildFrom(reftime, cell)
    else if (reftime == last.time) backend.init :+ cellBuilder.merge(backend.last, cell)
    else if (reftime < head.time)  cellBuilder.buildFrom(reftime, cell) +: backend
    else if (reftime == head.time) cellBuilder.merge(backend.head, cell) +: backend.tail
    else insert(reftime, cell)
  }
  
  // up to scala 2.10.2 the following insert method is the faster one
  // To check again with scala 2.11
  private def insert[X >: C <: Cell](reftime:Long, cell: X) = {
      val builder = backend.companion.newBuilder[X]
      val it = backend.iterator
      var found=false
      while(it.hasNext && !found) {
        val current = it.next
        if (current.time < reftime) builder += current
        else if (current.time == reftime) {
          builder += cellBuilder.merge(current, cell)
          found = true
        } else {
          builder += cellBuilder.buildFrom(reftime, cell)
          builder += current
          found=true
        }
      }
      while(it.hasNext) builder += it.next
      
      builder.result()    
  }
  
  /*
  // Quite slower than using directly a builder...
  private def insert[X >: C <: Cell](reftime:Long, cell: X) = {
      searchFirstGreaterOrEqual(backend, reftime) match {
        case Some(index) =>
           val (before, after) = backend.splitAt(index)
           if (after.head.time == reftime) (before :+ cellBuilder.merge(after.head, cell)) ++ after.tail
           //else if (before.last.time == reftime) (before :+ cellBuilder.merge(before.last, cell)) ++ after
           else (before :+ cellBuilder.buildFrom(reftime, cell)) ++ after
        case None =>
          backend :+ cellBuilder.buildFrom(reftime, cell)
      }    
  }
  
  // slower
  //private def searchFirstGreaterOrEqual(a: Seq[Cell], v: Long) = {
  //  a.indexWhere(_.time >= v) match {
  //    case -1 => None
  //    case x => Some(x)
  //  }
  //}
  // faster...
  private def searchFirstGreaterOrEqual(seq: Seq[Cell], time: Long):Option[Int] = {
    @tailrec
    def binarysearch(left:Int, right:Int):Option[Int] = {
      if (seq(left).time>=time) Some(left)
      if (seq(right).time<time) None // Not found
      else {
        (left+right)/2 match {
          case m if seq(m).time==time => Some(m)
          case m if seq(m).time>time  => 
            if (m!=left)  binarysearch(left, m)
            else Some(m)
          case m if seq(m).time<time  => 
            if (m<right) binarysearch(m+1, right)
            else None // Not found 
        }
      }
    }
    binarysearch(0, seq.size-1)
  }
  */

  /**
   * Combine all given series cells with the current one
   * @param that An cell's iterable
   * @return the resulting series
   */
  def <<<[X <: Cell](that: Iterable[X]): Series[C] = 
    ((this) /: that) { case (s, c) => s << c }


  /**
   * Combine all given tuples iterable with the current series, any number's type is accepted.
   * @param that An tuple2 iterable
   * @return the resulting series
   */
  def <<<[TN <% Number, CN <% Number](that: Iterable[Tuple2[TN,CN]]): Series[C] = {
    //this <<< that.map {case (time, value) => cellBuilder.buildFrom(time.longValue, value.doubleValue) }
    ((this) /: that) { case (s, (t, v)) => s << cellBuilder.buildFrom(t.longValue, v.doubleValue) }
  }


  /**
   * Combine all given list of Cell's iterable with the current series
   * @param that A list of cell's iterables
   * @return the resulting series
   */
  def <<<[X <: Cell](that: List[Iterable[X]]): Series[C] = {
    var ns = new Series[C](name, tm, alias)
    var current = that
    var moretodo = true
    do {
      val cells = (current map { _.headOption } filter { _.isDefined } map { _.get })
      if (cells.isEmpty) moretodo = false
      else {
        val minTime = (cells map { _.time }).min
        val syncCells = cells filter { _.time == minTime }
        ns <<<= syncCells
        current = current map { s => s.headOption match { case Some(head) if (head.time == minTime) => s.tail case _ => s } }
      }
    } while (moretodo)
    ns
  }

  // mathematic operations between a serie and a numeric value
  private def generic(op2: (Double) => Double) = {
    (new Series[CalcCell](name, tm, alias) /: this) {
      (ns, cell) => ns << CalcCell.cellBuilder.buildFrom(cell.time, op2(cell.value))
    }
  }
  def +[N <% Number](that: N) = generic(_ + that.doubleValue)
  def -[N <% Number](that: N) = generic(_ - that.doubleValue)
  def *[N <% Number](that: N) = generic(_ * that.doubleValue)
  def /[N <% Number](that: N) = generic(_ / that.doubleValue)
  def unary_- = generic(-_)
  def unary_+ = generic(+_)

  /* NEW IMPLEMENTATION */
  // mathematic operations between series
  // Warning : only left operand series (LOS) times are kept, and when a right operand series (ROS) time
  // is missing, we took the value for the previous time, or if not available the first ROS value (the head) 
  // This previous is better than the previous one, but we must make the behavior a "parameter"
  def generic(that: Iterable[Cell], op: (Double, Double) => Double): Series[CalcCell] = {
    if (that.size==0) {
      this.toSeries[CalcCell]()
    } else {
	    var currentROS = that.toList
	    var rosCell = currentROS.head
	    var newseries = new Series[CalcCell](name, tm, alias)
	
	    for (losCell <- backend) {
	      val time = losCell.time
	      currentROS.span { _.time < time } match {
	        case (Nil, Nil) =>
	        case (Nil, after) =>
	        case (before, Nil) =>
	          rosCell = before.last
	          currentROS = Nil
	        case (before, after) if (after.head.time == time) =>
	          rosCell = after.head
	          currentROS = after
	        case (before, after) =>
	          rosCell = before.last
	          currentROS = rosCell :: after
	      }
	      val nv = op(losCell.value, rosCell.value)
	      newseries <<= CalcCell.cellBuilder.buildFrom(time, nv)
	    }
	
	    newseries
    }
  }

  def +(that: Iterable[Cell]) = generic(that, _ + _)
  def -(that: Iterable[Cell]) = generic(that, _ - _)
  def *(that: Iterable[Cell]) = generic(that, _ * _)
  def /(that: Iterable[Cell]) = generic(that, _ / _)

  /**
   * Sample this series into a new one
   * @param period new sampling period
   * @return the new sampled series keep the same cell type as the origin
   */
  def sample[A <: Cell](newtm: TimeModel)(implicit builder: CellBuilder[A]) = {
    //new Series[StatCell](name, newtm, alias) <<< this
    new Series[A](name, newtm, alias) <<< backend
  }

  /**
   * Sample this series into a new one using StatCell !!
   * @param period new sampling period
   * @return the new sampled series
   */
  def statSample(newtm: TimeModel) = {
    new Series[StatCell](name, newtm, alias) <<< backend
  }

  /**
   * Convert series cells to a new cell type
   * @return the new series using the new cell type
   */
  def toSeries[Z <: Cell]()(implicit builder: CellBuilder[Z]) = {
    new Series[Z](name, tm, alias) <<< backend
  }

  /**
   * returns a sub-part of the current series, only take cells which belongs to the specified timerange
   * @param fromTime
   * @param toTime
   * @return sub timerange series
   */
  def take(fromTime: Long, toTime: Long):Series[C] = new Series[C](name, tm, alias, backend dropWhile { _.time < fromTime } takeWhile { _.time <= toTime })

  /**
   * returns a sub-part of the current series, only take cells which belongs to the specified timerange
   * @param range
   * @return sub timerange series
   */
  def take(range:TimeRange):Series[C] = take(range.from, range.to)
  
  /**
   * returns a sub-part of the current series, only the first "howlong" time
   * @param howlong how long time to take at the start of the series
   * @return sub timerange series
   */
  def take(howlong: Duration) = {
    val to = head.time + howlong.value
    new Series[C](name, tm, alias, backend takeWhile { _.time <= to })
  }

  /**
   * returns a sub-part of the current series, drop the first "howlong" time
   * @param howlong how long time to drop at the start of the series
   * @return sub timerange series
   */
  def drop(howlong: Duration) = {
    val from = head.time + howlong.value
    new Series[C](name, tm, alias, backend dropWhile { _.time < from })
  }

  /**
   * returns a sub-part of the current series, only the last "howlong" time
   * @param howlong how long time to keep at the end of the series
   * @return sub timerange series
   */
  def takeRight(howlong: Duration) = {
    val to = last.time - howlong.value
    new Series[C](name, tm, alias, backend dropWhile { _.time < to })
  }

  /**
   * returns a sub-part of the current series, drop the last "howlong" time
   * @param howlong how long time to drop at the end of the series
   * @return sub timerange series
   */
  def dropRight(howlong: Duration) = {
    val from = last.time + howlong.value
    new Series[C](name, tm, alias, backend takeWhile { _.time <= from })
  }

  /**
   * Build a new series from a given value contained into each cell
   * @param val2get the cell value extractor, for example : _.max for a StatCell
   * @return news series made of extracted cells values
   */
  def extract(val2get: C => Double = (x: C) => x.value) = {
    ((new Series[CalcCell](name, tm, alias)) /: backend) {
      (series, cell) => series << CalcCell.cellBuilder.buildFrom(cell.time, val2get(cell))
    }
  }

  /**
   * Cells values differences, the new series size will be the current one minus one.
   * @return delta series
   */
  def delta: Series[CalcCell] = {
    var result = new Series[CalcCell](name, tm, alias)
    backend.sliding(2, 1) foreach {
      case Seq(c1, c2) if (c2.value - c1.value >= 0) => result <<= CalcCell.cellBuilder.buildFrom(c2.time, c2.value - c1.value)
      case _ =>
    }
    result
  }

  /**
   * Compute cumulated ellapsed time spend by consecutive cells that satisfy the given test
   * @return Cumulated ellapsed time
   */
  def howlongFor(condition: (Double) => Boolean): Long = {
    var duration = 0L
    for (Seq(c1, c2) <- backend.sliding(2, 1)) {
      if (condition(c1.value) && condition(c2.value)) duration += c2.time - c1.time
    }
    duration
  }

  /**
   * Remove consecutive identical cell values, the oldest is the one kept.
   * @return compacted series
   */
  def compact(): Series[CalcCell] = {
    (new Series[CalcCell](name, tm, alias) /: backend) { (series, thisCell) =>
      series.lastOption match {
        case Some(thatCell) if (thatCell.value == thisCell.value) => series
        case Some(_) | None => series << thisCell
      }
    }
  }

  /**
   * Cumulates values into a new series. It computes a new series with cellN = cell(N-1)+cellN
   * @return cumulate series
   */
  def cumulate(): Series[CalcCell] = {
    var result = new Series[CalcCell](name, tm, alias)
    var sum = 0d
    foreach { cell =>
      sum += cell.value
      result = result << (cell.time -> sum)
    }
    result
  }

  /**
   * get values fields of all cells
   * @return values sequence
   */
  def values: Seq[Double] = backend map { _.value }

  /**
   * get time of all cells
   * @return times sequence
   */
  def times: Seq[Long] = backend map { _.time }

  /**
   * Rename a series (with alias set to newName, default behavior)
   * @param newName the new series's name
   * @return the renamed series
   */
  def rename(newName: String) = new Series[C](newName, tm, newName, backend)

  /**
   * Rename a series
   * @param newName the new series's name
   * @param newAlias the new series's alias
   * @return the renamed series
   */
  def rename(newName: String, newAlias: String) = new Series[C](newName, tm, newAlias, backend)

  /**
   * Rename a series
   * @param renamer how to rename the series, takes the old name and returns the new one
   * @param realiaser how to realias the series, takes the old alias and returns the new one
   * @return the renamed series
   */
  def rename(renamer: (String)=> String, aliaser: (String)=>String = x => x) = new Series[C](renamer(name), tm, aliaser(alias), backend)

  /**
   * Re-alias a series (alias is a pretty name given to the series)
   * @param newAlias the new series's alias
   * @return the re-aliased series
   */
  def realias(newAlias: String = name) = new Series[C](name, tm, newAlias, backend)

  /**
   * Re-alias a series (alias is a pretty name given to the series)
   * @param realiaser how to realias the series, takes the old alias and returns the new one
   * @return the re-aliased series
   */
  def realias(aliaser: (String)=>String) = new Series[C](name, tm, aliaser(alias), backend)

  /**
   * Re-alias a series (alias is a pretty name given to the series)
   * @param realiaser how to realias the series using the current series name
   * @return the re-aliased series
   */
  def realiasFromName(aliaser: (String)=>String) = new Series[C](name, tm, aliaser(name), backend)

  /**
   * Re-alias and Rename a series (alias is a pretty name given to the series)
   * @param renamer how to rename/realias the series using the current series name or series alias
   * @return the re-aliased series
   */
  def reboth(newlabel: String) = new Series[C](newlabel, tm, newlabel, backend)
  
  /**
   * Re-alias and Rename a series (alias is a pretty name given to the series)
   * @param renamer how to rename/realias the series using the current series name or series alias
   * @return the re-aliased series
   */
  def reboth(re: (String)=>String) = new Series[C](re(name), tm, re(name), backend)
  
  /**
   * Sync series start time with the given series
   * @param series the series to sync time with
   * @return the synchronized series
   */
  def sync(that: Series[Cell]): Series[C] = if (!that.isEmpty) sync(that.head.time) else this

  /**
   * Sync series start time with the given time
   * @param newStartTime new series start time
   * @return the synchronized series
   */
  def sync(newStartTime: Long): Series[C] = {
    val deltaTime = newStartTime - head.time
    val newbackend = backend map { cell => cellBuilder.buildFrom(cell.time + deltaTime, cell.value) }
    new Series[C](name, tm, alias, newbackend)
  }

  /**
   * series time relative synchronization
   * @param timeDeltaSpec time dela to remove to all cell times
   * @return the synchronized series
   */
  def relativeSync(timeDeltaSpec: Duration): Series[C] = {
    val newbackend = backend map { cell => cellBuilder.buildFrom(cell.time + timeDeltaSpec.value, cell.value) }
    new Series[C](name, tm, alias, newbackend)
  }

  /**
   * makes the series starts with zero value by removing to all cells the first cell value
   * @return the zero based series
   */
  def zeroBased() = if (!isEmpty) this - head.value else this

  /**
   * Calculate a rate series from the current series
   * @param ratePeriod rate chosen periodicity, if time is ms, then 1000 will mean 1s
   * @return the rate series
   */
  def toRate(ratePeriod: Long = 1000): Series[CalcCell] = {
    var result = new Series[CalcCell](name, tm, alias)
    backend.sliding(2, 1) foreach {
      case Seq(c1, c2) =>
        result <<= CalcCell.cellBuilder.buildFrom(c2.time, c2.value * ratePeriod / (c2.time - c1.time))
      case _ =>
    }
    result
  }
  
  /**
   * get first and last times of the series
   * @return the time range
   */
  def timeRange = for {
    first <- headOption
    last <- lastOption
  } yield TimeRange(first.time, last.time)
  
  
  /**
   * Best value gotten for the given time range size.
   * The computation can be CPU intensive, take care if the chosen stepper value
   *   big duration with small steps may be very slow to compute, in particular 
   *   if your extractor requires sub-series statistics computation 
   * @param sizeGoal time range size.
   * @param extract on which subseries characteristics ? Default is the statistics 90 percentile.
   * @param select which of two best time ranges is the best one. Default is take highest value
   * @param stepper increment to use between two sub-series. Default is chosen duration / 3
   */
  def bestTimeRange(
      sizeGoal:Duration,
      extract: (Series[C]) => Double = _.stat.percentile90,
      compare:(BestTimeRange,BestTimeRange)=>BestTimeRange = (a,b) => if (a.value > b.value) a else b,
      stepper:(Duration)=> Long = _.value / 3)
    : Option[BestTimeRange] = {
    timeRange match {
      case None => None
      case Some(tr) if tr.size < sizeGoal.value => None
      case Some(tr) =>
        val ref = take(tr)
        ???
    }
  }
}












