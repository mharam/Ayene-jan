package com.takaapoo.adab_parsi.favorite

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
import com.takaapoo.adab_parsi.search_result.PutAsideTransformer
import com.takaapoo.adab_parsi.util.getColorFromAttr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoriteDetailFragment: BasePoemFragment() {

    private val favoriteViewModel: FavoriteViewModel by activityViewModels()

    private val pageCallBack = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            visibleChildFrag = childFragmentManager.findFragmentByTag("f${position}")
                    as? PoemPagerFragment
            favoriteViewModel.apply {
                poemPosition = position
                if (detailPagerPosition != position){
                    visibleChildFrag?.favoriteItemMeasure(allFavorites[poemPosition], false)
                    visibleChildFrag?.moveToFavoritePosition()
                    detailPagerPosition = position
                } else if (!comeFromFavoriteFragment){ // when coming from other fragments
                    visibleChildFrag?.moveToFavoritePosition()
                }
            }

            childFragmentManager.apply {
                (findFragmentByTag("f${position-1}") as? PoemPagerFragment)?.finishActionMode()
                (findFragmentByTag("f${position+1}") as? PoemPagerFragment)?.finishActionMode()
            }
        }
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            visibleChildFrag?.deselectText()
        }
    }
//    private var currentFragment: PoemPagerFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding.bookImage.visibility = View.GONE
        favoriteViewModel.apply {
//            comeFromDetailFragment = true
            openedFromFavoriteDetailFrag = true
//            favoriteListDisplace = 0
//            favoriteListAddedScroll = 0
//            bottomViewedResultHeight = 0
//            topViewedResultHeight = 0
//            detailPagerPosition = -1
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            if (favoriteViewModel.allFavorites.isEmpty())
                favoriteViewModel.getAllFavoriteSuspend()

            motionInitialization()

            val poemAdapter = PoemAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,
                favoriteViewModel.poemCount, favoriteViewModel.poemList)

            viewPager.apply {
                adapter = poemAdapter
                setCurrentItem(favoriteViewModel.poemPosition, false)
                setPageTransformer(PutAsideTransformer())
                registerOnPageChangeCallback(pageCallBack)
                offscreenPageLimit = 1
                post { pageCallBack.onPageSelected(currentItem) }
            }

            // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
            setEnterSharedElementCallback(object : SharedElementCallback() {
                override fun onMapSharedElements(names: List<String>, sharedElements: MutableMap<String, View> ) {
//                val currentFragment =
//                    childFragmentManager.findFragmentByTag("f${favoriteViewModel.poemPosition}")
//                currentFragment?.view ?: return
                    favoriteViewModel.apply {
                        binding.bookFrame.transitionName =
                            "TN${allFavorites[poemPosition].poemm.id},${allFavorites[poemPosition].verse1Order}"
                    }
                    try {
                        sharedElements[names[0]] = binding.bookFrame
                    } catch (_: Exception) { }
                }
            })
        }


        (activity as? MainActivity)?.analyticsLogEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, "Favorite Detail screen")
            }
        )
        Firebase.crashlytics.setCustomKey("Enter Screen", "Favorite Detail screen")

    }

    override fun onStop() {
        super.onStop()
        favoriteViewModel.comeFromFavoriteFragment = false

        favoriteViewModel.apply {
            openedFromFavoriteDetailFrag = false
        }
    }

    override fun onDestroyView() {
        viewPager.unregisterOnPageChangeCallback(pageCallBack)
        super.onDestroyView()
    }


    private fun motionInitialization() {
        postponeEnterTransition()
        val enterMatConTrans = MaterialContainerTransform()
        val returnMatConTrans = MaterialContainerTransform()

        enterMatConTrans.duration = 500
        enterMatConTrans.fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 0.55f)
        enterMatConTrans.endContainerColor = requireContext().getColorFromAttr(R.attr.colorSurface)
        enterMatConTrans.doOnEnd {
            visibleChildFrag?.favoriteItemMeasure(
                favoriteViewModel.allFavorites[favoriteViewModel.poemPosition], false)
            visibleChildFrag?.moveToFavoritePosition()
        }

        returnMatConTrans.duration = 500
        returnMatConTrans.fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.05f, 1f)
        returnMatConTrans.startContainerColor = requireContext().getColorFromAttr(R.attr.colorSurface)

        sharedElementEnterTransition = enterMatConTrans
        sharedElementReturnTransition = returnMatConTrans
    }

    fun setViewPagerItem(){
        val poemAdapter = PoemAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,
            favoriteViewModel.poemCount, favoriteViewModel.poemList)
        viewPager.apply {
            adapter = poemAdapter
            setCurrentItem(favoriteViewModel.poemPosition, false)
            post { pageCallBack.onPageSelected(currentItem) }
        }
    }

}

