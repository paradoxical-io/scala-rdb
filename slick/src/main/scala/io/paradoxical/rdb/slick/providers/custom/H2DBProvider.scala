package io.paradoxical.rdb.slick.providers.custom

import io.paradoxical.rdb.slick.providers.SlickDBProvider
import javax.inject.Inject
import javax.sql.DataSource
import slick.jdbc.{DriverDataSource, JdbcProfile}

object H2DBProvider {
  val KEEP_ALIVE_SETTING = (delay: Int) => "DB_CLOSE_DELAY" -> s"$delay"
  val KEEP_ALIVE_SETTING_DEFAULT = KEEP_ALIVE_SETTING(-1)
  val DATABASE_TO_UPPER = (on: Boolean) => "DATABASE_TO_UPPER" -> s"$on"
  val DATABASE_TO_UPPER_DEFAULT = DATABASE_TO_UPPER(false)

  val DEFAULT_SETTINGS = Map(KEEP_ALIVE_SETTING_DEFAULT, DATABASE_TO_UPPER_DEFAULT)

  def getInMemoryUrl(name: String, settings: Map[String, String] = DEFAULT_SETTINGS): String = {
    val urlOptions = settings.toList.map(tup => s"${tup._1}=${tup._2}").mkString(";", ";", "")
    s"jdbc:h2:mem:$name;$urlOptions"
  }

  def getInMemoryDataSource(name: String, settings: Map[String, String] = DEFAULT_SETTINGS): DataSource = {
    val url = getInMemoryUrl(name, settings)
    new DriverDataSource(url, driverObject = new org.h2.Driver)
  }

  def getInMemoryDB(name: String, settings: Map[String, String] = DEFAULT_SETTINGS): H2DBProvider = {
    new H2DBProvider(getInMemoryDataSource(name, settings))
  }
}

class H2DBProvider @Inject()(val dataSource: DataSource) extends SlickDBProvider {
  override val driver: JdbcProfile = slick.jdbc.H2Profile

  import driver.api._

  private lazy val db = Database.forDataSource(dataSource, None)

  override def getDB: Database = db
}
