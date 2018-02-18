//
// SlickDAO.scala


package io.paradoxical.rdb.slick.dao

import io.paradoxical.global.tiny._
import java.lang.{Long => JLong}
import java.util.UUID
import org.joda.time.{DateTime, DateTimeZone}
import slick.jdbc.JdbcProfile
import slick.lifted.CanBeQueryCondition

object SlickDAO {
  class Implicits(val driver: JdbcProfile) {

    import driver.api._

    implicit val dateTimeToMillisMapper = MappedColumnType.base[DateTime, Long](_.getMillis, new DateTime(_).withZone(DateTimeZone.UTC))

    implicit def mappedTiny[T <: ValueType[_] : Manifest] = {
      val tiny = manifest[T].runtimeClass
      val constructor = tiny.getConstructors.head

      def newInstance[U <: Object](value: U): T = constructor.newInstance(value).asInstanceOf[T]

      tiny match {
        case _ if classOf[StringValue].isAssignableFrom(tiny) => MappedColumnType.base[T, String](_.asInstanceOf[StringValue].value, newInstance(_))
        case _ if classOf[LongValue].isAssignableFrom(tiny) => MappedColumnType.base[T, Long](_.asInstanceOf[LongValue].value, v => newInstance(new JLong(v)))
        case _ if classOf[IntValue].isAssignableFrom(tiny) => MappedColumnType.base[T, Int](_.asInstanceOf[IntValue].value, v => newInstance(new Integer(v)))
        case _ if classOf[DoubleValue].isAssignableFrom(tiny) => MappedColumnType.base[T, Double](_.asInstanceOf[DoubleValue].value, v => newInstance(new java.lang.Double(v)))
        case _ if classOf[FloatValue].isAssignableFrom(tiny) => MappedColumnType.base[T, Float](_.asInstanceOf[FloatValue].value, v => newInstance(new java.lang.Float(v)))
        case _ if classOf[UuidValue].isAssignableFrom(tiny) => MappedColumnType.base[T, UUID](_.asInstanceOf[UuidValue].value, newInstance(_))
        case x => throw new IllegalArgumentException(s"Unsupported tiny type of $x")
      }
    }
  }
}

/**
 * A base trait for creating Slick-backed DAOs
 *
 * Example implementation:
 * {{{
 * case class TestDTO(id: String, value: String)
 *
 * class TestDAO(val driver: JdbcProfile) extends SlickDAO {
 *   import driver.api._
 *
 *   override type RowType = TestDTO
 *   override type TableType = TestDAOs
 *
 *   // The actual table
 *   class TestDAOs(tag: Tag) extends DAOTable(tag, "test_daos") {
 *     def id = column[String]("id", O.PrimaryKey)
 *     def value = column[String]("value")
 *   }
 *
 *   override val query: driver.api.TableQuery[TestDAOs] = TableQuery[TestDAOs]
 * }
 * }}}
 *
 * Generally, SlickDAOs are bundled in a DB access class which exposes "lazy val" instances of their "query" value
 *
 * Example DB Access class
 * {{{
 * class MyDBAccess(driver: JdbcProfile) {
 *   lazy val test = new TestDAO(driver)
 * }
 *
 * val provider = new H2DBProvider()
 * val dbAccess = new MyDBAccess(provider.driver)
 * val future = provider.withDB(dbAccess.test.all)
 * }}}
 */
trait SlickDAO {
  // The driver that will be used for querying
  val driver: JdbcProfile

  import driver.api._

  // Common implicit conversions
  // make sure this is lazy to defer constructor initializers
  lazy val implicits = new SlickDAO.Implicits(driver)

  /**
   * The types of objects within the table
   * This is generally a DTO type defined outside of the SlickDAO class
   */
  type RowType

  /**
   * The type of the table itself, i.e. the concrete class that overrides DAOTable below
   */
  type TableType <: DAOTable

