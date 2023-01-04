package com.takaapoo.adab_parsi.favorite

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.Hold
import com.google.android.material.transition.platform.MaterialFade
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.database.toRankedFavoriteContent
import com.takaapoo.adab_parsi.databinding.FragmentFavoriteBinding
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.BounceEdgeEffectFactory
import com.takaapoo.adab_parsi.util.Orientation
import com.takaapoo.adab_parsi.util.barsPreparation
import com.takaapoo.adab_parsi.util.fastScroll.ResultFastScrollViewHelper
import com.takaapoo.adab_parsi.util.topPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.search_result_item.view.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    val favoriteViewModel: FavoriteViewModel by activityViewModels()
    val settingViewModel: SettingViewModel by activityViewModels()

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private var scrollViewHelper: ResultFastScrollViewHelper? = null
    var layoutManager: LinearLayoutManager? = null
    var adapter: FavoriteAdapter? = null
    var currentNightMode = 0

    private val favoriteListScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            favoriteViewModel.scroll += dy
            recyclerView.post { favoriteListHeight(false) }
        }
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            recyclerView.post { favoriteListHeight(true) }
            if (!recyclerView.canScrollVertically(1)) {
                scrollViewHelper?.modifyScroll()
            } else if (!recyclerView.canScrollVertically(-1)) {
                scrollViewHelper?.scroll = 0
            }
        }
    }


//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        favoriteViewModel.favoriteListDisplace = 0
//        favoriteViewModel.scroll = 0
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        postponeEnterTransition()

        enterTransition = MaterialFadeThrough()
        returnTransition = MaterialFade()

        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        val navController = findNavController()
        binding.favoriteToolbar.setupWithNavController(navController, AppBarConfiguration
            .Builder(navController.graph).build())

        layoutManager = binding.favoriteList.layoutManager as LinearLayoutManager
        currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        adapter = FavoriteAdapter(this, currentNightMode == Configuration.UI_MODE_NIGHT_NO)
        binding.favoriteList.adapter = adapter
        binding.favoriteList.edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)
        favoriteViewModel.lastItem = 0
//        (binding.favoriteList.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false

        binding.root.doOnPreDraw {
            favoriteViewModel.apply {
                rootWidth = binding.root.measuredWidth
                mesraContainerWidth = (rootWidth - (startMargin + endMargin + verseSeparation)) / 2

                getAllFavorite().observe(viewLifecycleOwner) {
//                    favoriteHeight += favoriteListDisplace
//                    adapter.submitList(null)
                    binding.favoriteList.itemAnimator = if (favoriteListAddedScroll > 0) null
                    else DefaultItemAnimator()

                    adapter!!.submitList(it.mapIndexed { index, fContent ->
                        fContent.toRankedFavoriteContent(
                            index
                        )
                    })
                    allFavorites = it.toMutableList()

                    poemList = it.map { fvContent ->
                        Content(
                            fvContent.poemm.id!!,
                            fvContent.poemm.catID, fvContent.poemm.title, 1
                        )
                    } as MutableList<Content>
                    poemCount = it.size

                    val scroll = favoriteViewModel.scroll

                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        binding.favoriteList.postDelayed({
                            if (favoriteListAddedScroll != 0) {
                                favoriteViewModel.scroll = 0
                                scrollViewHelper?.scroll = 0
                                binding.favoriteList.scrollBy(0, scroll)
                            }
                            binding.favoriteList.scrollBy(0, favoriteListAddedScroll)
                            favoriteListDisplace = 0
                            favoriteListAddedScroll = 0
                        }, 100)

                    } else {
                        favoriteListDisplace -= bottomViewedResultHeight
                        favoriteViewModel.scroll = 0
                        scrollViewHelper?.scroll = 0
                        binding.favoriteList.scrollBy(
                            0,
                            favoriteListAddedScroll + favoriteListDisplace + scroll
                        )
                        favoriteListDisplace = 0
                        favoriteListAddedScroll = 0
                    }

                    startPostponedEnterTransition()
                }
            }
        }

        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: List<String>, sharedElements: MutableMap<String, View>) {

//                val destID = findNavController().currentDestination?.id
                val selectedViewHolder: RecyclerView.ViewHolder = binding.favoriteList
                    .findViewHolderForAdapterPosition(favoriteViewModel.poemPosition) ?: return
//                selectedViewHolder.itemView.card_view.transitionName = "Result_transition"
                try {
                    sharedElements[names[0]] = selectedViewHolder.itemView.card_view
                } catch (e: Exception) { }
            }
        })
        binding.favoriteToolbar.navigationContentDescription = resources.getString(R.string.navigation_up)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.favoriteToolbar.setPadding(0, topPadding, 0, 0 )
        favoriteListHeight(false)

        binding.favoriteList.addOnScrollListener(favoriteListScrollListener)

        scrollViewHelper = ResultFastScrollViewHelper(
            binding.favoriteList, null, ::getResultHeight)
        FastScrollerBuilder(binding.favoriteList).apply { disableScrollbarAutoHide() }
            .setPadding(0, 0, 0, 0)
            .setThumbDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_thumb, context?.theme)!!
            )
            .setTrackDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_track, context?.theme)!!
            )
            .setViewHelper(scrollViewHelper)
            .build()

        settingViewModel.refreshContent.observe(viewLifecycleOwner){
            if (it == true){
                binding.favoriteList.adapter = adapter
                scrollViewHelper?.apply {
                    scroll = 0
                    initialOffset = 0
                }
                settingViewModel.doneRefreshing()
            }
        }

        barsPreparation()

        (activity as? MainActivity)?.analyticsLogEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, "Favorite screen")
            }
        )
        Firebase.crashlytics.setCustomKey("Enter Screen", "Favorite screen")
    }

    override fun onDestroyView() {
        super.onDestroyView()

        layoutManager = null
        binding.favoriteList.removeOnScrollListener(favoriteListScrollListener)
        binding.favoriteList.adapter = null
        scrollViewHelper = null
        adapter = null

        _binding = null
    }


    fun setTransitionType(excView: View){
        exitTransition = Hold().setDuration(500).excludeTarget(excView, true)
        reenterTransition = MaterialFadeThrough().setDuration(500)
    }

    fun favoriteListHeight(anyWay: Boolean): Int{
        val newLastItem = layoutManager!!.findLastVisibleItemPosition()
        if (newLastItem != RecyclerView.NO_POSITION ){
            if (favoriteViewModel.lastItem < newLastItem || anyWay) {

                favoriteViewModel.lastItem = newLastItem
                favoriteViewModel.firstItem = layoutManager!!.findFirstVisibleItemPosition()

                val displayedItemsCount = favoriteViewModel.lastItem - favoriteViewModel.firstItem
                val rect = Rect()

                binding.favoriteList.getDecoratedBoundsWithMargins(
                    binding.favoriteList.getChildAt(displayedItemsCount), rect
                )

                favoriteViewModel.favoriteHeight = ((rect.bottom + favoriteViewModel.scroll) *
                        adapter!!.itemCount / (favoriteViewModel.lastItem + 1f)).toInt()
            }
        } else
            binding.favoriteList.post{
                if (layoutManager!!.itemCount > 0)
                    favoriteListHeight(false)
            }

        return favoriteViewModel.favoriteHeight
    }

    private fun getResultHeight() = favoriteViewModel.favoriteHeight.let {
        if (it != 0) it else favoriteListHeight(true)
    }


}