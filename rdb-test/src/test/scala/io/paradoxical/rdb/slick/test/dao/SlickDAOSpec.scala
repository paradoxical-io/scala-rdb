package io.paradoxical.rdb.slick.test.dao

import io.paradoxical.global.tiny._
import io.paradoxical.rdb.slick.dao.SlickDAO
import io.paradoxical.rdb.slick.providers.custom.H2DBProvider
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.jdbc.JdbcProfile

class SlickDAOSpec extends FlatSpec with Matchers {
  private val DEFAULT_TIMEOUT = 30 seconds

  trait H2DB {
    val h2DBProvider = H2DBProvider.getInMemoryDB(getClass.getSimpleName)
    val testDAOAccess = new TestDAO(h2DBProvider.driver)
    val catsAccess = new Cats(h2DBProvider.driver)

    def createDB(): Unit = {
      Await.result(h2DBProvider.withDB(testDAOAccess.create), DEFAULT_TIMEOUT)
      Await.result(h2DBProvider.withDB(catsAccess.create), DEFAULT_TIMEOUT)
    }

    def insert(testDAO: TestDTO): Unit = {
      val action = testDAOAccess.insert(testDAO)
      Await.result(h2DBProvider.withDB(action), DEFAULT_TIMEOUT)
    }

    def insertCat(cat: Cat): Unit = {
      val action = catsAccess.insert(cat)
      Await.result(h2DBProvider.withDB(action), DEFAULT_TIMEOUT)
    }
  }

  "SlickDAO" should "create the table based on a DAO" in new H2DB {
    createDB()

    val resultSet = Await.result(h2DBProvider.withDB(testDAOAccess.all), DEFAULT_TIMEOUT)
    resultSet shouldBe empty
  }

  it should "insert a value" in new H2DB {
    createDB()
    insert(TestDTO("key", "value"))

    val resultSet = Await.result(h2DBProvider.withDB(testDAOAccess.all), DEFAULT_TIMEOUT)
    resultSet should have length 1
    resultSet.head.id should equal("key")
    resultSet.head.value should equal("value")
  }

  it should "insert and return a value" in new H2DB {
    import h2DBProvider.driver.api._

    createDB()

    val autoIncId = {
      val action = catsAccess.insert(Cat(0, "chuck", "1234"), _.id)
      Await.result(h2DBProvider.withDB(action), DEFAULT_TIMEOUT)
    }

    autoIncId should equal(1)
  }

  it should "update a value" in new H2DB {
    import testDAOAccess.driver.api._

    createDB()
    insert(TestDTO("key", "value"))

    val action = testDAOAccess.update(_.id === "key", TestDTO("key", "value2"))
    Await.result(h2DBProvider.withDB(action), DEFAULT_TIMEOUT)

    val resultSet = Await.result(h2DBProvider.withDB(testDAOAccess.all), DEFAULT_TIMEOUT)
    resultSet should have length 1
    resultSet.head.id should equal("key")
    resultSet.head.value should equal("value2")

    // Update specific field
    val action2 = testDAOAccess.updateWhere(_.id === "key", _.value, "value3")
    Await.result(h2DBProvider.withDB(action2), DEFAULT_TIMEOUT)

    val resultSet2 = Await.result(h2DBProvider.withDB(testDAOAccess.all), DEFAULT_TIMEOUT)
    resultSet2 should have length 1
    resultSet2.head.id should equal("key")
    resultSet2.head.value should equal("value3")
  }

  it should "insert or update" in new H2DB {
    createDB()
    insert(TestDTO("key", "value"))

    // Insert
    val action1 = testDAOAccess.insertOrUpdate(TestDTO("key2", "value2"))
    Await.result(h2DBProvider.withDB(action1), DEFAULT_TIMEOUT)
    val resultSet1 = Await.result(h2DBProvider.withDB(testDAOAccess.all), DEFAULT_TIMEOUT)
    resultSet1 should have length 2

    // Update
    val action2 = testDAOAccess.insertOrUpdate(TestDTO("key", "value3"))
    Await.result(h2DBProvider.withDB(action2), DEFAULT_TIMEOUT)
    val resultSet2 = Await.result(h2DBProvider.withDB(testDAOAccess.all), DEFAULT_TIMEOUT)
    resultSet2 should have length 2

    // Check final values
    val map = resultSet2.groupBy(_.id).mapValues(_.head)
    map("key2").value should equal("value2")
    map("key").value should equal("value3")
  }

