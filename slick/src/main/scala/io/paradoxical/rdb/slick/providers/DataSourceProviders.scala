package io.paradoxical.rdb.slick.providers

import io.paradoxical.rdb.hikari.config.RdbConfigWithConnectionPool
import io.paradoxical.rdb.hikari.metrics.LoggingMetricsTrackerFactory
import io.paradoxical.rdb.slick.providers.custom.HikariSourceProvider
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

object DataSourceProviders {

  import SourceProviderUtils._

  /**
   * Construct a new DataSource for use with a given configuration. This is the default provider
   *
   * This method MAY change over time as the default providers are adjusted
   *
   * @return
   */
  def default(
    config: RdbConfigWithConnectionPool,
    executionContext: ExecutionContext = defaultExecutionContext
  ): DataSource = {
    HikariSourceProvider.withHikariCP(config.toBasicRdb, config.connection_pool.toHikariConfig, LoggingMetricsTrackerFactory)
  }
}


