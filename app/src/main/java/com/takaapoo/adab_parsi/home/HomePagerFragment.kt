package com.takaapoo.adab_parsi.home

import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.add.ARG_ADD_PAGE
import com.takaapoo.adab_parsi.databinding.PagerHomeBinding
import com.takaapoo.adab_parsi.util.*
import com.takaapoo.adab_parsi.util.fastScroll.HomeFastScrollViewHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.zhanghai.android.fastscroll.FastScrollerBuilder


@AndroidEntryPoint
class HomePagerFragment: Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()

    private var adapter: PoetListAdapter? = null
    private var _binding: PagerHomeBinding? = null
    val binding get() = _binding!!

    private var tracker: SelectionTracker<Long>? = null
    private var actionMode: ActionMode? = null
    private var onStopCalled = false
    private var layoutManager: GridLayoutManager? = null
    private var mediaPlayer: MediaPlayer? = null

    val callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.home_contextual_action_bar, menu)
            homeViewModel.reportEvent(HomeEvent.OnCreateActionMode)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.delete -> {
                    homeViewModel.reportEvent(HomeEvent.OnDeleteClick(tracker!!.selection))
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            if (!onStopCalled) {
                tracker?.clearSelection()
            }
            homeViewModel.reportEvent(HomeEvent.OnDestroyActionMode)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = PagerHomeBinding.inflate(inflater, container, false)
        onStopCalled = false

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(ARG_ADD_PAGE) }?.apply {
            binding.loadedPoetList.contentDescription = context?.getString(
                when(getInt(ARG_ADD_PAGE)){
                    0 -> R.string.content_desc_ancient_poet_list
                    else -> R.string.content_desc_recent_poet_list
                }
            )

            layoutManager = binding.loadedPoetList.layoutManager as GridLayoutManager
            adapter = PoetListAdapter(homeViewModel, getInt(ARG_ADD_PAGE))
            binding.loadedPoetList.adapter = adapter
            binding.loadedPoetList.edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)

            tracker = SelectionTracker.Builder(
                "selection-1",
                binding.loadedPoetList,
                RecyclerViewIdKeyProvider(binding.loadedPoetList),
                MyLookup(binding.loadedPoetList, getInt(ARG_ADD_PAGE)),
                StorageStrategy.createLongStorage()
            )
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()

            if(savedInstanceState != null)
                tracker?.onRestoreInstanceState(savedInstanceState)

            adapter!!.setTracker(tracker)
            tracker?.addObserver(object: SelectionTracker.SelectionObserver<Long>() {
                override fun onSelectionChanged() {
                    val nItems:Int? = tracker?.selection?.size()
                    if (nItems != null){
                        if (nItems == 0)
                            finishActionMode()
                        else {
                            if (actionMode == null) {
                                actionMode = (requireActivity() as AppCompatActivity)
                                    .startSupportActionMode(callback)
                            }
                            actionMode?.title = resources.getString(R.string.poet_selected, nItems)
                        }
//                        for (i in 0 until adapter.itemCount) {
//                            val itemBinding = (binding.loadedPoetList.findViewHolderForLayoutPosition(i) as?
//                                    PoetListAdapter.ViewHolderAncient)?.binding
//                            itemBinding?.checkBox?.visibility = if (tracker!!.hasSelection())
//                                View.VISIBLE else View.INVISIBLE
//                            itemBinding?.checkBox?.isChecked =
//                                tracker!!.isSelected(itemBinding?.catItem?.poetID?.toLong())
//                        }
                    }
                }
            })
            tracker?.selection?.let {
                if (it.size()>0){
                    actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(callback)
                    actionMode?.title = resources.getString(R.string.poet_selected, it.size())
                }
            }

            val sharedPreference = PreferenceManager.getDefaultSharedPreferences(requireContext())
            view.doOnPreDraw {
                layoutManager!!.spanCount = (view.width / 320.dpTOpx(resources)).toInt().coerceAtLeast(1)
//                homeViewModel.spanCount = layoutManager!!.spanCount

                homeViewModel.allCat.observe(viewLifecycleOwner) { items ->
                    allCategory = items
                    allCategory.filter { it.ancient == getInt(ARG_ADD_PAGE) && it.parentID == 0 }
                        .let { list ->
                            when (getInt(ARG_ADD_PAGE)) {
                                0 -> homeViewModel.ancientPoetIds = list.map { it.poetID }
                                1 -> homeViewModel.recentPoetIds = list.map { it.poetID }
                            }
                            if (list.size < adapter!!.itemCount &&
                                !sharedPreference.getBoolean("sound", false)
                            ) {
                                mediaPlayer =
                                    MediaPlayer.create(requireContext(), R.raw.sweep)?.apply {
                                        setVolume(0.7f, 0.7f)
                                        start()
                                    }
                            }

                            adapter!!.submitList(null)
                            adapter!!.submitList(list)

//                        val firstPosition = if (getInt(ARG_ADD_PAGE) == 0) homeViewModel.firstOpenedAncient
//                        else homeViewModel.firstOpenedRecent

                            if (list.isNotEmpty())
                                scrollToPosition()
                            else
                                parentFragment?.startPostponedEnterTransition()
                        }
                }

                val viewHelper = HomeFastScrollViewHelper(binding.loadedPoetList, null)
                FastScrollerBuilder(binding.loadedPoetList)/*.apply { disableScrollbarAutoHide() }*/
                    .setPadding(0, 0, 0, 0)
                    .setThumbDrawable(
                        ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_thumb, context?.theme)!!)
                    .setTrackDrawable(
                        ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_track, context?.theme)!!)
                    .setViewHelper(viewHelper)
                    .build()
            }

            homeViewModel.actionModeState.onEach {
                if (it == ActionModeState.GONE)
                    finishActionMode()
            }
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }
    }

    override fun onStop() {
        super.onStop()
        onStopCalled = true
        finishActionMode()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tracker = null
        layoutManager = null
        adapter!!.setTracker(null)
        binding.loadedPoetList.adapter = null
        adapter = null

        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tracker?.onSaveInstanceState(outState)
    }

    fun finishActionMode(){
        actionMode?.finish()
        actionMode = null
    }

    fun getSelectedPoetList() = tracker?.selection?.toList()

    private fun scrollToPosition() {
        layoutManager!!.scrollToPositionWithOffset(homeViewModel.viewpagePosition, -1)
        binding.loadedPoetList.doOnNextLayout {
            parentFragment?.startPostponedEnterTransition()
            Handler(Looper.getMainLooper()).postDelayed({
                homeViewModel.viewpagePosition = 0
            }, 300)
        }
    }

    fun firstItemRectangle(): Rect {
        val gap = 8.dpTOpx(resources).toInt()
        val rect = Rect()
        var firstItem = layoutManager!!.findFirstCompletelyVisibleItemPosition()
        if (firstItem == RecyclerView.NO_POSITION)
            firstItem = layoutManager!!.findFirstVisibleItemPosition()

        binding.loadedPoetList.findViewHolderForAdapterPosition(firstItem)?.itemView?.let {
            binding.loadedPoetList.getDecoratedBoundsWithMargins(it, rect)
            rect.set(rect.left + gap, rect.top - 24.dpTOpx(resources).toInt() + gap,
                rect.right - gap, rect.bottom - gap)
        }

        return rect
    }

}