  /**
   * Extend this class with the actual table definition
   * You should never have to instantiate this class directly, but rather reference it
   * through `query` below and any operations. Moreover, the "tag" argument does not need to be
   * implemented and should be a part of the implementing class' constructor.
   * Ex. class ConcreteDAOTable(tag: Tag) extends DAOTable(tag, "test_table")e
   */
  abstract class DAOTable(tag: Tag, name: String) extends Table[RowType](tag, name)

  /**
   * Define how to construct a table query for this table type. Generally just TableQuery[TableType]
   * We can't "generalize" this because the type parameter for TableQuery expects a concrete class
   */
  val query: TableQuery[TableType]

  /**
   * Create the table defined by DAOTable's type
   *
   * @return
   */
  def create: DBIO[Unit] = query.schema.create

  /**
   * Insert a new row
   *
   * @param row
   * @return
   */
  def insert(row: RowType): DBIO[Unit] = (query += row) andThen DBIO.successful(())

  /**
   * Insert a new row and return an auto-incrementing column
   *
   * @param row
   * @param returningFunc
   * @return
   */
  def insert[F, G, T](
    row: RowType,
    returningFunc: TableType => F
  )(implicit shape: Shape[_ <: FlatShapeLevel, F, T, G]): DBIO[T] = {
    (query returning map(returningFunc)) += row
  }

  /**
   * Insert or update a row
   *
   * @param row The new row
   * @return
   */
  def insertOrUpdate(row: RowType): DBIO[Unit] = (query insertOrUpdate row) andThen DBIO.successful(())

  /**
   * Insert or update a row and return the auto-incrementing column
   *
   * @param row
   * @param returningFunc Mapping function specifying data to return from the query
   * @return
   */
  def insertOrUpdate[F, G, T](
    row: RowType,
    returningFunc: TableType => F
  )(implicit shape: slick.lifted.Shape[_ <: FlatShapeLevel, F, T, G]): DBIO[Option[T]] = {
    (query returning map(returningFunc)) insertOrUpdate row
  }

  /**
   * Full update convenience method
   *
   * @param where  The filter in which to select rows
   * @param newRow The new row
   * @return
   */
  def update[Where <: Rep[_]](
    where: TableType => Where,
    newRow: TableType#TableElementType
  )(implicit wt: CanBeQueryCondition[Where]): DBIO[Int] = {
    updateWhere(where, identity, newRow)
  }

  /**
   * Full or partial update of rows matching a predicate
   *
   * Example:
   * Scala: MyDAO.updateWhere(_.id === 3, _.name, "Christian")
   * SQL: update my_daos set name = 'Christian' where id = 3;
   *
   * @param where     The filter in which to select rows
   * @param mapper    Function which maps the row type to a product of columns to update
   * @param newRow    The new value to update the found row(s).
   * @return          Action describing whether the update occurred
   */
  def updateWhere[F, G, T, Where <: Rep[_]](
    where: TableType => Where,
    mapper: TableType => F,
    newRow: T
  )(implicit shape: slick.lifted.Shape[_ <: FlatShapeLevel, F, T, G], wt: CanBeQueryCondition[Where]) = {
    query.filter(where).map(mapper).update(newRow)
  }

  /**
   * Get all items in the table
   *
   * @return
   */
  def all: StreamingDBIO[Seq[RowType], RowType] = query.result

  /**
   * Get all items matching the given where clause
   *
   * @param where The where clause to match
   * @return
   */
  def get[T <: Rep[_]](where: TableType => T)(implicit wt: CanBeQueryCondition[T]): DBIO[Seq[RowType]] = {
    query.filter(where).result
  }

  /**
   * Return the first result matching the given where clause
   *
   * @param where The where clause to match
   * @return
   */
  def find[T <: Rep[_]](where: TableType => T)(implicit wt: CanBeQueryCondition[T]): DBIO[Option[RowType]] = {
    query.filter(where).result.headOption
  }

  // Alias for map to fix type inference issues with returning contextd
  protected def map[F, G, T](returningFunc: TableType => F)(implicit shape: slick.lifted.Shape[_ <: FlatShapeLevel, F, T, G]): Query[G, T, Seq] = {
    query.map(returningFunc)
  }
}

