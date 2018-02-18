package io.paradoxical.rdb.slick.providers.custom

import io.paradoxical.rdb.config.BasicRdbConfig
import io.paradoxical.rdb.slick.providers.SourceProviderUtils
import java.sql.Driver
import java.util.Properties
import javax.sql.DataSource
import slick.jdbc.DriverDataSource

/**
 * Providers using a manual driver, does not provide connection pooling by default!
 */
object ManualSourceProviders {
  import SourceProviderUtils._

  /**
   * A null data source. Used for injection into tests when using providers that don't need data sources (e.g. H2)
   */
  val nullDataSource = new DriverDataSource(null)

  /**
   * Construct a MySQLDBProvider with a given config
   *
   * @param config
   * @return
   */
  def withConfig[T <: Driver : Manifest](config: BasicRdbConfig): DataSource = {
    withConfigAndProps[T](config, new Properties())
  }

  /**
   *
   * @param config
   * @param properties
   * @tparam T
   * @return
   */
  def withConfigAndProps[T <: Driver : Manifest](
    config: BasicRdbConfig,
    properties: Properties
  ): DataSource = {
    val props = mergeProps(config.security.map(_.toJdbcProps).getOrElse(new Properties()), properties)

    new DriverDataSource(
      config.url,
      config.credentials.user,
      config.credentials.password,
      props,
      driverObject = manifest[T].runtimeClass.newInstance().asInstanceOf[T]
    )
  }
}
