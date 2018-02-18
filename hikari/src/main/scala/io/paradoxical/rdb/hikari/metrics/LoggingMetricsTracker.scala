package io.paradoxical.rdb.hikari.metrics

import com.zaxxer.hikari.metrics.MetricsTracker
import org.slf4j.LoggerFactory

object LoggingMetricsTracker extends MetricsTracker {
  private val logger = LoggerFactory.getLogger(getClass)

  override def recordConnectionTimeout(): Unit = {
    logger.info("Connection timeout occurred.")
  }

  override def recordConnectionUsageMillis(elapsedBorrowedMillis: Long): Unit = {
    logger.info(s"Connection usage in millis recorded: $elapsedBorrowedMillis")
  }

  override def recordConnectionAcquiredNanos(elapsedAcquiredNanos: Long): Unit = {
    logger.info(s"Connection acquired in nanos recorded: $elapsedAcquiredNanos")
  }
}
