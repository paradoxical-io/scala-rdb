package io.paradoxical.rdb.hikari.metrics

import com.zaxxer.hikari.metrics.{MetricsTracker, MetricsTrackerFactory, PoolStats}

// Easily define a Factory without creating a type
class InstanceMetricsTrackerFactory(provider: (String, PoolStats) => MetricsTracker) extends MetricsTrackerFactory {
  override def create(poolName: String, poolStats: PoolStats): MetricsTracker = provider(poolName, poolStats)
}

object DisabledMetricsTrackerFactory extends InstanceMetricsTrackerFactory((_, _) => DisabledMetricsTracker)

object LoggingMetricsTrackerFactory extends InstanceMetricsTrackerFactory((_, _) => LoggingMetricsTracker)
