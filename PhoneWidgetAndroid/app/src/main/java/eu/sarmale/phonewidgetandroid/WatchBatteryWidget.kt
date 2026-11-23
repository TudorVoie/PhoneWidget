package eu.sarmale.phonewidgetandroid

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import android.widget.RemoteViews
import java.util.Date

class WatchBatteryWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {
        private const val PREFS = "watch_battery_prefs"

        // Apelat din service când ceasul trimite o valoare nouă
        fun save(context: Context, pct: Int) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("pct", pct)
                .putLong("time", System.currentTimeMillis())
                .apply()
            refreshAll(context)
        }

        private fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WatchBatteryWidget::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val pct = prefs.getInt("pct", -1)
            val time = prefs.getLong("time", 0L)

            val views = RemoteViews(context.packageName, R.layout.widget_watch_battery)
            views.setTextViewText(R.id.widget_value, if (pct >= 0) "$pct%" else "--%")
            views.setTextViewText(
                R.id.widget_time,
                if (time > 0) DateFormat.getTimeFormat(context).format(Date(time)) else ""
            )

            // Tap pe widget = cere bateria ceasului chiar acum
            val intent = Intent(context, PhoneMonitorService::class.java)
                .setAction(PhoneMonitorService.ACTION_REQUEST_WATCH_BATTERY)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, 0, intent, flags)
            } else {
                PendingIntent.getService(context, 0, intent, flags)
            }
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            manager.updateAppWidget(id, views)
        }
    }
}