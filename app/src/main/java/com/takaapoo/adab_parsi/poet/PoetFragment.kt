package com.takaapoo.adab_parsi.poet

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentPoetBinding
import com.takaapoo.adab_parsi.home.HomeViewModel
import com.takaapoo.adab_parsi.util.Destinations
import com.takaapoo.adab_parsi.util.GlideApp
import com.takaapoo.adab_parsi.util.getColorFromAttr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.book_item.view.*
import kotlinx.android.synthetic.main.pager_poet.*
import kotlinx.android.synthetic.main.pager_poet.view.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class PoetFragment : FragmentWithTransformPage() {

    private var _binding: FragmentPoetBinding? = null
    val binding get() = _binding!!
    val homeViewModel: HomeViewModel by activityViewModels()
    val poetViewModel: PoetViewModel by activityViewModels()

    var currentChildFragment: PoetPagerFragment? = null

    private var pageCallBack = object : ViewPager2.OnPageChangeCallback(){
        override fun onPageSelected(position: Int) {
            homeViewModel.viewpagePosition = position

            currentChildFragment =
                childFragmentManager.findFragmentByTag("f${position}") as? PoetPagerFragment
            currentChildFragment?.apply {
                backCallback()
                firebaseLog()
            }

            setExitSharedElementCallback(object : SharedElementCallback() {
                override fun onMapSharedElements(names: List<String>,
                                                 sharedElements: MutableMap<String, View>) {
                    val destID = findNavController().currentDestination?.id
                    val currentFragment = childFragmentManager.findFragmentByTag("f$position")
                    currentFragment?.view ?: return

                    if (destID == R.id.bookFragment
                        || destID == R.id.poetFragment && poetViewModel.enterBookFragment){
                        val selectedViewHolder: RecyclerView.ViewHolder = currentFragment.poet_book_list
                            .findViewHolderForAdapterPosition(poetViewModel.bookPosition +
                                    poetViewModel.bookShelfSpanCount) ?: return

                        try {
                            sharedElements[names[0]] = selectedViewHolder.itemView.book_item_layout
                        } catch (_: Exception) { }
                    }
                    else
                        poetViewModel.enterBookFragment = false

                    if (destID == R.id.poetFragment && poetViewModel.enterBookFragment)
                        poetViewModel.bookPosition = 0
                }
            })
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        motionInitialization()
        _binding = FragmentPoetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        homeViewModel.enterPoetFragment = true

//        barsPreparation()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val poetBookAdapter = PoetBookAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,
            homeViewModel.ancient, homeViewModel.count)
        currentChildFragment =
            childFragmentManager.findFragmentByTag("f${homeViewModel.viewpagePosition}") as? PoetPagerFragment

        binding.viewPager.apply {
            adapter = poetBookAdapter
            (getChildAt(0) as RecyclerView).edgeEffectFactory =
                PoetBounceEdgeEffectFactory(this@PoetFragment)
//            val pos = homeViewModel.viewpagePosition
            setCurrentItem(homeViewModel.viewpagePosition, false)
            setPageTransformer(ZoomPageTransformer(this@PoetFragment))
            registerOnPageChangeCallback(pageCallBack)
            post {
//                homeViewModel.viewpagePosition = pos
                pageCallBack.onPageSelected(homeViewModel.viewpagePosition)
            }
        }

        // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View> ) {
//                val currentFragment =
//                    childFragmentManager.findFragmentByTag("f${homeViewModel.viewpagePosition}")
                currentChildFragment?.view ?: return
                try {
                    sharedElements[names[0]] = currentChildFragment!!.poet_layout
                } catch (_: Exception) { }
            }
        })

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val firstFragEntrance = preferenceManager.getBoolean("poetFragFirstEnter", true)
        if (firstFragEntrance) {
            poetViewModel.reportEvent(PoetEvent.OnShowHelp(PoetHelpState.PAGING))
            preferenceManager.edit().putBoolean("poetFragFirstEnter", false).apply()
        }

        val help = Help(this)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                poetViewModel.uiEvent.collect{ event ->
                    when(event){
                        is PoetEvent.OnBiographyClicked -> {
                            val bundle = Bundle().apply {
                                putInt("poet_id", event.poetID)
                            }
                            PoetBottomSheetFragment().run {
                                arguments = bundle
                                show(this@PoetFragment.childFragmentManager, "poet_bottom_sheet")
                            }
                        }
                        is PoetEvent.Navigate -> {
                            when(event.destination){
                                Destinations.SEARCH -> {
                                    exitTransition = MaterialFadeThrough().apply { duration = 500 }
                                    try {
                                        findNavController().navigate(
                                            directions = event.directions
                                        )
                                    } catch (_: Exception) { }
                                }
                                Destinations.BOOK -> {
                                    try {
                                        poetViewModel.poetLibContentShot = binding.viewPager.drawToBitmap()
                                        findNavController().navigate(
                                            directions = event.directions,
                                            navigatorExtras = event.extras!!
                                        )
                                    } catch (_: Exception) { }
                                }
                                else -> {}
                            }
                        }
                        is PoetEvent.OnShowHelp -> {
                            help.showHelp(event.state, if (firstFragEntrance) 2000 else 500)
                        }
                    }
                }
            }
        }

