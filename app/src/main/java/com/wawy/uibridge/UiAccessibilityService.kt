package com.wawy.uibridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class UiAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: UiAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i("UiBridge", "Accessibility connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun clickText(value: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(value)
        return nodes.any { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) || clickParent(it) }
    }

    fun clickId(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        return nodes.any { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) || clickParent(it) }
    }

    fun inputText(value: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scrollForward(): Boolean {
        val root = rootInActiveWindow ?: return false
        return findScrollable(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
    }

    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun openApp(packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 250): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun swipeLeft(): Boolean {
        val dm = resources.displayMetrics
        val y = dm.heightPixels * 0.5f
        return swipe(dm.widthPixels * 0.85f, y, dm.widthPixels * 0.15f, y)
    }

    fun swipeRight(): Boolean {
        val dm = resources.displayMetrics
        val y = dm.heightPixels * 0.5f
        return swipe(dm.widthPixels * 0.15f, y, dm.widthPixels * 0.85f, y)
    }

    fun swipeUp(): Boolean {
        val dm = resources.displayMetrics
        val x = dm.widthPixels * 0.5f
        return swipe(x, dm.heightPixels * 0.8f, x, dm.heightPixels * 0.2f)
    }

    fun swipeDown(): Boolean {
        val dm = resources.displayMetrics
        val x = dm.widthPixels * 0.5f
        return swipe(x, dm.heightPixels * 0.2f, x, dm.heightPixels * 0.8f)
    }

    fun uiTreeSummary(): Map<String, Any?> {
        val root = rootInActiveWindow
        val nodes = mutableListOf<Map<String, Any?>>()
        fun walk(node: AccessibilityNodeInfo?) {
            if (node == null) return
            nodes.add(mapOf(
                "text" to node.text?.toString(),
                "id" to node.viewIdResourceName,
                "clickable" to node.isClickable,
                "editable" to node.isEditable,
                "class" to node.className?.toString()
            ))
            for (i in 0 until node.childCount) walk(node.getChild(i))
        }
        walk(root)
        return mapOf("package" to root?.packageName?.toString(), "nodes" to nodes)
    }

    private fun clickParent(node: AccessibilityNodeInfo): Boolean {
        var p = node.parent
        while (p != null) {
            if (p.isClickable && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            p = p.parent
        }
        return false
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }
}
