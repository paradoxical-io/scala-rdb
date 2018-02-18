package io.paradoxical.rdb.slick.providers

import java.util.concurrent.RejectedExecutionException
import scala.concurrent.{ExecutionContext, Future}
import slick.basic.DatabasePublisher
import slick.jdbc.JdbcProfile

/**
 * A base trait for implementing DB access providers
 */
trait SlickDBProvider {
  val dataSource: javax.sql.DataSource

  /**
   * The underlying driver
   */
  val driver: JdbcProfile

  import driver.api._

  private lazy val jdbcUrl = {
    val connection = dataSource.getConnection
    try {
      connection.getMetaData.getURL
    } finally {
      connection.close
    }
  }

  /**
   * Returns the DB instance value for this provider
   *
   * @return
   */
  def getDB: Database

  /**
   * Execute a DB action asynchronously
   *
   * @param action Action to execute
   * @return
   */
  def withDB[R, S <: NoStream, E <: Effect](action: DBIOAction[R, S, E])(implicit execCtx: ExecutionContext): Future[R] = {
    getDB.run(action).recoverWith {
      case _: RejectedExecutionException => Future.failed(SlickDBNoAvailableThreadsException("DB thread pool is busy and queue is full, try again"))
    }
  }

  /**
   * Execute a streamable DB action asynchronously
   *
   * @param action The streamable DB action
   * @return       A publisher that pushes streamed values to a subscriber
   */
  def withDBStream[R](action: StreamingDBIO[_, R]): DatabasePublisher[R] = {
    try {
      getDB.stream(action)
    } catch {
      case _: RejectedExecutionException => throw SlickDBNoAvailableThreadsException("DB thread pool is busy and queue is full, try again")
    }
  }

  /**
   * If the Future completes successfully, the DB is "healthy". In this case, "healthy" means a connection is established
   * and we can execute a simple query
   *
   * @return
   */
  def isHealthy()(implicit execCtx: ExecutionContext): Future[Unit] = withDB(sql"""SELECT 1;""".as[Int]).map(_ => {})
}

case class SlickDBNoAvailableThreadsException(message: String) extends Exception(message)
