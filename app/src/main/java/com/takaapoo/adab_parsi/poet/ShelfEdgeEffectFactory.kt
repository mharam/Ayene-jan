package com.takaapoo.adab_parsi.poet

import android.graphics.Canvas
import android.widget.EdgeEffect
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.util.FLING_TRANSLATION_MAGNITUDE
import com.takaapoo.adab_parsi.util.OVERSCROLL_TRANSLATION_MAGNITUDE


class ShelfEdgeEffectFactory(private val shelfRecyclerView: RecyclerView) : RecyclerView.EdgeEffectFactory() {

    override fun createEdgeEffect(recyclerView: RecyclerView, direction: Int): EdgeEffect {

        return object : EdgeEffect(recyclerView.context) {

            val anim =
                SpringAnimation(recyclerView, SpringAnimation.TRANSLATION_Y)
                    .setSpring(
                        SpringForce()
                        .setFinalPosition(0f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                        .setStiffness(SpringForce.STIFFNESS_LOW)
                    )

            override fun onPull(deltaDistance: Float) {
                super.onPull(deltaDistance)
                handlePull(deltaDistance)
            }

            override fun onPull(deltaDistance: Float, displacement: Float) {
                super.onPull(deltaDistance, displacement)
                handlePull(deltaDistance)
            }

            private fun handlePull(deltaDistance: Float) {
                // Translate the recyclerView with the distance
                val sign = if (direction == DIRECTION_BOTTOM) -1 else 1
                val translationDelta = sign *
                        recyclerView.height * deltaDistance * OVERSCROLL_TRANSLATION_MAGNITUDE

                recyclerView.translationY += translationDelta
                shelfRecyclerView.translationY += translationDelta
                anim.cancel()
            }

            override fun onRelease() {
                super.onRelease()
                // The finger is lifted. Start the animation to bring translation back to the resting state.
                anim.cancel()

                if (recyclerView.translationY != 0f)
                    anim.addUpdateListener { _, value, _ ->
                        shelfRecyclerView.translationY = value
                    }.start()
            }

            override fun onAbsorb(velocity: Int) {
                super.onAbsorb(velocity)
                // The list has reached the edge on fling.
                val sign = if (direction == DIRECTION_BOTTOM) -1 else 1
                val translationVelocity = sign * velocity * FLING_TRANSLATION_MAGNITUDE
                anim.cancel()
                anim.setStartVelocity(translationVelocity).addUpdateListener { _, value, _ ->
                    shelfRecyclerView.translationY = value
                }.start()
            }

            override fun draw(canvas: Canvas?): Boolean {
                // don't paint the usual edge effect
                return false
            }

            override fun isFinished(): Boolean {
                // Without this, will skip future calls to onAbsorb()
                return anim.isRunning.not()
            }
        }
    }
}