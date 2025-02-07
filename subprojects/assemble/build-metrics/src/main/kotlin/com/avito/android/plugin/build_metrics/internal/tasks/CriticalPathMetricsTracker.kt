package com.avito.android.plugin.build_metrics.internal.tasks

import com.avito.android.critical_path.CriticalPathListener
import com.avito.android.critical_path.TaskOperation
import com.avito.android.plugin.build_metrics.internal.module
import com.avito.android.plugin.build_metrics.internal.toSeriesName
import com.avito.android.stats.SeriesName
import com.avito.android.stats.StatsDSender
import com.avito.android.stats.TimeMetric
import com.avito.graph.OperationsPath
import com.avito.math.sumByLong

internal class CriticalPathMetricsTracker(
    private val metricsTracker: StatsDSender
) : CriticalPathListener {

    override fun onCriticalPathReady(path: OperationsPath<TaskOperation>) {
        path.operations
            .groupBy { taskOperation ->
                taskOperation.path.module.toSeriesName()
                    .append(taskOperation.type.simpleName)
            }
            .forEach { (groupName, taskOperations) ->
                val name = SeriesName.create("build", "tasks", "critical", "task").append(groupName)
                val durationMs = taskOperations.sumByLong { it.durationMs }

                metricsTracker.send(
                    TimeMetric(name, durationMs)
                )
            }
    }
}
