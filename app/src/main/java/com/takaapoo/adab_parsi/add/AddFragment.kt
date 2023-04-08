package com.takaapoo.adab_parsi.add

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentAddBinding
import com.takaapoo.adab_parsi.home.HomeViewModel
import com.takaapoo.adab_parsi.network.PoetProperty
import com.takaapoo.adab_parsi.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class AddFragment : Fragment() {

    private val addViewModel: AddViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private var viewPager: ViewPager2? = null
    private var tabMediator: TabLayoutMediator? = null

    private lateinit var viewPagerCallback : ViewPager2.OnPageChangeCallback


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        motionInitialization()

        _binding = FragmentAddBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = addViewModel

        val navController = findNavController()
        binding.toolbar.setupWithNavController(navController,
            AppBarConfiguration.Builder(navController.graph).build())

        viewPagerCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                homeViewModel.homePagerPosition = position
            }
        }

        navController.getBackStackEntry(navController.currentDestination!!.id)
        binding.toolbar.navigationContentDescription = resources.getString(R.string.navigation_up)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topBar.setPadding(0, topPadding, 0, 0)

        viewPager = binding.viewPager
        viewPager!!.adapter = AddPoetAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager!!.setCurrentItem(homeViewModel.homePagerPosition, false)
        (viewPager!!.getChildAt(0) as RecyclerView).edgeEffectFactory =
            BounceEdgeEffectFactory(Orientation.HORIZONTAL)
        viewPager!!.registerOnPageChangeCallback(viewPagerCallback)
        initializeTab(emptyList())

//        addViewModel.allPoet.observe(viewLifecycleOwner) { list -> initializeTab(list) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                launch {
                    addViewModel.notInstalledPoet.collect { poetPropertyList ->
                        initializeTab(poetPropertyList)
                    }
                }
                launch {
                    addViewModel.uiEvent.collect { event ->
                        when (event) {
                            is AddEvent.ShowSnack -> {
                                Snackbar.make(binding.myLayout, event.mess, Snackbar.LENGTH_LONG)
                                    .show()
                            }
                            is AddEvent.PoetTouched -> {
                                ObjectAnimator.ofInt(
                                    event.poetView,
                                    "height",
                                    if (event.poetView.height == 0) event.height else 0
                                ).apply {
                                    interpolator = AccelerateInterpolator(1f)
                                }.start()
                            }
                            is AddEvent.DownloadPoet -> {
                                addViewModel.downloadPoet(event.poetItem)
                            }
                        }
                    }
                }
            }
        }

        (activity as? MainActivity)?.analyticsLogEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, "Add poet screen")
            }
        )
        Firebase.crashlytics.setCustomKey("Enter Screen", "Add poet screen")
    }

    private fun initializeTab(list: List<PoetProperty>?){
        tabMediator = TabLayoutMediator(binding.tab, viewPager!!) { tab, position ->
            val badge = tab.orCreateBadge
            when (position){
                0 -> {
                    tab.text = "کهن"
                    badge.isVisible = false
                    badge.number = list?.filter { it.ancient == 0 && it.parentID == 0}?.size ?: 0
                    badge.isVisible = badge.number != 0
                }
                1 -> {
                    tab.text = "معاصر"
                    badge.isVisible = false
                    badge.number = list?.filter { it.ancient == 1 && it.parentID == 0}?.size ?: 0
                    badge.isVisible = badge.number != 0
                }
            }
        }
        tabMediator!!.attach()
    }

    private fun motionInitialization(){
        val enterMatConTrans = MaterialContainerTransform()
        val returnMatConTrans = MaterialContainerTransform()

        enterMatConTrans.apply {
            duration = 500
            pathMotion = MaterialArcMotion()
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 0.95f)
//        enterMatConTrans.scrimColor = Color.TRANSPARENT
            endContainerColor = requireContext().getColorFromAttr(R.attr.colorSurface)
            startContainerColor = requireContext().getColorFromAttr(R.attr.colorSecondary)
        }

        returnMatConTrans.apply {
            duration = 500
            pathMotion = MaterialArcMotion()
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.05f, 1f)
//        returnMatConTrans.scrimColor = Color.TRANSPARENT
            endContainerColor = requireContext().getColorFromAttr(R.attr.colorSecondary)
            startContainerColor = requireContext().getColorFromAttr(R.attr.colorSurface)
        }

        sharedElementEnterTransition = enterMatConTrans
        sharedElementReturnTransition = returnMatConTrans
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager!!.unregisterOnPageChangeCallback(viewPagerCallback)
        viewPager!!.adapter = null
        tabMediator?.detach()
        tabMediator = null

        viewPager = null
        _binding = null

        Runtime.getRuntime().gc()
    }

    override fun onDestroy() {
        super.onDestroy()
        GlideApp.get(requireContext()).trimMemory(Glide.TRIM_MEMORY_COMPLETE)
    }


}

const val ARG_ADD_PAGE = "RecentOrNo"
class AddPoetAdapter(fragManager: FragmentManager, lifeCycle: Lifecycle)
    : FragmentStateAdapter(fragManager, lifeCycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = AddPageFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_ADD_PAGE, position)
        }
        return fragment
    }
}
