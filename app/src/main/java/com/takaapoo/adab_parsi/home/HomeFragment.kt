package com.takaapoo.adab_parsi.home

import android.content.SharedPreferences
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
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.Hold
import com.google.android.material.transition.platform.MaterialElevationScale
import com.google.android.material.transition.platform.MaterialFade
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.add.ARG_ADD_PAGE
import com.takaapoo.adab_parsi.add.AddViewModel
import com.takaapoo.adab_parsi.databinding.FragmentHomeBinding
import com.takaapoo.adab_parsi.util.*
import com.takaapoo.adab_parsi.util.eventHandler.EventObserver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.pager_home.*
import kotlinx.android.synthetic.main.poet_item_recent.view.*
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {

    val homeViewModel: HomeViewModel by activityViewModels()
//    private val poemViewModel: PoemViewModel by activityViewModels()
    private val addViewModel: AddViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    val binding get() = _binding!!
    var currentChildFragment: HomePagerFragment? = null
    private var tabMediator: TabLayoutMediator? = null

    lateinit var preferenceManager: SharedPreferences

    private val viewPagerCallback = object : ViewPager2.OnPageChangeCallback(){
        override fun onPageSelected(position: Int) {
            homeViewModel.homePagerPosition = position

            setExitSharedElementCallback( object : SharedElementCallback() {
                override fun onMapSharedElements(names: List<String>,
                                                 sharedElements: MutableMap<String, View>) {
                    val destID = findNavController().currentDestination?.id
                    if (destID == R.id.poetFragment
                        || (destID == R.id.homeFragment && homeViewModel.enterPoetFragment)){
                        val currentFragment = childFragmentManager.findFragmentByTag("f$position")
                        currentFragment?.view ?: return
                        val selectedViewHolder: RecyclerView.ViewHolder = currentFragment.loaded_poet_list
                            .findViewHolderForAdapterPosition(homeViewModel.viewpagePosition) ?: return
                        try {
                            sharedElements[names[0]] = selectedViewHolder.itemView.card_view
                        } catch (e: Exception) { }

                    } else
                        homeViewModel.enterPoetFragment = false
                }
            })

            childFragmentManager.apply {
                (findFragmentByTag("f0") as? HomePagerFragment)?.finishActionMode()
                (findFragmentByTag("f1") as? HomePagerFragment)?.finishActionMode()
            }
            currentChildFragment = childFragmentManager
                .findFragmentByTag("f${position}") as? HomePagerFragment
        }
    }

//    private var help: Help? = null

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

//        exitTransition = Hold().setDuration(500)

        exitTransition = MaterialFade()
        binding.fab.setOnClickListener {
            exitTransition = Hold().setDuration(500)
            reenterTransition = null
            try {
                val extras = FragmentNavigatorExtras(binding.fab to "container_transform_FAB")
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToAddFragment(), extras)
            } catch (e: Exception) { }
        }

        binding.hamburger.setOnClickListener {
            (activity as? MainActivity)?.moveDrawer()
        }

        binding.cardView.setOnClickListener {
            exitTransition = Hold().setDuration(500)
            reenterTransition = null
            try {
                val extras = FragmentNavigatorExtras(binding.cardView to "container_transform_SEARCH")
                findNavController().navigate(
                    HomeFragmentDirections.actionGlobalSearchFragment(-1, -1), extras)
            } catch (e: Exception) { }
        }

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
        var firstFragEnterance = preferenceManager.getBoolean("homeFragFirstEnter", true)
        if (firstFragEnterance) {
            homeViewModel.doShowHelp()
            preferenceManager.edit().putBoolean("homeFragFirstEnter", false).apply()
        }

        val help = Help(this)
        homeViewModel.showHelp.observe(viewLifecycleOwner) {
            help.showHelp(it, if (firstFragEnterance) 2500 else 500)
            if (it != null) firstFragEnterance = false
        }

