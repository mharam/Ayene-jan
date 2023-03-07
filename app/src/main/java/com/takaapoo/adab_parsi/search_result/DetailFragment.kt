package com.takaapoo.adab_parsi.search_result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.transition.doOnEnd
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.poem.BasePoemFragment
import com.takaapoo.adab_parsi.poem.PoemAdapter
import com.takaapoo.adab_parsi.poem.PoemPagerFragment
import com.takaapoo.adab_parsi.search.SearchViewModel
import com.takaapoo.adab_parsi.util.getColorFromAttr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class DetailFragment: BasePoemFragment() {

    private val searchViewModel: SearchViewModel by activityViewModels()

    private val pageCallBack = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            visibleChildFrag = childFragmentManager.findFragmentByTag("f${position}")
                    as? PoemPagerFragment
            searchViewModel.poemPosition = position

            if (searchViewModel.detailPagerPosition != position){
                visibleChildFrag?.resultItemMeasure()
                searchViewModel.detailPagerPosition = position
                visibleChildFrag?.moveToSearchedPosition()
            } else if (!searchViewModel.comeFromResultFragment) { // when coming from other fragments
                visibleChildFrag?.moveToSearchedPosition()
            }

            searchViewModel.comeFromResultFragment = false

            childFragmentManager.apply {
                (findFragmentByTag("f${position-1}") as? PoemPagerFragment)?.finishActionMode()
                (findFragmentByTag("f${position+1}") as? PoemPagerFragment)?.finishActionMode()
            }
//                mPoemTextMenu.ppf = childFragmentManager.findFragmentByTag("f${position}") as PoemPagerFragment
        }
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            visibleChildFrag?.deselectText()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding.bookImage.visibility = View.GONE
        searchViewModel.apply {
            comeFromDetailFragment = true
            openedFromDetailFrag = true
//            resultListDisplace = 0
//            bottomViewedResultHeight = 0
//            topViewedResultHeight = 0
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        motionInitialization()

        viewLifecycleOwner.lifecycleScope.launch {
            if (searchViewModel.poemList.isEmpty()) {
                val finalResult = searchViewModel.search()
                poemViewModel.resultRowId1 = finalResult.map { it.rowId1 }
            }

            val poemAdapter = PoemAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,
                searchViewModel.poemCount, searchViewModel.poemList)
            viewPager.apply {
                adapter = poemAdapter
                setCurrentItem(searchViewModel.poemPosition, false)
                setPageTransformer(PutAsideTransformer())
                registerOnPageChangeCallback(pageCallBack)
                offscreenPageLimit = 1
                post { pageCallBack.onPageSelected(currentItem) }
            }
        }

        // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View> ) {
                val currentFragment = childFragmentManager
                    .findFragmentByTag("f${searchViewModel.poemPosition}") as? PoemPagerFragment
                currentFragment?.view ?: return
//                currentFragment.poem_layout.transitionName = "Result_transition"
                try {
                    sharedElements[names[0]] = currentFragment.binding.poemLayout
                } catch (_: Exception) { }
            }
        })

        (activity as? MainActivity)?.analyticsLogEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, "Search result detail screen")
            }
        )
        Firebase.crashlytics.setCustomKey("Enter Screen", "Search result detail screen")

    }

    override fun onStop() {
        super.onStop()
        searchViewModel.apply {
            openedFromDetailFrag = false
        }
    }

    override fun onDestroyView() {
        viewPager.unregisterOnPageChangeCallback(pageCallBack)
        super.onDestroyView()
    }

    private fun motionInitialization(){
        postponeEnterTransition()
        val enterMatConTrans = MaterialContainerTransform()
        val returnMatConTrans = MaterialContainerTransform()

        enterMatConTrans.duration = 500
        enterMatConTrans.fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 0.55f)
        enterMatConTrans.endContainerColor = requireContext().getColorFromAttr(R.attr.colorSurface)
        enterMatConTrans.doOnEnd {
            visibleChildFrag?.resultItemMeasure()
            visibleChildFrag?.moveToSearchedPosition()
        }

        returnMatConTrans.duration = 500
        returnMatConTrans.fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.05f, 1f)
        returnMatConTrans.startContainerColor = requireContext().getColorFromAttr(R.attr.colorSurface)

        sharedElementEnterTransition = enterMatConTrans
        sharedElementReturnTransition = returnMatConTrans
    }

    fun setViewPagerItem(){
        val poemAdapter = PoemAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,
            searchViewModel.poemCount, searchViewModel.poemList)
        viewPager.adapter = poemAdapter
        viewPager.setCurrentItem(searchViewModel.poemPosition, false)
    }


}


class PutAsideTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            when {
                // [-Infinity,-1]
                position <= -1 -> {
                    translationZ = 20f
                }
                // (-1,1)
//                position == 0f -> translationX = 0f
                position < 0 -> translationZ = 20f
                position >= 0 && position < 1 -> {
                    x = 0f
                    translationZ = 0f
                }
                // [1,+Infinity]
                else -> {
                    translationX = 0f
                    translationZ = 0f
                }
            }
        }
    }
}