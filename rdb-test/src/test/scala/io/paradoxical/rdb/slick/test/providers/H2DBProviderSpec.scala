package io.paradoxical.rdb.slick.test.providers

import io.paradoxical.rdb.slick.providers.custom.{H2DBProvider, ManualSourceProviders}
import io.paradoxical.rdb.slick.test.{BaseProviderSpec, TestConfig}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class H2DBProviderSpec extends BaseProviderSpec {
  "H2DBProvider" should "connect, insert, and retrieve" in {
    val H2DB = H2DBProvider.getInMemoryDB("test")
    runTestInsertAndGet(H2DB)
  }

  it should "stream" in {
    val H2DB = H2DBProvider.getInMemoryDB("test2")
    runTestStream(H2DB)
  }

  it should "handle mismatching cased table names quoted and unquoted" in {
    val timeout = BaseProviderSpec.DEFAULT_TIMEOUT
    val provider = H2DBProvider.getInMemoryDB("test-table-names")

    import provider.driver.api._

    val actions = DBIO.seq(
      sqlu"""CREATE TABLE USERS(`id` BIGINT NOT NULL);""",
      sqlu"""INSERT INTO users VALUES (1)"""
    )

    Await.result(provider.withDB(actions), timeout)

    val resultSetFut = provider.withDB(sql"""select id from "users";""".as[Int])
    val res = Await.result(resultSetFut, timeout)

    res should have length 1
    res.head should equal(1)
  }

  it should "build an H2 connection with a data source" in {
    val defaultConfiguration = TestConfig.default.toBasicRdb.copy(url = H2DBProvider.getInMemoryUrl("test3"))

    val dataSource = ManualSourceProviders.withConfig[org.h2.Driver](defaultConfiguration)

    val H2DB = new H2DBProvider(dataSource)

    runTestInsertAndGet(H2DB)
  }
}
