package io.paradoxical.rdb.slick.providers.custom

import io.paradoxical.rdb.hikari.config.HikariConnectionPoolConfig
import io.paradoxical.rdb.config.BasicRdbConfig
import io.paradoxical.rdb.slick.providers.SourceProviderUtils
import io.paradoxical.rdb.slick.providers.SourceProviderUtils.{defaultExecutionContext, mergeProps}
import com.zaxxer.hikari.metrics.MetricsTrackerFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import java.util.Properties
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

object HikariSourceProvider {
  lazy val defaultHikariConfig = HikariConnectionPoolConfig(
    max_pool_size = SourceProviderUtils.DEFAULT_THREAD_POOL_SIZE,
    pool_name = "connection_pool",
    minimum_idle = None,
    idle_timeout = None
  )

  /**
   * Construct a new DataSource for use with HikariCP with a given configuration
   *
   * @param config
   * @param executionContext
   * @param metricsTrackerFactory
   * @return
   */
  def withHikariCP(
    config: BasicRdbConfig,
    connectionPool: HikariConfig,
    metricsTrackerFactory: MetricsTrackerFactory,
    executionContext: ExecutionContext = defaultExecutionContext
  ): DataSource = {
    connectionPool.setJdbcUrl(config.url)
    connectionPool.setMetricsTrackerFactory(metricsTrackerFactory)

    val props = mergeProps(config.credentials.toJdbcProps, config.security.map(_.toJdbcProps).getOrElse(new Properties()))

    connectionPool.setDataSourceProperties(props)

    new HikariDataSource(connectionPool)
  }
}
