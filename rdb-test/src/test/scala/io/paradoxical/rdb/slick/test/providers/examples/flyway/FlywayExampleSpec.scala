package io.paradoxical.rdb.slick.test.providers.examples.flyway

import io.paradoxical.rdb.slick.providers.custom.H2DBProvider
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.scalatest._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.jdbc.meta.MTable

class FlywayExampleSpec extends FlatSpec with Matchers with Inspectors with BeforeAndAfterAll {
  private val h2 = H2DBProvider.getInMemoryDB("test_db")

  it should "create tables" in {
    val tables = Await.result(h2.withDB(MTable.getTables), 1 second).toList.map(_.name.name.toLowerCase)
    tables should contain allElementsOf List("cats", "clients")
  }

  it should "have inserted data" in {
    import h2.driver.api._

    // Raw SQL example with custom mapping
    val findCats = sql"""select * from cats where name = 'chuck';""".as[(Long, String, Long)]
    val cats = Await.result(h2.withDB(findCats), 1 second).map(FlywayCat.tupled).toList
    cats should have length 1

    val catAccess = new FlywayCats(h2.driver)
    val findCatPretty = catAccess.find(_.name === "chuck")
    val catOption = Await.result(h2.withDB(findCatPretty), 1 second)

    catOption shouldBe defined
    catOption.get.name should equal("chuck")
  }

  it should "be able to join data" in {
    import h2.driver.api._

    val clientAccess = new FlywayClients(h2.driver)
    val catAccess = new FlywayCats(h2.driver)

    val catsAndOwnersAction = (for {
      cat <- catAccess.query
      client <- clientAccess.query if cat.ownerClientId === client.id
    } yield (cat, client)).result

    val catsAndOwners = Await.result(h2.withDB(catsAndOwnersAction), 1 second)

    catsAndOwners should have length 1
    val (cat, owner) = catsAndOwners.head

    cat should equal(FlywayCat(1, "chuck", 1))
    owner should equal(FlywayClient(1))
  }

  override protected def beforeAll(): Unit = {
    setupDb(h2.dataSource)
  }

  override protected def afterAll(): Unit = {}

  private def setupDb(datasource: DataSource, schemaPath: String = "classpath:db/migration/h2"): Unit = {
    val flyway: Flyway = new Flyway()
    flyway.setDataSource(datasource)
    flyway.setLocations(schemaPath)
    val appliedMigrations = flyway.migrate()
    println(s"Applied $appliedMigrations migration(s)")
  }
}
