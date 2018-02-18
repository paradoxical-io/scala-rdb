//
// DisabledMetricsTracker.scala


package io.paradoxical.rdb.hikari.metrics

import com.zaxxer.hikari.metrics.MetricsTracker

// Singleton MetricsTracker that just sends items into the abyss
// Useful so MetricsTracker instances don't have to keep being created
object DisabledMetricsTracker extends MetricsTracker
