package io.paradoxical.rdb.slick.test.providers

import io.paradoxical.rdb.slick.test.{BaseProviderSpec, MySqlDockerProvider, TestConfig}
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.ExecutionContext.Implicits.global

class MySQLDBProviderSpec extends BaseProviderSpec with BeforeAndAfterAll {
  private val defaultConfiguration = TestConfig.default.toBasicRdb

  val mysqlDbProvider = new MySqlDockerProvider(defaultConfiguration)

  "MySQLDBProvider" should "connect, insert, and retrieve" in {
    val mysqlDb = mysqlDbProvider.provider("test")
    runTestInsertAndGet(mysqlDb)
  }

  it should "stream" in {
    val mysqlDb = mysqlDbProvider.provider("test")
    runTestStream(mysqlDb)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    mysqlDbProvider.createDb("test")
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    mysqlDbProvider.shutdown()
  }
}
