package io.paradoxical.rdb.slick.providers

import java.util.Properties
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object SourceProviderUtils {
  def mergeProps(properties: Properties*): Properties = {
    val props = new Properties
    properties.foreach(props.putAll)
    props
  }

  val DEFAULT_THREAD_POOL_SIZE = 100

  protected lazy val defaultThreadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE)

  lazy val defaultExecutionContext = {
    ExecutionContext.fromExecutorService(defaultThreadPool)
  }
}
