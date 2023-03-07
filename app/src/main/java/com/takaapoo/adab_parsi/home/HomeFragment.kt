package com.takaapoo.adab_parsi.home

import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateOvershootInterpolator
import androidx.activity.addCallback
import androidx.core.animation.doOnEnd
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.Hold
import com.google.android.material.transition.platform.MaterialElevationScale
import com.google.android.material.transition.platform.MaterialFade
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.add.ARG_ADD_PAGE
import com.takaapoo.adab_parsi.add.AddViewModel
import com.takaapoo.adab_parsi.databinding.FragmentHomeBinding
import com.takaapoo.adab_parsi.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    val homeViewModel: HomeViewModel by activityViewModels()
    private val addViewModel: AddViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    val binding get() = _binding!!
//    var currentChildFragment: HomePagerFragment? = null
    private var tabMediator: TabLayoutMediator? = null
    private var help: Help? = null

//    private val viewPagerPosition = ViewPagerPosition(lifecycleScope)
    lateinit var preferenceManager: SharedPreferences

//    private val viewPagerCallback = object : ViewPager2.OnPageChangeCallback(){
//        override fun onPageSelected(position: Int) {
//            homeViewModel.homePagerPosition = position
//
//            setExitSharedElementCallback( object : SharedElementCallback() {
//                override fun onMapSharedElements(names: List<String>,
//                                                 sharedElements: MutableMap<String, View>
//                ) {
//                    val destID = findNavController().currentDestination?.id
//                    if (destID == R.id.poetFragment
//                        || (destID == R.id.homeFragment && homeViewModel.enterPoetFragment)){
//                        val currentFragment = childFragmentManager.findFragmentByTag("f$position")
//                        currentFragment?.view ?: return
//                        val selectedViewHolder: RecyclerView.ViewHolder = currentFragment.loaded_poet_list
//                            .findViewHolderForAdapterPosition(homeViewModel.viewpagePosition) ?: return
//                        try {
//                            sharedElements[names[0]] = selectedViewHolder.itemView.card_view
//                        } catch (e: Exception) { }
//
//                    } else
//                        homeViewModel.enterPoetFragment = false
//                }
//            })
//
//            childFragmentManager.apply {
//                (findFragmentByTag("f0") as? HomePagerFragment)?.finishActionMode()
//                (findFragmentByTag("f1") as? HomePagerFragment)?.finishActionMode()
//            }
//            currentChildFragment = childFragmentManager
//                .findFragmentByTag("f${position}") as? HomePagerFragment
//        }
//    }


    private val animatedTrashCallback = object : Animatable2Compat.AnimationCallback(){
        override fun onAnimationEnd(drawable: Drawable?) {
            Handler(Looper.getMainLooper()).post{
                (binding.trash.drawable as Animatable).start()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        postponeEnterTransition()
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = homeViewModel

        exitTransition = MaterialFade()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            topPadding = insets.top
//            poemViewModel.topPadding = windowInsets.systemWindowInsetTop
            binding.toolbar.setPadding(0, insets.top, 0, 0)
            (binding.cardView.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
                4.dpTOpx(resources).toInt() + insets.top

            windowInsets
        }

        preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())


//        preferenceManager.edit().putBoolean("homeFragFirstEnter", true).apply()


        tabMediator = TabLayoutMediator(binding.tab, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "کهن"
                else -> "معاصر"
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!binding.interactionBlocker.isVisible){
                if (findNavController().currentDestination?.id == R.id.homeFragment)
                    activity?.finish()
                else
                    findNavController().popBackStack()
            }
        }

        homeViewModel.poetFirstOpening = true
//        barsPreparation()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewPager.apply {
            adapter = HomePoetAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
            (getChildAt(0) as RecyclerView).edgeEffectFactory =
                BounceEdgeEffectFactory(Orientation.HORIZONTAL)
//            registerOnPageChangeCallback(viewPagerCallback)
        }