  it should "get based on a predicate" in new H2DB {
    import testDAOAccess.driver.api._

    createDB()
    insert(TestDTO("key", "value"))

    val action = testDAOAccess.find(_.id === "key")
    val result = Await.result(h2DBProvider.withDB(action), DEFAULT_TIMEOUT)

    result should not be empty
    result.get.value should equal("value")

    val action2 = testDAOAccess.get(_.id === "key_bogus")
    val result2 = Await.result(h2DBProvider.withDB(action2), DEFAULT_TIMEOUT)
    result2 shouldBe empty

    insert(TestDTO("key2", "value"))
    val action3 = testDAOAccess.find(_.value === "value")
    val result3 = Await.result(h2DBProvider.withDB(action3), DEFAULT_TIMEOUT)

    result3 should not be empty
  }

  it should "return special typed columns" in new H2DB {
    import testDAOAccess.driver.api._

    // We need implicits here so we can query on the tiny type
    import testDAOAccess.implicits._

    createDB()

    insert(TestDTO("key", "value", optionalCol = Some(1L), colWithSpecialType = Some(ClientId(1L))))

    val result = Await.result(h2DBProvider.withDB(testDAOAccess.find(_.colWithSpecialType === ClientId(1L))), DEFAULT_TIMEOUT)
    result should not be empty
    result.get.colWithSpecialType should equal(Some(ClientId(1)))
  }

  it should "perform joins" in new H2DB {
    import h2DBProvider.driver.api._

    createDB()

    insert(TestDTO("key", "value"))
    insertCat(Cat(0, "seamus", "key"))

    // Join in SQL is explicit (uses JOIN keyword)
    val applicativeJoin = for {
      (cat, test) <- catsAccess.query join testDAOAccess.query on (_.testDaoId === _.id)
    } yield (cat, test)

    val (cat1, test1) = Await.result(h2DBProvider.withDB(applicativeJoin.result.head), DEFAULT_TIMEOUT)

    cat1.name should equal("seamus")
    test1.value should equal("value")

    // Join in SQL is implicit (does not use JOIN keyword)
    val monadicJoin = for {
      cat <- catsAccess.query
      test <- testDAOAccess.query if cat.testDaoId === test.id
    } yield (cat, test)

    val (cat2, test2) = Await.result(h2DBProvider.withDB(monadicJoin.result.head), DEFAULT_TIMEOUT)

    cat1.name should equal("seamus")
    test1.value should equal("value")
  }
}

case class TestDTO(
  id: String,
  value: String,
  optionalCol: Option[Long] = None,
  colWithText: Option[String] = None,
  colWithDefault: String = "",
  colWithSpecialType: Option[ClientId] = None
)

case class ClientId(value: Long) extends LongValue
case class RandomInt(value: Int) extends IntValue

class TestDAO(val driver: JdbcProfile) extends SlickDAO {
  // Import driver specific API
  import driver.api._
  // Import driver specific implicits for column mapping
  import implicits._

  implicit val clientIdMapper = mappedTiny[ClientId]

  override type RowType = TestDTO
  override type TableType = TestDAOTable

  // The actual table
  class TestDAOTable(tag: Tag) extends DAOTable(tag, "test_daos") {
    def id = column[String]("id", O.PrimaryKey)
    def value = column[String]("value")
    def optionalCol = column[Option[Long]]("optional_long")
    def colWithText = column[Option[String]]("optional_text", O.Length(255, varying = true), O.SqlType("text"))
    def colWithDefault = column[String]("col_with_default", O.Default("default_value!!"))
    def colWithSpecialType = column[Option[ClientId]]("client_id")
    override def * = (id, value, optionalCol, colWithText, colWithDefault, colWithSpecialType) <> (TestDTO.tupled, TestDTO.unapply)
    def idx = index("uniq_id", id, unique = true)
    def complexIdx = index("complex", (id, value))
  }

  override val query: driver.api.TableQuery[TestDAOTable] = TableQuery[TestDAOTable]
}

case class Cat(id: Long, name: String, testDaoId: String)

class Cats(val driver: JdbcProfile) extends SlickDAO {
  import driver.api._

  override type RowType = Cat
  override type TableType = CatsTable

  class CatsTable(tag: Tag) extends DAOTable(tag, "cats") {
    def id = column[Long]("id", O.AutoInc)
    def name = column[String]("name")
    def testDaoId = column[String]("test_dao_id")
    override def * = (id, name, testDaoId) <> (Cat.tupled, Cat.unapply)
  }

  override val query = TableQuery[CatsTable]
}
