package com.gemofgemma.accessibility

// NOT YET WIRED — reserved for future UI-automation tool, not called by ActionDispatcher.

import android.graphics.Rect

/**
 * Interface for the future UI-automation integration.
 *
 * NOT YET WIRED — reserved for future UI-automation tool, not called by
 * ActionDispatcher. Keep this contract isolated until a concrete action and
 * SafetyValidator policy are added.
 */
interface AccessibilityBridge {
    fun isServiceEnabled(): Boolean
    fun executeGlobalAction(action: Int): Boolean
    fun findAndClickByText(text: String): Boolean
    fun findAndClickById(viewId: String): Boolean
    fun typeText(text: String): Boolean
    fun getScreenContent(): List<NodeInfo>
}

data class NodeInfo(
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val isClickable: Boolean,
    val bounds: Rect?
)
