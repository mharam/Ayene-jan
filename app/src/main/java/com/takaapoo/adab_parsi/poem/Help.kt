package com.takaapoo.adab_parsi.poem

import android.animation.ValueAnimator
import android.content.res.Resources
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.graphics.toRectF
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.enlarge

class Help(private val poemFragment: PoemFragment) {

    val binding = poemFragment.binding
    private val poemViewModel = poemFragment.poemViewModel
//    private var currentPPF: PoemPagerFragment? = null

    val resources: Resources = poemFragment.resources
    private val initialRadius = 1500.dpTOpx(resources)

    private var help3Shown = false

    init {
        binding.helpDialog.c4.isVisible = false
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
                3 -> {
                    binding.helpDialog.root.visibility = View.INVISIBLE
                    help3Shown = false
                    closeHelp2 { openHelp3() }
                }
            }
        } else {
            binding.helpDialog.root.visibility = View.INVISIBLE
            binding.helpDialog.bullets.forEach { circ -> circ.isActivated = false }

            if (binding.flasher.isVisible)
                closeHelp1 {}

            if (binding.flasherDown.isVisible)
                closeHelp2 {}

            if (binding.helpFocusRect.isVisible)
                closeHelp3 {}
        }
    }

    private fun openHelp1(delay: Long){
        binding.helpDialog.mainText.text = resources.getString(R.string.help_poem_next_prev)

        binding.root.doOnLayout {
            binding.helpDialog.apply {
                root.x = (binding.root.width - root.width) / 2f
                root.doOnLayout { root.y = (binding.root.height - root.height) / 2f }

                dismiss.setOnClickListener{ poemViewModel.doneShowHelp() }
                next.setOnClickListener { poemViewModel.increaseShowHelp() }
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
        binding.root.doOnLayout {
            binding.helpDialog.apply {
                mainText.text = resources.getString(R.string.help_poem_down_drag)
                root.x = (binding.root.width - root.width) / 2f
                root.doOnLayout {
                    root.y = (binding.root.height / 5f + 216.dpTOpx(resources))
                        .coerceAtMost(binding.root.height - root.height - 16.dpTOpx(resources))
                }

                dismiss.setOnClickListener{ poemViewModel.doneShowHelp() }
                next.setOnClickListener { poemViewModel.increaseShowHelp() }
                c1.isActivated = true
                c2.isActivated = true
            }
        }

        binding.helpDialog.root.postDelayed({
            binding.helpDialog.root.visibility = View.VISIBLE
            binding.flasherDown.offseted = false
            binding.flasherDown.isVisible = true
        }, 300)
    }

    private fun closeHelp2(endAction: () -> Unit){
        binding.flasherDown.isVisible = false
        endAction()
    }

    private fun openHelp3(){
        val gap = 32.dpTOpx(resources)
        binding.helpDialog.mainText.text = resources.getString(R.string.help_poem_verse)
        binding.helpFocusRect.isVisible = false

//        currentPPF = poemFragment.childFragmentManager.findFragmentByTag("f${poemViewModel.poemPosition}")
//                as? PoemPagerFragment

        binding.root.doOnLayout {
            poemViewModel.poemListLayoutCompleted.observe(poemFragment.viewLifecycleOwner) { completed ->
                if (completed && !help3Shown){
                    help3Shown = true
                    binding.helpFocusRect.isVisible = true
                    val rect = poemFragment.visibleChildFrag?.firstVerseRectangle()!!

                    binding.helpDialog.apply {
                        val y1 = rect.bottom + binding.bookViewPager.top + gap
                        root.doOnLayout {
                            val y2 = rect.top + binding.bookViewPager.top - root.height - gap
                            root.y = (if (y1 + root.height + gap/2 > binding.root.height) y2 else y1)
                                .coerceAtLeast(0f)
                            root.x = rect.exactCenterX() - root.width / 2
                        }
                        dismiss.isVisible = false
                        next.text = resources.getString(R.string.got_it)
                        next.setOnClickListener { poemViewModel.doneShowHelp() }
                        c1.isActivated = true
                        c2.isActivated = true
                        c3.isActivated = true
                    }

                    ValueAnimator.ofFloat(initialRadius, 0f).apply {
                        duration = 400
                        addUpdateListener { updatedAnimation ->
                            val margin = (updatedAnimation.animatedValue as Float)
                            binding.helpFocusRect.rect = (rect.toRectF()).enlarge(margin)
                            binding.helpFocusRect.invalidate()
                        }
                        doOnEnd {
                            binding.helpDialog.root.visibility = View.VISIBLE
                        }
                    }.start()
                }
            }
        }
    }

    private fun closeHelp3(endAction: () -> Unit){
        val rect = poemFragment.visibleChildFrag?.firstVerseRectangle()!!.toRectF()

        ValueAnimator.ofFloat(0f, initialRadius).apply {
            duration = 400
            addUpdateListener { updatedAnimation ->
                val margin = (updatedAnimation.animatedValue as Float)
                binding.helpFocusRect.rect = rect.enlarge(margin)
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