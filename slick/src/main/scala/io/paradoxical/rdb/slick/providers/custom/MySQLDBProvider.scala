//
// MySqlDBProvider.scala


package io.paradoxical.rdb.slick.providers.custom

import io.paradoxical.rdb.slick.executors.CustomAsyncExecutor
import io.paradoxical.rdb.slick.providers.SlickDBProvider
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcProfile

/**
 * MySQL provider which encapsulates connecting and querying
 * @param dataSource
 * @param executionContext
 */
class MySQLDBProvider @Inject()(
  val dataSource: javax.sql.DataSource
)(implicit executionContext: ExecutionContext) extends SlickDBProvider {
  override val driver: JdbcProfile = slick.jdbc.MySQLProfile

  import driver.api._

  protected lazy val db = Database.forDataSource(
    dataSource, None, executor = CustomAsyncExecutor(executionContext)
  )

  override def getDB: Database = db
}

