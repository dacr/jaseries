package fr.janalyse.series

import org.slf4j._

trait LazyLogging {
  val logger = LoggerFactory.getLogger(getClass)
}
