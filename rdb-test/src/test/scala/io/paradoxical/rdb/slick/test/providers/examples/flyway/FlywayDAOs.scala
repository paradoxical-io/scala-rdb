package io.paradoxical.rdb.slick.test.providers.examples.flyway

import io.paradoxical.rdb.slick.dao.SlickDAO
import slick.jdbc.JdbcProfile

case class FlywayCat(id: Long, name: String, ownerClientId: Long)

class FlywayCats(val driver: JdbcProfile) extends SlickDAO {
  import driver.api._

  override type RowType = FlywayCat
  override type TableType = FlywayCatTable

  class FlywayCatTable(tag: Tag) extends DAOTable(tag, "cats") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def ownerClientId = column[Long]("owner_client_id")
    def * = (id, name, ownerClientId) <> (FlywayCat.tupled, FlywayCat.unapply)
  }

  override val query = TableQuery[FlywayCatTable]
}

case class FlywayClient(id: Long)

class FlywayClients(val driver: JdbcProfile) extends SlickDAO {
  import driver.api._

  override type RowType = FlywayClient
  override type TableType = FlywayClientTable

  class FlywayClientTable(tag: Tag) extends DAOTable(tag, "clients") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    override def * = id <> ((id: Long) => FlywayClient(id), FlywayClient.unapply)
  }

  override val query = TableQuery[FlywayClientTable]
}
