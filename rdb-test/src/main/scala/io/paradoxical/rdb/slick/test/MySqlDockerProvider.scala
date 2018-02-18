package io.paradoxical.rdb.slick.test

import io.paradoxical.rdb.config.BasicRdbConfig
import io.paradoxical.rdb.hikari.config.RdbConfigWithConnectionPool
import io.paradoxical.rdb.slick.providers.DataSourceProviders
import io.paradoxical.rdb.slick.providers.custom.{ManualSourceProviders, MySQLDBProvider}
import scala.concurrent.ExecutionContext
import scala.util.Try

trait MysqlDockerInstance {
  protected lazy val mysqlDocker = Mysql.docker()

  def docker: MysqlDocker = mysqlDocker

  def shutdown(): Try[Unit] = Try(mysqlDocker.close())

  def createDb(name: String, charset: String = "utf8mb4", collation: String = "utf8mb4_unicode_ci"): String = docker.createDatabase(name)

  def dropDb(name: String): String = docker.dropDatabase(name)
}

class MySqlDockerProvider(config: BasicRdbConfig)(implicit executionContext: ExecutionContext) extends MysqlDockerInstance {
  def provider(
    db: String = "",
    configOverride: Option[BasicRdbConfig] = None,
    isDocker: Boolean = true
  ): MySQLDBProvider = {
    val url = if (isDocker) mysqlDocker.url(db) else configOverride.getOrElse(config).url
    val dbConfig = configOverride.getOrElse(config).copy(url = url)
    new MySQLDBProvider(ManualSourceProviders.withConfig[com.mysql.jdbc.Driver](dbConfig))
  }
}

class HikariDockerProvider(config: RdbConfigWithConnectionPool) extends MysqlDockerInstance {
  def provider(
    db: String = "",
    configOverride: Option[RdbConfigWithConnectionPool] = None,
    isDocker: Boolean = true
  )(implicit executionContext: ExecutionContext): MySQLDBProvider = {
    val url = if (isDocker) mysqlDocker.url(db) else configOverride.getOrElse(config).url

    val dbConfig = configOverride.getOrElse(config).copy(url = url)

    new MySQLDBProvider(DataSourceProviders.default(dbConfig))
  }
}
