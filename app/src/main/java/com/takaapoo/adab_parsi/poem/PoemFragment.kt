package com.takaapoo.adab_parsi.poem

import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.book.BookViewModel
import com.takaapoo.adab_parsi.poet.PoetViewModel
import com.takaapoo.adab_parsi.util.allUpCategories
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class PoemFragment : BasePoemFragment(){

    private val bookViewModel: BookViewModel by activityViewModels()
    private val poetViewModel: PoetViewModel by activityViewModels()
    private var bookItemID: Int? = null

    private val pageCallBack = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            visibleChildFrag = childFragmentManager.findFragmentByTag("f${position}")
                    as? PoemPagerFragment
            visibleChildFrag?.firebaseLog()

            val poemItem = poemViewModel.poemList[position]
            bookViewModel.bookCurrentItem?.id?.let {
                bookViewModel.bookContentScrollPosition[it] = poemItem!!
            }

            val upCategories = allUpCategories(poemItem?.parentID)
            if (position != poemViewModel.poemPosition){
                upCategories.subList(0, (upCategories.size-2).coerceAtLeast(0))
                    .forEach { id -> bookViewModel.listOpen[id] = true }

                poemViewModel.poemPosition = position
            }

            childFragmentManager.apply {
                (findFragmentByTag("f${position-1}") as? PoemPagerFragment)?.apply {
                    fullBookPage()
                    finishActionMode()
                }
                (findFragmentByTag("f${position+1}") as? PoemPagerFragment)?.apply {
                    fullBookPage()
                    finishActionMode()
                }
                (findFragmentByTag("f${position}") as? PoemPagerFragment)?.fullBookPage()
            }

            super.onPageSelected(position)
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            visibleChildFrag?.deselectText()
        }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        poemViewModel.bookContentScrollY = 0
        if (poemViewModel.contentShot != null)
            binding.bookImage.setImageBitmap(poemViewModel.contentShot)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            if (poemViewModel.poemList.isEmpty()){
                poetViewModel.let {
                    it.sortedPoemItems()
                    poemViewModel.poemList = it.poemListItems[it.bookListItems[it.bookPosition]!!.id]!!
                    poemViewModel.poemCount = poemViewModel.poemList.size
                }
            }

            if (bookItemID != bookViewModel.bookCurrentItem?.id ||
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                viewPager.apply {
                    adapter = PoemAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,
                        poemViewModel.poemCount, poemViewModel.poemList)
                    setCurrentItem(poemViewModel.poemPosition, false)
                    registerOnPageChangeCallback(pageCallBack)
                    post { pageCallBack.onPageSelected(currentItem) }
                }
                bookItemID = bookViewModel.bookCurrentItem?.id

            } else {
                viewPager.setCurrentItem(poemViewModel.poemPosition, false)
            }
            viewPager.setPageTransformer(TurnPageTransformer())
            viewPager.offscreenPageLimit = 1

            view.doOnLayout {
                binding.bookImage.apply {
                    doOnLayout {
                        pivotX = width.toFloat()
                        pivotY = height / 2f
                        cameraDistance = 30f * width

                        if (poemViewModel.poemFirstOpening &&
                            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            ObjectAnimator.ofFloat(this@apply, "rotationY", 0f, 90f).apply {
                                startDelay = 800
                                duration = 600
                                interpolator = LinearInterpolator()
                                doOnEnd {
                                    poemViewModel.contentShot = null
                                    binding.bookImage.setImageDrawable(null)
                                }
                            }.start()
                        } else
                            binding.bookImage.rotationY = 90f

                        poemViewModel.poemFirstOpening = false
                    }
                }
            }
        }

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val firstFragEntrance = preferenceManager.getBoolean("poemFragFirstEnter", true)
        if (firstFragEntrance) {
            poemViewModel.doShowHelp()
            preferenceManager.edit().putBoolean("poemFragFirstEnter", false).apply()
        }

        val help = Help(this)
        poemViewModel.showHelp.observe(viewLifecycleOwner) {
            help.showHelp(it, if (firstFragEntrance) 2500 else 500)
        }
    }

    override fun onPause() {
        super.onPause()
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            (activity as MainActivity).showSystemBars()

        if (activity?.isChangingConfigurations == false)
            poemViewModel.doneShowHelp()
    }

    override fun onDestroyView() {
        viewPager.unregisterOnPageChangeCallback(pageCallBack)
        super.onDestroyView()
    }

    override fun getFragment(): PoemPagerFragment? {
        return childFragmentManager.findFragmentByTag("f${poemViewModel.poemPosition}")
                as? PoemPagerFragment
    }

}


class TurnPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.apply {
            if (pivotX < width){
                pivotX = width.toFloat()
                cameraDistance = 30f * width
            }
            when {
                position <= -1 -> { // [-Infinity,-1]
                    translationX = 0f
                    rotationY = 90f
                    translationZ = 0f
                }
                position < 1 -> { // (-1,1)
                    x = 0f
                    if (position <= 0){
                        rotationY = position * (-90f)
                        translationZ = 4f
                    } else{
                        rotationY = 0f
                        translationZ = 0f
                    }
                }
                else -> { // [1,+Infinity]
                    translationX = 0f
                    rotationY = 0f
                    translationZ = 0f
                }
            }
        }
    }
}