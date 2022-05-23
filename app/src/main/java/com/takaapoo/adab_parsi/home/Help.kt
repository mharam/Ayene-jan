package com.takaapoo.adab_parsi.home

import android.animation.ValueAnimator
import android.content.res.Resources
import android.graphics.RectF
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.graphics.toRectF
import androidx.core.view.*
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.enlarge

class Help(private val homeFragment: HomeFragment) {

    val binding = homeFragment.binding
    val homeViewModel = homeFragment.homeViewModel

    val resources: Resources = homeFragment.resources

    private val initialRadius = 1500.dpTOpx(resources)
    private val finalRadius = 44.dpTOpx(resources)

    init {
        binding.helpDialog.c4.isVisible = false
    }

    fun showHelp(helpState: Int?, startDelay: Long) {

        if (helpState != null){
            when (helpState) {
                1 -> {
                    if (!binding.helpFocusCirc.isVisible){
                        binding.helpFocusCirc.isVisible = true
                        openHelp1(startDelay)
                    }
                }
                2 -> {
                    binding.helpDialog.root.visibility = View.INVISIBLE
                    closeHelp1 { openHelp2() }
                }
                3 -> {
                    binding.helpDialog.root.visibility = View.INVISIBLE
                    closeHelp2 { openHelp3() }
                }
            }
        } else {
            binding.helpDialog.root.visibility = View.INVISIBLE

            if (binding.helpFocusCirc.isVisible)
                closeHelp1 {}

            if (binding.helpFocusRect.isVisible){
                if (!binding.helpDialog.c3.isActivated)
                    closeHelp2 {}
                else
                    closeHelp3 {}
            }

            binding.helpDialog.bullets.forEach { circ -> circ.isActivated = false }
        }
    }


    private fun openHelp1(delay: Long){
        binding.helpDialog.apply {
            mainText.text = resources.getString(R.string.help_fab)
            binding.fab.doOnLayout {
                root.x = binding.fab.x + 44.dpTOpx(resources)
                root.y = binding.fab.y - 160.dpTOpx(resources)

                binding.helpFocusCirc.apply {
                    centerX = it.x + it.width/2
                    centerY = it.y + it.height/2
                }
            }
            dismiss.setOnClickListener{ homeViewModel.doneShowHelp() }
            next.setOnClickListener { homeViewModel.increaseShowHelp() }
            c1.isActivated = true
        }

        ValueAnimator.ofFloat(initialRadius, finalRadius).apply {
            duration = 400
            startDelay = delay
            addUpdateListener { updatedAnimation ->
                binding.helpFocusCirc.radius = (updatedAnimation.animatedValue as Float)
                binding.helpFocusCirc.invalidate()
            }
            doOnEnd {
                binding.helpDialog.root.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun closeHelp1(endAction: () -> Unit){
        ValueAnimator.ofFloat(finalRadius, initialRadius).apply {
            duration = 400
            addUpdateListener { updatedAnimation ->
                binding.helpFocusCirc.radius = (updatedAnimation.animatedValue as Float)
                binding.helpFocusCirc.invalidate()
            }
            doOnEnd {
                binding.helpFocusCirc.isVisible = false
                endAction()
            }
        }.start()
    }

    private fun openHelp2(){
        val gap = 16.dpTOpx(resources)
        binding.helpFocusRect.isVisible = true
        binding.helpDialog.mainText.text = resources.getString(R.string.help_search)

        binding.cardView.doOnLayout {
            val finalRectangle = RectF(it.left - gap, it.top - gap,
                it.right + gap, it.bottom + gap)

            binding.helpDialog.apply {
                root.doOnLayout {
                    root.x = binding.cardView.x + (binding.cardView.width - root.width) / 2
                    root.y = (binding.cardView.y + binding.cardView.height + 2 * gap)
                        .coerceAtMost((binding.root.height - root.height).toFloat())
                }
                dismiss.setOnClickListener{ homeViewModel.doneShowHelp() }
                next.setOnClickListener { homeViewModel.increaseShowHelp() }
                c1.isActivated = true
                c2.isActivated = true
            }
            binding.helpFocusRect.rect = finalRectangle

            ValueAnimator.ofFloat(initialRadius, 0f).apply {
                duration = 400
                addUpdateListener { updatedAnimation ->
                    val margin = (updatedAnimation.animatedValue as Float)
                    binding.helpFocusRect.rect = finalRectangle.enlarge(margin)
                    binding.helpFocusRect.invalidate()
                }
                doOnEnd {
                    binding.helpDialog.root.visibility = View.VISIBLE
                }
            }.start()

        }
    }

    private fun closeHelp2(endAction: () -> Unit){
        val finalRectangle = binding.helpFocusRect.rect
        ValueAnimator.ofFloat(0f, initialRadius).apply {
            duration = 400
            addUpdateListener { updatedAnimation ->
                val margin = (updatedAnimation.animatedValue as Float)
                binding.helpFocusRect.rect = finalRectangle.enlarge(margin)
                binding.helpFocusRect.invalidate()
            }
            doOnEnd {
                binding.helpFocusRect.isVisible = false
                endAction()
            }
        }.start()
    }

    private fun openHelp3(){
        val gap = 0
        binding.helpFocusRect.isVisible = true
        binding.helpDialog.mainText.text = resources.getString(R.string.help_delete)
        val rect = homeFragment.currentChildFragment?.firstItemRectangle()?.toRectF()

        binding.helpDialog.apply {
            val y1 = rect!!.bottom + binding.viewPager.top + 32.dpTOpx(resources)
            root.doOnLayout {
                val y2 = rect.top + binding.viewPager.top - root.height - 32.dpTOpx(resources)
                root.y = if (y1 + root.height + gap/2 > binding.root.height) y2 else y1
            }
            dismiss.isVisible = false
            next.text = resources.getString(R.string.got_it)
            next.setOnClickListener { homeViewModel.doneShowHelp() }
            c1.isActivated = true
            c2.isActivated = true
            c3.isActivated = true
        }

        val finalRectangle = if (rect?.isEmpty == true) RectF() else
            RectF(rect!!.left, rect.top + binding.viewPager.top,
                rect.right, rect.bottom + binding.viewPager.top)

        ValueAnimator.ofFloat(initialRadius, 0f).apply {
            duration = 400
            addUpdateListener { updatedAnimation ->
                val margin = (updatedAnimation.animatedValue as Float)
                binding.helpFocusRect.rect = finalRectangle.enlarge(margin)
                binding.helpFocusRect.invalidate()
            }
            doOnEnd {
                binding.helpDialog.root.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun closeHelp3(endAction: () -> Unit){
        val finalRectangle = binding.helpFocusRect.rect
        ValueAnimator.ofFloat(0f, initialRadius).apply {
            duration = 400
            addUpdateListener { updatedAnimation ->
                val margin = (updatedAnimation.animatedValue as Float)
                binding.helpFocusRect.rect = finalRectangle.enlarge(margin)
                binding.helpFocusRect.invalidate()
            }
            doOnEnd {
                binding.helpFocusRect.isVisible = false
                binding.helpDialog.apply {
                    dismiss.isVisible = true
                    next.text = resources.getString(R.string.next)
                }
                endAction()
            }
        }.start()
    }

}