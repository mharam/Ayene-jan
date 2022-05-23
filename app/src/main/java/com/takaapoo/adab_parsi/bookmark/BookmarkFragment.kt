package com.takaapoo.adab_parsi.bookmark

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.Hold
import com.google.android.material.transition.platform.MaterialFade
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentBookmarkBinding
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.BounceEdgeEffectFactory
import com.takaapoo.adab_parsi.util.Orientation
import com.takaapoo.adab_parsi.util.barsPreparation
import com.takaapoo.adab_parsi.util.fastScroll.ResultFastScrollViewHelper
import com.takaapoo.adab_parsi.util.topPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.zhanghai.android.fastscroll.FastScrollerBuilder


@AndroidEntryPoint
class BookmarkFragment: Fragment() {

    val bookmarkViewModel: BookmarkViewModel by activityViewModels()
    val settingViewModel: SettingViewModel by activityViewModels()

    private var _binding: FragmentBookmarkBinding? = null
    private val binding get() = _binding!!

    private var scrollViewHelper: ResultFastScrollViewHelper? = null
    var layoutManager: LinearLayoutManager? = null
    var adapter: BookmarkAdapter? = null
    var currentNightMode = 0

    private val holderJob = Job()
    val holderScope = CoroutineScope(Dispatchers.Main + holderJob)

    private val bookListScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            bookmarkViewModel.scroll += dy
            recyclerView.post { bookmarkListHeight(false) }
        }
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            recyclerView.post { bookmarkListHeight(true) }
            if (!recyclerView.canScrollVertically(1)) {
                scrollViewHelper?.modifyScroll()
            }
            if (!recyclerView.canScrollVertically(-1)) {
                scrollViewHelper?.scroll = 0
            }
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        bookmarkViewModel.bookmarkListDisplace = 0
        bookmarkViewModel.scroll = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        postponeEnterTransition()

        enterTransition = MaterialFadeThrough()
        returnTransition = MaterialFade()

        _binding = FragmentBookmarkBinding.inflate(inflater, container, false)
        val navController = findNavController()
//        val appBarConfiguration = (requireActivity() as MainActivity).appBarConfiguration
//        binding.bookmarkToolbar.setupWithNavController(navController, appBarConfiguration)
        binding.bookmarkToolbar.setupWithNavController(navController,
            AppBarConfiguration.Builder(navController.graph).build())

        layoutManager = binding.bookmarkList.layoutManager as LinearLayoutManager
        currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        adapter = BookmarkAdapter(this, currentNightMode == Configuration.UI_MODE_NIGHT_NO)
        binding.bookmarkList.adapter = adapter
        binding.bookmarkList.edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)
        bookmarkViewModel.lastItem = 0
//        binding.bookmarkList.itemAnimator = null

        binding.root.doOnPreDraw {
            bookmarkViewModel.apply {
                rootWidth = binding.root.measuredWidth
                mesraContainerWidth = (rootWidth - (startMargin + endMargin + verseSeparation)) / 2

                getAllBookmark().observe(viewLifecycleOwner) {
//                    bookmarkViewModel.lastItem = 0
//                    bookmarkHeight += bookmarkListDisplace
                    adapter!!.submitList(null)
                    adapter!!.submitList(it)
                    allBookmarks = it.toMutableList()
//                    bookmarkListHeight(true)

//                    binding.bookmarkList.post {
                    val scroll = bookmarkViewModel.scroll
                    bookmarkViewModel.scroll = 0
                    scrollViewHelper?.scroll = 0
                    if ((activity as MainActivity).binding.drawerLayout.tag == "land_scape")
                        binding.bookmarkList.scrollBy(0, scroll + bookmarkListAddedScroll)
                    else
                        binding.bookmarkList.scrollBy(0, scroll + bookmarkListDisplace)

                    startPostponedEnterTransition()
//                    }
                }
            }
        }

//        setExitSharedElementCallback(object : SharedElementCallback() {
//            override fun onMapSharedElements(names: List<String>,
//                                             sharedElements: MutableMap<String, View>) {
//                val selectedViewHolder: RecyclerView.ViewHolder = binding.bookmarkList
//                    .findViewHolderForItemId(bookmarkViewModel.itemClickedBookmark) ?: return
//                sharedElements[names[0]] = selectedViewHolder.itemView.card_view
//            }
//        })
        binding.bookmarkToolbar.navigationContentDescription = resources.getString(R.string.navigation_up)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.bookmarkToolbar.setPadding(0, topPadding, 0, 0 )
        bookmarkListHeight(false)

        binding.bookmarkList.addOnScrollListener(bookListScrollListener)

        scrollViewHelper = ResultFastScrollViewHelper(
            binding.bookmarkList, null, ::getResultHeight)
        FastScrollerBuilder(binding.bookmarkList).apply { disableScrollbarAutoHide() }
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
                binding.bookmarkList.adapter = adapter
                scrollViewHelper?.apply {
                    scroll = 0
                    initialOffset = 0
                }
                settingViewModel.doneRefreshing()
            }
        }

        barsPreparation()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        layoutManager = null
        binding.bookmarkList.removeOnScrollListener(bookListScrollListener)
        binding.bookmarkList.adapter = null
        scrollViewHelper = null
        adapter = null

        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        holderJob.cancel()
    }

    fun setTransitionType(excView: View){
        exitTransition = Hold().setDuration(500).excludeTarget(excView, true)
        reenterTransition = MaterialFadeThrough().setDuration(500)
    }

    private fun bookmarkListHeight(computeAnyway: Boolean): Int{
        val newLastItem = layoutManager!!.findLastVisibleItemPosition()
        if (newLastItem != RecyclerView.NO_POSITION /*&& (bookmarkViewModel.lastItem < newLastItem )*/){
            if (bookmarkViewModel.lastItem < newLastItem || computeAnyway) {
                bookmarkViewModel.lastItem = newLastItem
                bookmarkViewModel.firstItem = layoutManager!!.findFirstVisibleItemPosition()

//            val displayedItemsCount = bookmarkViewModel.lastItem - bookmarkViewModel.firstItem
                val rect = Rect()

                layoutManager!!.findViewByPosition(bookmarkViewModel.lastItem)?.let {
                    binding.bookmarkList.getDecoratedBoundsWithMargins(it, rect)
                    bookmarkViewModel.bookmarkHeight =
                        ((rect.bottom + (scrollViewHelper?.scroll ?: 0)) *
                                adapter!!.itemCount / (bookmarkViewModel.lastItem + 1f)).toInt()
                }
            }
        } else
            binding.bookmarkList.post{
                if (layoutManager!!.itemCount > 0)
                    bookmarkListHeight(false)
            }

        return bookmarkViewModel.bookmarkHeight
    }

    private fun getResultHeight() = bookmarkViewModel.bookmarkHeight.let {
        if (it != 0) it else bookmarkListHeight(true)
    }


}