package io.paradoxical.rdb.slick.executors

import slick.util.AsyncExecutor
import scala.concurrent.ExecutionContext

object CustomAsyncExecutor {
  def apply(_executionContext: ExecutionContext, onClose: => Unit = {}): AsyncExecutor = new AsyncExecutor {
    override def executionContext: ExecutionContext = _executionContext

    override def close(): Unit = onClose
  }
}
