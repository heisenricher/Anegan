/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.designsystem.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Nova Haptic Feedback System — V3.2
 *
 * 10 distinct haptic patterns mapped to every interaction type in the app.
 * Each pattern has a unique tactile signature so users can "feel" the UI state.
 *
 * Pattern Taxonomy:
 * - TICK:      Single light tick (scroll snap, slider markers)
 * - CLICK:     Clean click (standard button taps)
 * - CONFIRM:   Satisfying thud (successful actions)
 * - REJECT:    Double buzz (errors, validation failures)
 * - TOGGLE:    Toggle snap (switches, checkboxes)
 * - LONG_PRESS: Deep press (context menus, long press actions)
 * - SWIPE_SNAP: Quick snap (swipe-to-dismiss, drawer snap)
 * - RECORDING:  Heartbeat pulse (active recording state)
 * - WARNING:    Alert double-tap (destructive action confirmation)
 * - SUCCESS:    Ascending crescendo (task completion)
 */
object NovaHaptics {

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Single light tick — scroll snapping, slider markers, minor interactions.
     */
    fun tick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Clean click — standard button taps, widget selections.
     */
    fun click(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Satisfying thud — successful actions (save, send, convert, generate).
     */
    fun confirm(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Double buzz — errors, validation failures, rejected inputs.
     */
    fun reject(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            val vibrator = getVibrator(view.context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 20, 50, 30), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 20, 50, 30), -1)
            }
        }
    }

    /**
     * Toggle snap — switch toggles, checkbox changes, segmented controls.
     */
    fun toggle(view: View) {
        val vibrator = getVibrator(view.context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25)
        }
    }

    /**
     * Deep press — long press actions, context menus.
     */
    fun longPress(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Quick snap — swipe-to-dismiss, drawer snap, bottom sheet anchor.
     */
    fun swipeSnap(view: View) {
        val vibrator = getVibrator(view.context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    /**
     * Heartbeat pulse — active recording state. Call in a loop with 1000ms interval.
     */
    fun recording(view: View) {
        val vibrator = getVibrator(view.context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 80, 60), // beat-pause-beat pattern
                    -1 // don't repeat — caller manages loop
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 40, 80, 60), -1)
        }
    }

    /**
     * Alert double-tap — destructive action confirmation (delete, clear all).
     */
    fun warning(view: View) {
        val vibrator = getVibrator(view.context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 20, 50, 30),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 20, 50, 30), -1)
        }
    }

    /**
     * Ascending crescendo — task completion, file saved, conversion done.
     */
    fun success(view: View) {
        val vibrator = getVibrator(view.context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 10, 30, 20, 30, 30), // ascending intensity feel
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 10, 30, 20, 30, 30), -1)
        }
    }
}
