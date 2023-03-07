package com.takaapoo.adab_parsi.bookmark

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
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
import com.takaapoo.adab_parsi.poem.TurnPageTransformer
import com.takaapoo.adab_parsi.util.getColorFromAttr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookmarkDetailFragment: BasePoemFragment() {

    val bookmarkViewModel: BookmarkViewModel by activityViewModels()

    private val systemUiRunnable = Runnable { (activity as MainActivity).hideSystemBars() }
    private lateinit var handler: Handler
//    private var onPauseCalled = false

    private val pageCallBack = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            bookmarkViewModel.poemPosition = position

            visibleChildFrag = childFragmentManager.findFragmentByTag("f${position}")
                    as? PoemPagerFragment
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
        }
        override fun onPageScrolled(position: Int, positionOffset: Float,
                                    positionOffsetPixels: Int) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            visibleChildFrag?.deselectText()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding.bookImage.visibility = View.GONE
        handler = Handler(Looper.getMainLooper())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        motionInitialization()

        viewLifecycleOwner.lifecycleScope.launch {
            bookmarkViewModel.let {
                if (it.poemList.isEmpty())
                    it.getPoemWithCatID(it.selectedItemCatID)
            }

            val poemAdapter = PoemAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,
                bookmarkViewModel.poemCount, bookmarkViewModel.poemList)

            viewPager.apply {
                adapter = poemAdapter
                setCurrentItem(bookmarkViewModel.poemPosition, false)
                setPageTransformer(TurnPageTransformer())
                registerOnPageChangeCallback(pageCallBack)
                offscreenPageLimit = 1
                post { pageCallBack.onPageSelected(currentItem) }
            }

            // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
            setEnterSharedElementCallback(object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>, sharedElements: MutableMap<String, View> ) {
                    val currentFragment = childFragmentManager
                        .findFragmentByTag("f${bookmarkViewModel.poemPosition}") as? PoemPagerFragment
                    currentFragment?.view ?: return
//                currentFragment.poem_layout.transitionName = "TM${bookmarkViewModel.poemPosition}"
                    try {
                        sharedElements[names[0]] = currentFragment.binding.poemLayout
                    } catch (_: Exception) { }

                }
            })
        }

        (activity as? MainActivity)?.analyticsLogEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, "Bookmark Detail screen")
            }
        )
        Firebase.crashlytics.setCustomKey("Enter Screen", "Bookmark Detail screen")

    }

    override fun onResume() {
        super.onResume()
//        onPauseCalled = false
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            handler.postDelayed( systemUiRunnable , 3500)
    }

    override fun onPause() {
//        onPauseCalled = true
        handler.removeCallbacks(systemUiRunnable)
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            (activity as MainActivity).showSystemBars()

        super.onPause()
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

        returnMatConTrans.duration = 500
        returnMatConTrans.fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.05f, 1f)
        returnMatConTrans.startContainerColor = requireContext().getColorFromAttr(R.attr.colorSurface)

        sharedElementEnterTransition = enterMatConTrans
        sharedElementReturnTransition = returnMatConTrans
    }

    fun setViewPagerItem(){
        viewPager.adapter = PoemAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,
            bookmarkViewModel.poemCount, bookmarkViewModel.poemList)
        viewPager.setCurrentItem(bookmarkViewModel.poemPosition, false)
    }

}



