package io.paradoxical.rdb.slick.test.providers

import io.paradoxical.rdb.slick.providers.SlickDBNoAvailableThreadsException
import io.paradoxical.rdb.slick.test
import io.paradoxical.rdb.slick.test.{BaseProviderSpec, HikariDockerProvider, TestConfig}
import java.util.concurrent._
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import slick.util.AsyncExecutor.{PrioritizedRunnable, WithConnection}
import slick.util.ManagedArrayBlockingQueue

class HikariProviderSpec extends BaseProviderSpec with BeforeAndAfterAll {

  private val config = test.DbWithPool(TestConfig.default)

  private lazy val hikari = new HikariDockerProvider(config.db)

  trait Hikari {
    // Use tiny execution context for concurrency test below
    val provider = hikari.provider("test")(getExecutionContext(1, 1))
  }

  "HikariProvider" should "connect, insert, and retrieve" in new Hikari {
    runTestInsertAndGet(provider)
  }

  it should "stream" in new Hikari {
    runTestStream(provider)
  }

  ignore should "allow for overriding the executor" in new Hikari {
    import provider.driver.api._

    // We pass double the default queue size here to break Slick
    val futs = (0 until 2000).map(_ => {
      provider.withDB(sql"""select id from users;""".as[Int])
    })

    a[SlickDBNoAvailableThreadsException] should be thrownBy {
      Await.result(Future.sequence(futs), Duration.Inf)
    }

    val provider2 = hikari.provider("test")(getExecutionContext(50, 10000))

    val futs2 = (0 until 2000).map(_ => {
      provider2.withDB(sql"""select id from users;""".as[Int])
    })

    Await.result(Future.sequence(futs2), Duration.Inf)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    hikari.createDb("test")
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    hikari.dropDb("test")
    hikari.shutdown()
  }

  private def getExecutionContext(numThreads: Int, queueSize: Int): ExecutionContext = {
    val queue = new ManagedArrayBlockingQueue[slick.util.AsyncExecutor.PrioritizedRunnable](queueSize, 1) {
      def accept(r: Runnable, size: Int) = r match {
        case pr: PrioritizedRunnable if pr.priority != WithConnection => true
        case _ => size < queueSize
      }
    }.asInstanceOf[BlockingQueue[Runnable]]
    val executor = new ThreadPoolExecutor(numThreads, numThreads, 1, TimeUnit.MINUTES, queue, Executors.defaultThreadFactory())
    ExecutionContext.fromExecutor(executor)
  }
}