//        currentChildFragment = childFragmentManager
//            .findFragmentByTag("f${homeViewModel.homePagerPosition}") as? HomePagerFragment

        tabMediator?.attach()

        binding.root.doOnPreDraw {
            binding.title.updateLayoutParams {
                width = binding.cardView.width - binding.hamburger.width - binding.title.marginEnd
            }
        }
        handlePaging()
        binding.deleteDialog.translationY = 400f
        help = Help(this)
        var firstFragEntrance = preferenceManager.getBoolean("homeFragFirstEnter", true)
        if (firstFragEntrance) {
//            homeViewModel.doShowHelp()
            homeViewModel.reportEvent(HomeEvent.OnShowHelp(HelpView.ADD_FAB))
            preferenceManager.edit().putBoolean("homeFragFirstEnter", false).apply()
        }

//        homeViewModel.showHelp.observe(viewLifecycleOwner) {
//            help.showHelp(it, if (firstFragEntrance) 2500 else 500)
//            if (it != null) firstFragEntrance = false
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                homeViewModel.uiEvent.collect{ event ->
                    when(event){
                        is HomeEvent.Navigate -> {
                            try {
                                val navController = findNavController()
                                when(event.destination){
                                    Destinations.ADD_POET -> {
                                        exitTransition = Hold().setDuration(500)
                                        reenterTransition = null
                                        val extras = FragmentNavigatorExtras(
                                            binding.addPoetFab to "container_transform_FAB"
                                        )
                                        val direction =
                                            HomeFragmentDirections.actionHomeFragmentToAddFragment()
                                        navController.navigate(direction, extras)
                                    }
                                    Destinations.SEARCH -> {
                                        exitTransition = Hold().setDuration(500)
                                        reenterTransition = null
                                        val extras = FragmentNavigatorExtras(
                                            binding.cardView to "container_transform_SEARCH")
                                        val direction =
                                            HomeFragmentDirections.actionGlobalSearchFragment(-1, -1)
                                        navController.navigate(direction, extras)
                                    }
                                    Destinations.POET -> {
                                        exitTransition = MaterialElevationScale(false)
                                        reenterTransition = MaterialElevationScale(true)
                                        val direction =
                                            HomeFragmentDirections.actionHomeFragmentToPoetFragment()
                                        homeViewModel.navigatorExtra?.let {
                                            navController.navigate(direction, it)
                                        }
                                    }
                                    else -> {}
                                }
                            } catch (_: Exception) {}
                        }
                        is HomeEvent.OpenDrawer -> {
                            (activity as? MainActivity)?.moveDrawer()
                        }
                        is HomeEvent.OnDeleteDialogPosClick -> {
                            val mainActivity = activity as? MainActivity
                            binding.interactionBlocker.isVisible = true

                            AnimatedVectorDrawableCompat.registerAnimationCallback(
                                binding.trash.drawable,
                                animatedTrashCallback
                            )
                            (binding.trash.drawable as Animatable).start()
                            binding.deleteText.text = getString(
                                R.string.delete_poet_snack,
                                engNumToFarsiNum(event.poetList.size())
                            )
                            binding.deleteDialog
                                .animate()
                                .setDuration(500)
                                .translationY(0f)
                                .setInterpolator(AnticipateOvershootInterpolator())
                                .start()

                            homeViewModel.deleteDatabase(event.poetList.toList()).invokeOnCompletion {
                                binding.deleteDialog
                                    .animate()
                                    .setDuration(400)
                                    .translationY(400f)
                                    .setInterpolator(AnticipateOvershootInterpolator())
                                    .withEndAction {
                                        AnimatedVectorDrawableCompat.unregisterAnimationCallback(
                                            binding.trash.drawable, animatedTrashCallback
                                        )
                                        binding.interactionBlocker.isVisible = false
                                        if (mainActivity?.binding?.drawerLayout?.tag == "land_scape") {
                                            mainActivity.removeFragmentFromContainer()
                                        }
                                    }
                                    .start()
                                addViewModel.determineAllPoet()
                            }
                        }
                        is HomeEvent.OnDeleteDialogDismiss -> {
//                            currentChildFragment?.finishActionMode()
                            homeViewModel.setActionModeState(ActionModeState.GONE)
                        }
                        is HomeEvent.OnCreateActionMode -> {
                            ValueAnimator.ofArgb(
                                requireContext().getColorFromAttr(R.attr.colorSurface),
                                requireContext().getColorFromAttr(R.attr.colorBeitSelect)
                            ).apply {
                                addUpdateListener { updatedAnimation ->
                                    requireActivity().window.statusBarColor = updatedAnimation.animatedValue as Int
                                }
                            }.start()
                            homeViewModel.setActionModeState(ActionModeState.VISIBLE)
                        }
                        is HomeEvent.OnDeleteClick -> {
//                            homeViewModel.selectedPoetCount = event.poetList.size()
//                            homeViewModel.deleteDialogTitle = getString(
//                                R.string.delete_poet_title,
//                                engNumToFarsiNum(event.poetList.size())
//                            )
                            DeletePoetDialogFragment(event.poetList).show(
                                childFragmentManager,
                                "Delete"
                            )
                        }
                        is HomeEvent.OnDestroyActionMode -> {
                            ValueAnimator.ofArgb(
                                requireContext().getColorFromAttr(R.attr.colorBeitSelect),
                                requireContext().getColorFromAttr(R.attr.colorSurface)
                            ).apply {
                                addUpdateListener { updatedAnimation ->
                                    activity?.window?.statusBarColor = updatedAnimation.animatedValue as Int
                                }
                                doOnEnd { activity?.window?.statusBarColor = Color.TRANSPARENT }
                            }.start()
                        }
                        is HomeEvent.OnShowHelp -> {
                            help?.showHelp(event.helpView, if (firstFragEntrance) 2200 else 500)
                            firstFragEntrance = false
                        }
                    }
                }
            }
        }

        Firebase.analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, "Home screen")
            }
        )
        Firebase.crashlytics.setCustomKey("Enter Screen", "Home screen")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AnimatedVectorDrawableCompat.unregisterAnimationCallback(
            binding.trash.drawable, animatedTrashCallback
        )
