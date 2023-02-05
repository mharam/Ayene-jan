package com.takaapoo.adab_parsi.poet

import android.graphics.Canvas
import android.widget.EdgeEffect
import androidx.core.view.children
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.util.FLING_TRANSLATION_MAGNITUDE
import com.takaapoo.adab_parsi.util.OVERSCROLL_TRANSLATION_MAGNITUDE

class PoetBounceEdgeEffectFactory(private val fragment: FragmentWithTransformPage)
    : RecyclerView.EdgeEffectFactory() {

    override fun createEdgeEffect(recyclerView: RecyclerView, direction: Int): EdgeEffect {

        val firstChild = recyclerView.children.first()
        val lastChild = recyclerView.children.last()

        val floatValueHolder = FloatValueHolder(0f)
        val anim = SpringAnimation(floatValueHolder)
            .setSpring(
                SpringForce(0f)
                .setDampingRatio(0.6f)
                .setStiffness(SpringForce.STIFFNESS_LOW)
            )

        return object : EdgeEffect(recyclerView.context) {

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
                val sign = if (direction == DIRECTION_RIGHT) 1 else -1
                val translationDelta = sign * recyclerView.width * deltaDistance * OVERSCROLL_TRANSLATION_MAGNITUDE

                floatValueHolder.value +=  translationDelta
                if (direction == DIRECTION_RIGHT)
                    fragment.transformPage(firstChild, floatValueHolder.value / recyclerView.width)
                else if (direction == DIRECTION_LEFT)
                    fragment.transformPage(lastChild, floatValueHolder.value / recyclerView.width)

                anim?.cancel()
            }

            override fun onRelease() {
                super.onRelease()
                // The finger is lifted. Start the animation to bring translation back to the resting state.
                if (!isFinished)
                    anim.cancel()

                if (floatValueHolder.value != 0f){
                    if (direction == DIRECTION_RIGHT) {
                        anim.addUpdateListener { _, value, _ ->
                            fragment.transformPage(firstChild, value / recyclerView.width)
                        }.start()
                    }
                    else if (direction == DIRECTION_LEFT) {
                        anim.addUpdateListener { _, value, _ ->
                            fragment.transformPage(lastChild, value / recyclerView.width)
                        }.start()
                    }
                }
            }

            override fun onAbsorb(velocity: Int) {
                super.onAbsorb(velocity)
                // The list has reached the edge on fling.
                val sign = if (direction == DIRECTION_RIGHT) 1 else -1
                val translationVelocity = sign * recyclerView.width * velocity * FLING_TRANSLATION_MAGNITUDE
                anim?.cancel()
                anim.setStartVelocity(translationVelocity)?.also {
                    if (direction == DIRECTION_RIGHT) {
                        it.addUpdateListener { _, value, _ ->
                            fragment.transformPage(firstChild, value / recyclerView.width)
                        }.start()
                    }
                    else if (direction == DIRECTION_LEFT) {
                        it.addUpdateListener { _, value, _ ->
                            fragment.transformPage(lastChild, value / recyclerView.width)
                        }.start()
                    }
                }
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