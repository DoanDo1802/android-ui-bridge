package com.wawy.uibridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class LocalBridgeService : Service() {
    private var server: ActionServer? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notif = Notification.Builder(this, "ui_bridge")
            .setContentTitle("UI Bridge running")
            .setContentText("Listening on 127.0.0.1:8080")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1001, notif)
        server = ActionServer()
        server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    }

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("ui_bridge", "UI Bridge", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}

class ActionServer : NanoHTTPD("127.0.0.1", 8080) {
    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.POST || session.uri != "/action") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"ok\":false,\"error\":\"not_found\"}")
        }

        val body = HashMap<String, String>()
        session.parseBody(body)
        val payload = body["postData"] ?: "{}"
        val json = JSONObject(payload)
        val type = json.optString("type")
        val value = json.optString("value")

        if (type == "health") {
            val ok = UiAccessibilityService.instance != null
            return newFixedLengthResponse(Response.Status.OK, "application/json", JSONObject(mapOf("ok" to true, "result" to mapOf("accessibilityReady" to ok))).toString())
        }

        val svc = UiAccessibilityService.instance
            ?: return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, "application/json", "{\"ok\":false,\"error\":\"accessibility_not_ready\"}")

        val result: Any = when (type) {
            "click_text" -> svc.clickText(value)
            "click_id" -> svc.clickId(value)
            "input_text" -> svc.inputText(value)
            "scroll" -> svc.scrollForward()
            "back" -> svc.goBack()
            "home" -> svc.goHome()
            "recent" -> svc.openRecents()
            "open_app" -> svc.openApp(value)
            "tap" -> {
                val x = json.optDouble("x", Double.NaN)
                val y = json.optDouble("y", Double.NaN)
                if (x.isNaN() || y.isNaN()) false else svc.tap(x.toFloat(), y.toFloat())
            }
            "swipe" -> {
                val x1 = json.optDouble("x1", Double.NaN)
                val y1 = json.optDouble("y1", Double.NaN)
                val x2 = json.optDouble("x2", Double.NaN)
                val y2 = json.optDouble("y2", Double.NaN)
                val duration = json.optLong("durationMs", 250)
                if (x1.isNaN() || y1.isNaN() || x2.isNaN() || y2.isNaN()) false
                else svc.swipe(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), duration)
            }
            "swipe_left" -> svc.swipeLeft()
            "swipe_right" -> svc.swipeRight()
            "swipe_up" -> svc.swipeUp()
            "swipe_down" -> svc.swipeDown()
            "get_ui_tree" -> JSONObject(svc.uiTreeSummary())
            else -> return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"ok\":false,\"error\":\"unknown_action\"}")
        }

        val res = JSONObject().put("ok", true).put("result", result).toString()
        return newFixedLengthResponse(Response.Status.OK, "application/json", res)
    }
}