//        preferenceManager.edit().putBoolean("poetFragFirstEnter", true).apply()

    }

    override fun onPause() {
        super.onPause()
        if (activity?.isChangingConfigurations == false)
            poetViewModel.reportEvent(PoetEvent.OnShowHelp(PoetHelpState.NULL))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.unregisterOnPageChangeCallback(pageCallBack)
        binding.viewPager.adapter = null

        currentChildFragment = null
        _binding = null
        GlideApp.get(requireContext()).trimMemory(Glide.TRIM_MEMORY_COMPLETE)
    }

    private fun motionInitialization(){
        postponeEnterTransition()
        val enterMatConTrans = MaterialContainerTransform()
        val returnMatConTrans = MaterialContainerTransform()

        enterMatConTrans.apply {
            duration = 500
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 0.55f)
            endContainerColor = requireContext().getColorFromAttr(R.attr.colorSurface)
            scrimColor = Color.TRANSPARENT
        }

        returnMatConTrans.apply {
            duration = 500
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.05f, 1f)
            startContainerColor = requireContext().getColorFromAttr(R.attr.colorSurface)
            scrimColor = Color.TRANSPARENT
        }

        sharedElementEnterTransition = enterMatConTrans
        sharedElementReturnTransition = returnMatConTrans
    }

    override fun transformPage(view: View, position: Float){
        view.apply {
            // Move it behind the left page
            translationZ = -position
            translationX = 0f
            isVisible = true
            pivotX = 0f
            pivotY = view.height / 2f

            when {
                position == 0f && homeViewModel.poetFirstOpening -> {
                    x = 0f
                }
                position == 0f -> {
                    x = 0f
                    scaleView(1f, view)
                    scaleX = 1f
                    scaleY = 1f
                }
                position <= -1f -> { // [-Infinity,-1) This page is way off-screen to the right.
                    isVisible = false
                }
                position < 0f -> { // [-1,0)
                    when {
                        position > -0.3f -> {
                            val scaleFactor = MIN_SCALE2.coerceAtLeast(1 - abs(2 * position))
                            scaleView(scaleFactor, view)
                            scaleX = 1f
                            scaleY = 1f
                            x = if (position > -0.05f) 0f else 6 * (abs(position) - 0.05f) * view.width / 5
                        }
                        else -> {
                            if (position < -0.97f)
                                x = 2f * view.width
                            val scaleFactor = (2 * abs(position)).coerceAtLeast(1f)
                            scaleView(MIN_SCALE2, view)
                            scaleX = scaleFactor
                            scaleY = scaleFactor
                        }
                    }
                }
                position < 1f -> { // (0,1]
                    scaleX = 1f
                    scaleY = 1f
                    when {
                        position > 0.5f -> {
                            scaleView(MIN_SCALE, view)
                        }
                        else -> {
                            val scaleFactor = 1 - ((1 - MIN_SCALE) * position / 0.5f)
                            scaleView(scaleFactor, view)
                        }
                    }
                    x = 0f
                }
                else -> { // (1,+Infinity]  This page is way off-screen to the right.
                    x = width.toFloat()
                    isVisible = false
                }
            }
        }
    }

    private fun scaleView(scale: Float, view: View){
        val widget = view.widgets
        val poetCoordinate = view.poet_coordinate
        val wall = view.wall

        widget?.scaleX = DEFAULT_SCALE * scale
        widget?.scaleY = DEFAULT_SCALE * scale
        poetCoordinate?.scaleX = scale
        poetCoordinate?.scaleY = scale
        wall?.scaleX = scaleXWall * scale
        wall?.scaleY = DEFAULT_SCALE * scale
    }
}


const val ARG_POET_POSITION = "poet_position"
const val ARG_POET_ANCIENT = "poet_ancient"

class PoetBookAdapter(fragManager: FragmentManager, lifeCycle: Lifecycle, val ancient: Int,
                      private val itemNumber: Int) : FragmentStateAdapter(fragManager, lifeCycle){

    override fun getItemCount(): Int = itemNumber

    override fun createFragment(position: Int): Fragment {
        val fragment = PoetPagerFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_POET_POSITION, position)
            putInt(ARG_POET_ANCIENT, ancient)
        }
        return fragment
    }
}



const val DEFAULT_SCALE = 1.048f
const val scaleXWall = 1.035f
private const val MIN_SCALE = 0.5f
private const val MIN_SCALE2 = 0.9f

class ZoomPageTransformer(private val poetFragment: PoetFragment) : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        poetFragment.transformPage(view, position)
    }
}