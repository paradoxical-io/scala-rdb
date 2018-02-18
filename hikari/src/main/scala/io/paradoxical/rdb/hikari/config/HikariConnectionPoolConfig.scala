package io.paradoxical.rdb.hikari.config

import com.zaxxer.hikari.HikariConfig
import io.paradoxical.rdb.config.{BasicRdbConfig, RdbCredentials, RdbSecurityConfig}
import scala.concurrent.duration.Duration

/**
 * Class meant to be loaded from a configuration file that conforms to this shape
 */
case class RdbConfigWithConnectionPool(
  url: String,
  credentials: RdbCredentials,
  security: RdbSecurityConfig,
  connection_pool: HikariConnectionPoolConfig
) {
  def toBasicRdb = BasicRdbConfig(url, credentials, security)
}

case class HikariConnectionPoolConfig(
  max_pool_size: Int,
  pool_name: String,
  minimum_idle: Option[Int],
  idle_timeout: Option[Duration]
) {
  def toHikariConfig: HikariConfig = {
    val config = new HikariConfig()
    config.setMaximumPoolSize(max_pool_size)
    config.setPoolName(pool_name)
    minimum_idle.foreach(config.setMinimumIdle)
    idle_timeout.foreach(it => config.setIdleTimeout(it.toMillis))
    config
  }
}