//        preferenceManager.edit().putBoolean("homeFragFirstEnter", true).apply()


        tabMediator = TabLayoutMediator(binding.tab, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "کهن"
                1 -> tab.text = "معاصر"
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
        barsPreparation()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewPager.apply {
            adapter = HomePoetAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
            (getChildAt(0) as RecyclerView).edgeEffectFactory =
                BounceEdgeEffectFactory(Orientation.HORIZONTAL)
            registerOnPageChangeCallback(viewPagerCallback)
        }
        currentChildFragment = childFragmentManager
            .findFragmentByTag("f${homeViewModel.homePagerPosition}") as? HomePagerFragment

        tabMediator?.attach()

        binding.root.doOnPreDraw {
            binding.title.updateLayoutParams {
                width = binding.cardView.width - binding.hamburger.width - binding.title.marginEnd
            }
        }

//        homeViewModel.verseCount()
        binding.deleteDialog.translationY = 400f
        homeViewModel.deletePoetDialogPosClick.observe(viewLifecycleOwner, EventObserver {
            val mainActivity = activity as? MainActivity
            binding.interactionBlocker.isVisible = true
            val selectedList = currentChildFragment?.getSelectedPoetList()?.toMutableList()
//            preferenceManager.edit().putString("toBeDeletedPoets", selectedList?.joinToString(","))
//                .apply()

            AnimatedVectorDrawableCompat.registerAnimationCallback(
                binding.trash.drawable, animatedTrashCallback
            )
            (binding.trash.drawable as Animatable).start()
            binding.deleteText.text = getString(R.string.delete_poet_snack,
                engNumToFarsiNum(selectedList?.size ?: 0))
            binding.deleteDialog.animate().setDuration(500).translationY(0f).setInterpolator(
                AnticipateOvershootInterpolator()
            ).start()

            homeViewModel.deleteDatabase(selectedList).invokeOnCompletion {
                binding.deleteDialog.animate().setDuration(400).translationY(400f)
                    .setInterpolator(AnticipateOvershootInterpolator()).withEndAction {
                        AnimatedVectorDrawableCompat.unregisterAnimationCallback(
                            binding.trash.drawable, animatedTrashCallback
                        )
                        binding.interactionBlocker.isVisible = false
                        if (mainActivity?.binding?.drawerLayout?.tag == "land_scape") {
                            mainActivity.removeFragmentFromContainer()
                        }
                    }.start()
//                preferenceManager.edit().putString("toBeDeletedPoets", "").apply()
                addViewModel.determineAllPoet()
            }
        })

        homeViewModel.deletePoetDialogNegClick.observe(viewLifecycleOwner, EventObserver{
            currentChildFragment?.finishActionMode()
        })

        homeViewModel.navigateToPoet.observe(viewLifecycleOwner, EventObserver {
            exitTransition = MaterialElevationScale(false)
            reenterTransition = MaterialElevationScale(true)
            try {
                val action = HomeFragmentDirections.actionHomeFragmentToPoetFragment()
                homeViewModel.navigatorExtra?.let { findNavController().navigate(action, it) }
            } catch (e: Exception) { }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        AnimatedVectorDrawableCompat.unregisterAnimationCallback(
            binding.trash.drawable, animatedTrashCallback
        )
        binding.viewPager.unregisterOnPageChangeCallback(viewPagerCallback)
        binding.viewPager.adapter = null
        tabMediator?.detach()
        tabMediator = null

        binding.apply {
            fab.setOnClickListener(null)
            hamburger.setOnClickListener(null)
            cardView.setOnClickListener(null)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, null)
        currentChildFragment = null
        homeViewModel.navigatorExtra = null

        _binding = null
        GlideApp.get(requireContext()).trimMemory(Glide.TRIM_MEMORY_COMPLETE)
    }


}

class HomePoetAdapter(fragManager: FragmentManager, lifeCycle: Lifecycle)
    : FragmentStateAdapter(fragManager, lifeCycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = HomePagerFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_ADD_PAGE, position)
        }
        return fragment
    }
}

