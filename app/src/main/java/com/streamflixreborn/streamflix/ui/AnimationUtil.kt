package com.streamflixreborn.streamflix.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

object AnimationUtil {

    private const val PRESS_DURATION_MS = 150L
    private const val RELEASE_DURATION_MS = 200L
    private const val PRESS_SCALE = 0.92f

    fun bouncePress(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(PRESS_SCALE)
                        .scaleY(PRESS_SCALE)
                        .setDuration(PRESS_DURATION_MS)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(RELEASE_DURATION_MS)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
            }
            false
        }
    }

    fun fadeInView(view: View, durationMs: Long = 400L) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(durationMs)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    fun fadeInView(view: View, durationMs: Long, startDelayMs: Long) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(durationMs)
            .setStartDelay(startDelayMs)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    fun scaleIn(view: View, durationMs: Long = 300L) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = durationMs
            interpolator = FastOutSlowInInterpolator()
            start()
        }
    }

    fun staggerFadeIn(vararg views: View, staggerDelayMs: Long = 80L) {
        views.forEachIndexed { index, view ->
            fadeInView(view, 400L, index * staggerDelayMs)
        }
    }
}
