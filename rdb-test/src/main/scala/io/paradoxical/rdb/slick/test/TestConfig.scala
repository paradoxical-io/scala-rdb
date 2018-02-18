package io.paradoxical.rdb.slick.test

import io.paradoxical.rdb.config.RdbCredentials
import io.paradoxical.rdb.hikari.config.{HikariConnectionPoolConfig, RdbConfigWithConnectionPool}

object TestConfig {
  val default = RdbConfigWithConnectionPool (
    url = "",
    credentials = RdbCredentials("root", ""),
    connection_pool = HikariConnectionPoolConfig()
  )
}