//        binding.viewPager.unregisterOnPageChangeCallback(viewPagerCallback)
        binding.viewPager.adapter = null
        tabMediator?.detach()
        tabMediator = null
        help = null

//        binding.apply {
//            addPoetFab.setOnClickListener(null)
//            hamburger.setOnClickListener(null)
//            cardView.setOnClickListener(null)
//        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, null)
//        currentChildFragment = null
        homeViewModel.navigatorExtra = null

        _binding = null
        GlideApp.get(requireContext()).trimMemory(Glide.TRIM_MEMORY_COMPLETE)
    }

    private fun handlePaging(){
        ViewPagerPosition(lifecycleScope, binding.viewPager, 0)
            .pagePositionFlow
            .onEach { position ->
                homeViewModel.homePagerPosition = position

                setExitSharedElementCallback(
                    object : SharedElementCallback() {
                        override fun onMapSharedElements(
                            names: List<String>,
                            sharedElements: MutableMap<String, View>
                        ) {
                            val destID = findNavController().currentDestination?.id
                            if (destID == R.id.poetFragment
                                || (destID == R.id.homeFragment && homeViewModel.enterPoetFragment)) {
                                val currentFragment =
                                    childFragmentManager.findFragmentByTag("f$position") as? HomePagerFragment
                                currentFragment?.view ?: return
                                val selectedViewHolder: RecyclerView.ViewHolder =
                                    currentFragment.binding.loadedPoetList
                                    .findViewHolderForAdapterPosition(homeViewModel.viewpagePosition) ?: return
                                try {
                                    sharedElements[names[0]] = selectedViewHolder.itemView.findViewById(R.id.card_view)
                                } catch (_: Exception) { }

                            } else
                                homeViewModel.enterPoetFragment = false
                        }
                    }
                )
                childFragmentManager.apply {
                    (findFragmentByTag("f0") as? HomePagerFragment)?.finishActionMode()
                    (findFragmentByTag("f1") as? HomePagerFragment)?.finishActionMode()
                }
            }
            .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

}

class HomePoetAdapter(
    fragManager: FragmentManager,
    lifeCycle: Lifecycle
) : FragmentStateAdapter(fragManager, lifeCycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = HomePagerFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_ADD_PAGE, position)
        }
        return fragment
    }
}

