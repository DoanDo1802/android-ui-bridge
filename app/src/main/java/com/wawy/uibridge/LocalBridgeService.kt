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

        if (type == "status") {
            val res = JSONObject().put("ok", true).put("result", JSONObject(svc.statusSummary())).toString()
            return newFixedLengthResponse(Response.Status.OK, "application/json", res)
        }

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
            "tap_ratio" -> {
                val xr = json.optDouble("x", Double.NaN)
                val yr = json.optDouble("y", Double.NaN)
                if (xr.isNaN() || yr.isNaN()) false
                else {
                    val dm = svc.resources.displayMetrics
                    val px = (dm.widthPixels * xr).toFloat()
                    val py = (dm.heightPixels * yr).toFloat()
                    svc.tap(px, py)
                }
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
            "swipe_pages" -> {
                val count = json.optInt("count", 1).coerceIn(1, 10)
                val direction = json.optString("direction", "up")
                var ok = true
                repeat(count) {
                    val step = when (direction) {
                        "down" -> svc.swipeDown()
                        "left" -> svc.swipeLeft()
                        "right" -> svc.swipeRight()
                        else -> svc.swipeUp()
                    }
                    ok = ok && step
                    Thread.sleep(180)
                }
                ok
            }
            "swipe_left" -> svc.swipeLeft()
            "swipe_right" -> svc.swipeRight()
            "swipe_up" -> svc.swipeUp()
            "swipe_down" -> svc.swipeDown()
            "macro_youtube" -> {
                val query = json.optString("query", "a do mixi na na na a phung thanh do")
                val pages = json.optInt("pages", 2).coerceIn(0, 10)
                val tapY = json.optDouble("tapY", 0.56)
                val okOpen = svc.openApp("com.google.android.youtube")
                Thread.sleep(1200)
                val encoded = query.replace(" ", "+")
                val i = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://m.youtube.com/results?search_query=$encoded"))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                svc.startActivity(i)
                Thread.sleep(2200)
                repeat(pages) {
                    svc.swipeUp()
                    Thread.sleep(400)
                }
                val dm = svc.resources.displayMetrics
                val tapped = svc.tap((dm.widthPixels * 0.5f), (dm.heightPixels * tapY).toFloat())
                okOpen && tapped
            }
            "get_ui_tree" -> JSONObject(svc.uiTreeSummary())
            else -> return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"ok\":false,\"error\":\"unknown_action\"}")
        }

        val res = JSONObject().put("ok", true).put("result", result).toString()
        return newFixedLengthResponse(Response.Status.OK, "application/json", res)
    }
}
