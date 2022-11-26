/*
 * Copyright (c) Techbee e.U.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.techbee.jtx.widgets

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private const val TAG = "ListWidgetRec"


class ListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ListWidget()

    companion object {
        val list = stringSetPreferencesKey("list")
        val subtasks = stringSetPreferencesKey("subtasks")
        val subnotes = stringSetPreferencesKey("subnotes")
        val filterConfig = stringPreferencesKey("filter_config")

        /**
         * Sets a worker to update the widget
         * @param delay: if true the update of the widget is delayed by 2 seconds and the existing work is replaced ( in order to not trigger multiple works in a row )
         * if false the update of the widget is done immediately
         */
        fun setOneTimeWork(context: Context, delay: Duration? = null) {
            val work: OneTimeWorkRequest = OneTimeWorkRequestBuilder<ListWidgetUpdateWorker>()
                .apply {
                    if (delay != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        setInitialDelay(delay.toJavaDuration())
                }.build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                    "listWidgetOneTimeWorker",
                    ExistingWorkPolicy.REPLACE,
                    work
                )
            Log.d(TAG, "Work enqueued")
        }
    }
}