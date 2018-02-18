package io.paradoxical.rdb.slick.test

import io.paradoxical.rdb.config.BasicRdbConfig
import io.paradoxical.rdb.hikari.config.RdbConfigWithConnectionPool
import io.paradoxical.rdb.slick.providers.SlickDBProvider
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object BaseProviderSpec {
  val DEFAULT_TIMEOUT = 30 seconds
}

case class SimpleDbConfig(db: BasicRdbConfig)
case class DbWithPool(db: RdbConfigWithConnectionPool)

class BaseProviderSpec extends FlatSpec with Matchers {
  protected def runTestInsertAndGet(provider: SlickDBProvider, timeout: FiniteDuration = BaseProviderSpec.DEFAULT_TIMEOUT): Unit = {
    import provider.driver.api._

    val actions = DBIO.seq(
      sqlu"""CREATE TABLE users(`id` BIGINT NOT NULL);""",
      sqlu"""INSERT INTO users VALUES (1)"""
    )

    Await.result(provider.withDB(actions), timeout)

    val resultSetFut = provider.withDB(sql"""select id from users;""".as[Int])
    val res = Await.result(resultSetFut, timeout)

    res should have length 1
    res.head should equal(1)
  }

  protected def runTestStream(provider: SlickDBProvider, timeout: FiniteDuration = BaseProviderSpec.DEFAULT_TIMEOUT): Unit = {
    import provider.driver.api._

    val range = 0 to 50

    val records = range.map(i => {
      sqlu"""INSERT INTO users_stream VALUES ($i);"""
    }).toList


    val actions = DBIO.seq(
      (sqlu"""CREATE TABLE users_stream(`id` BIGINT NOT NULL);""" +: records): _*
    )

    Await.result(provider.withDB(actions), timeout)

    val resultSetPublisher = provider.withDBStream(sql"""select id from users_stream;""".as[Int])

    var sum = 0
    val fut = resultSetPublisher.foreach(sum += _)
    Await.result(fut, timeout)

    sum should equal (range.sum)
  }
}
