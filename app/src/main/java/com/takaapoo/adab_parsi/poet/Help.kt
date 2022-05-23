package com.takaapoo.adab_parsi.poet

import android.animation.ValueAnimator
import android.content.res.Resources
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.util.dpTOpx

class Help(val poetFragment: PoetFragment) {

//    val screamColor = poetFragment.requireContext().getColorFromAttr(R.attr.colorHelpScream)
    val binding = poetFragment.binding
    val poetViewModel = poetFragment.poetViewModel

//    private val searchScreamer = (binding.helpScrimmerCircle.drawable as GradientDrawable)
//    private val helpScrimDim = poetFragment.resources.getDimension(R.dimen.help_scream_dim)
    val resources: Resources = poetFragment.resources
    private val initialRadius = 1500.dpTOpx(resources)
    private val finalRadius = 24.dpTOpx(resources)

    init {
        binding.helpDialog.c4.isVisible = false
        binding.helpDialog.c3.isVisible = false
    }


    fun showHelp(helpState: Int?, startDelay: Long) {

        if (helpState != null){
            when (helpState) {
                1 -> {
                    if (!binding.flasher.isVisible){
                        openHelp1(startDelay)
                    }
                }
                2 -> {
                    binding.helpDialog.root.visibility = View.INVISIBLE
                    closeHelp1 { openHelp2() }
                }
            }
        } else {
            binding.helpDialog.root.visibility = View.INVISIBLE
            binding.helpDialog.bullets.forEach { circ -> circ.isActivated = false }

            if (binding.flasher.isVisible)
                closeHelp1 {}

            if (binding.helpFocusCirc.isVisible)
                closeHelp2 {}
        }
    }

    private fun openHelp1(delay: Long){
        binding.root.doOnLayout {
            binding.helpDialog.apply {
                mainText.text = resources.getString(R.string.help_poet_next_prev)
                root.x = (binding.root.width - root.width) / 2f
                root.doOnLayout { root.y = (binding.root.height - root.height) / 2f }

                dismiss.setOnClickListener{ poetViewModel.doneShowHelp() }
                next.setOnClickListener { poetViewModel.increaseShowHelp() }
                c1.isActivated = true
            }
        }

        binding.helpDialog.root.postDelayed({
            binding.helpDialog.root.visibility = View.VISIBLE
            binding.flasher.offseted = false
            binding.flasher.isVisible = true
        }, delay)
    }

    private fun closeHelp1(endAction: () -> Unit){
        binding.flasher.isVisible = false
        endAction()
    }

    private fun openHelp2(){
        val gap = 40.dpTOpx(resources)

        poetFragment.currentChildFragment?.binding?.toolbar?.doOnPreDraw {
            val searchIconPos = poetFragment.currentChildFragment?.searchIconCenter()
            binding.helpFocusCirc.apply {
                centerX = (searchIconPos?.get(0) ?: 0).toFloat()
                centerY = (searchIconPos?.get(1) ?: 0).toFloat()
                isVisible = true
            }

            binding.helpDialog.root.doOnLayout {
                binding.helpDialog.apply {
                    mainText.text = resources.getString(R.string.help_search_poet)
                    root.x = 64.dpTOpx(resources)
                    root.y = (searchIconPos?.get(1) ?: 0) + 40.dpTOpx(resources)
                    dismiss.isVisible = false
                    next.text = resources.getString(R.string.got_it)
                    next.setOnClickListener { poetViewModel.doneShowHelp() }
                    c1.isActivated = true
                    c2.isActivated = true
                }
            }
        }

        ValueAnimator.ofFloat(initialRadius, finalRadius).apply {
            duration = 400
            addUpdateListener { updatedAnimation ->
                binding.helpFocusCirc.radius = (updatedAnimation.animatedValue as Float)
                binding.helpFocusCirc.invalidate()
            }
            doOnEnd {
                binding.helpDialog.root.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun closeHelp2(endAction: () -> Unit){
        poetFragment.currentChildFragment?.binding?.toolbar?.doOnPreDraw {
            val searchIconPos = poetFragment.currentChildFragment?.searchIconCenter()
            binding.helpFocusCirc.apply {
                centerX = (searchIconPos?.get(0) ?: 0).toFloat()
                centerY = (searchIconPos?.get(1) ?: 0).toFloat()
                isVisible = true
            }
        }

        ValueAnimator.ofFloat(finalRadius, initialRadius).apply {
            duration = 400
            addUpdateListener { updatedAnimation ->
                binding.helpFocusCirc.radius = (updatedAnimation.animatedValue as Float)
                binding.helpFocusCirc.invalidate()
            }
            doOnEnd {
                binding.helpFocusCirc.isVisible = false
                binding.helpDialog.apply {
                    dismiss.isVisible = true
                    next.text = resources.getString(R.string.next)
                }
                endAction()
            }
        }.start()
    }



}