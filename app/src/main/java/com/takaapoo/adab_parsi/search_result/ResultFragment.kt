package com.takaapoo.adab_parsi.search_result

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils.TruncateAt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.SharedElementCallback
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.Hold
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.database.SearchContent
import com.takaapoo.adab_parsi.databinding.FragmentSearchResultBinding
import com.takaapoo.adab_parsi.poem.PoemViewModel
import com.takaapoo.adab_parsi.search.SearchViewModel
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.search_result_item.view.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.reflect.Field


@AndroidEntryPoint
class ResultFragment : Fragment() {

    private val poemViewModel: PoemViewModel by activityViewModels()
    val searchViewModel: SearchViewModel by activityViewModels()
    val settingViewModel: SettingViewModel by activityViewModels()

    private var _binding: FragmentSearchResultBinding? = null
    val binding get() = _binding!!
//    private lateinit var scrollViewHelper: ResultFastScrollViewHelper
    var layoutManager: LinearLayoutManager? = null
    var adapter: ResultListAdapter? = null
    var currentNightMode = 0

    var displayResultJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        motionInitialization()
        if (searchViewModel.comeFromDetailFragment)
            postponeEnterTransition()

        var applyExitSharedElement = true

        _binding = FragmentSearchResultBinding.inflate(inflater, container, false)
        val navController = findNavController()
        binding.resultToolbar.setupWithNavController(navController,
            AppBarConfiguration.Builder(navController.graph).build())

        binding.resultToolbar.title = searchViewModel.submittedSearch
        binding.resultToolbar.setOnClickListener {
            applyExitSharedElement = false
            searchViewModel.searchSubmit = false
            searchViewModel.searchQuery.value = searchViewModel.submittedSearch
            try {
                navController.navigate(ResultFragmentDirections
                    .actionResultFragmentToSearchFragment(searchViewModel.poemID, searchViewModel.catID))
            } catch (e: Exception) { }
        }

        layoutManager = binding.resultList.layoutManager as LinearLayoutManager
        currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        adapter = ResultListAdapter(this, currentNightMode == Configuration.UI_MODE_NIGHT_NO)
        binding.resultList.adapter = adapter
        binding.resultList.edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)


        binding.root.post {
            searchViewModel.apply {
                rootWidth = binding.root.measuredWidth
                mesraContainerWidth = (rootWidth - (startMargin + endMargin + verseSeparation)) / 2

                if (comeFromDetailFragment) {
                    adapter!!.submitList(searchResultList)
                    afterSubmit(searchResultList)
                } else {
                    search(submittedSearch.trim(' ', '‌' ), poemID, catID)
                        .observe(viewLifecycleOwner) { result ->
                            displayResultJob = CoroutineScope(Dispatchers.Main).launch {
                                val finalResult = sortedResults(result)
                                adapter!!.submitList(finalResult)
                                afterSubmit(finalResult)

                                comeFromDetailFragment = true

                                searchResultList = finalResult
                                searchViewModel.poemList = finalResult.map {
                                    Content(it.poemm.id!!, it.poemm.catID, it.poemm.title, 1)
                                }
                                poemViewModel.resultRowId1 = finalResult.map { it.rowId1 }
                                searchViewModel.poemCount = finalResult.size
                            }
                        }
                }

            }
        }

        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: List<String>,
                                             sharedElements: MutableMap<String, View>) {
                if (!applyExitSharedElement) return
//                val destID = findNavController().currentDestination?.id
                val selectedViewHolder: RecyclerView.ViewHolder = binding.resultList
                    .findViewHolderForAdapterPosition(searchViewModel.poemPosition) ?: return
//                selectedViewHolder.itemView.card_view.transitionName = "Result_transition"
                try {
                    sharedElements[names[0]] = selectedViewHolder.itemView.card_view
                } catch (e: java.lang.Exception) { }
            }
        })
        binding.resultToolbar.navigationContentDescription = resources.getString(R.string.navigation_up)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.resultToolbar.setPadding(0, topPadding, 0,topPadding / 2)

        binding.upArrow.isVisible = searchViewModel.scroll > 10
        binding.resultList.addOnScrollListener( object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                searchViewModel.scroll += dy
//                resultListHeight()
                binding.upArrow.isVisible = searchViewModel.scroll > 10
            }
        })

//        scrollViewHelper = ResultFastScrollViewHelper(
//            binding.resultList, null, ::getResultHeight)
//        FastScrollerBuilder(binding.resultList).apply { disableScrollbarAutoHide() }
//            .setThumbDrawable(
//                ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_thumb, context?.theme)!!
//            )
//            .setTrackDrawable(
//                ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_track, context?.theme)!!
//            )
//            .setViewHelper(scrollViewHelper)
//            .build()

        settingViewModel.refreshContent.observe(viewLifecycleOwner){
            if (it == true){
                binding.resultList.adapter = adapter
//                scrollViewHelper?.apply {
//                    scroll = 0
//                    initialOffset = 0
//                }
                settingViewModel.doneRefreshing()
            }
        }

        binding.upArrow.setOnClickListener { binding.resultList.smoothScrollToPosition(0) }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding.resultList.adapter = null
        adapter = null
        layoutManager = null

        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        displayResultJob?.cancel()
    }

    private suspend fun sortedResults(result: List<SearchContent>): List<SearchContent>{
        return withContext(Dispatchers.Default){
            val distinctResult = result.distinctBy { it.rowId1 + it.rowId2 }
            val resultRanks = mutableMapOf<Int, Float>()
            for (i in distinctResult.indices){
                if (!isActive) break
                resultRanks[distinctResult[i].rowId1] = resultRank(distinctResult[i])
            }
            if (!isActive)
                distinctResult
            else
                distinctResult.sortedWith { r1, r2 ->
                    when {
                        resultRanks[r1.rowId1] == resultRanks[r2.rowId1] -> 0
                        resultRanks[r1.rowId1]!! > resultRanks[r2.rowId1]!! -> -1
                        else -> 1
                    }
                }
        }
    }

    private fun motionInitialization(){
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 500
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 1f)
        }

        sharedElementReturnTransition = MaterialContainerTransform().apply {
            duration = 500
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 1f)
        }
    }

    fun setTransitionType(excView: View){
        exitTransition = Hold().setDuration(500).excludeTarget(excView, true)
        reenterTransition = MaterialFadeThrough().setDuration(500)
    }

//    fun resultListHeight(){
//        val newLastItem = layoutManager!!.findLastVisibleItemPosition()
//        if (searchViewModel.resultLastItem < newLastItem){
//            searchViewModel.resultLastItem = newLastItem
//            searchViewModel.resultFirstItem = layoutManager!!.findFirstVisibleItemPosition()
//
//            val displayedItemsCount = searchViewModel.resultLastItem - searchViewModel.resultFirstItem
//            val rect = Rect()
//
//            binding.resultList.getDecoratedBoundsWithMargins(
//                binding.resultList.getChildAt(displayedItemsCount), rect)
//
//            searchViewModel.resultHeight = ((rect.bottom + searchViewModel.scroll)*
//                    adapter!!.itemCount/(searchViewModel.resultLastItem+1f)).toInt()
//        }
//    }
//
//    private fun getResultHeight() = searchViewModel.resultHeight

    private fun afterSubmit(finalResult: List<SearchContent>){
        binding.progress.hide()
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.resultToolbar.subtitle = (if (finalResult.size > 10000)
            "${resources.getString(R.string.more_than_result_count, engNumToFarsiNum(10000))} "
            else "${resources.getString(R.string.result_count, engNumToFarsiNum(finalResult.size))} ") +
                    searchViewModel.searchDomain

        try {
            val field: Field = Toolbar::class.java.getDeclaredField("mSubtitleTextView")
            field.isAccessible = true
            val subtitleTextView = field.get(binding.resultToolbar) as TextView
            subtitleTextView.apply {
                isSingleLine = false
                maxLines = 2
                ellipsize = TruncateAt.END
                textSize = 14f
            }
        } catch (e: Exception) {
            Timber.e(e.toString())
        }

        binding.resultList.post {
            val scroll = searchViewModel.scroll
            searchViewModel.scroll = 0
            binding.resultList.scrollBy(0, scroll)

            val viewAtPosition = layoutManager!!.findViewByPosition(searchViewModel.poemPosition)
            if (viewAtPosition == null || layoutManager!!
                    .isViewPartiallyVisible(viewAtPosition, false, true)){
                searchViewModel.resultListDisplace -= searchViewModel.bottomViewedResultHeight
                binding.resultList.scrollBy(0, searchViewModel.resultListDisplace)
            }
            startPostponedEnterTransition()
        }
    }

    private fun resultRank(item: SearchContent): Float{
        val text = makeTextBiErab(when (item.position){
            0,2 -> "${item.majorText?.trim()} ${item.minorText?.trim()}"
            1,3 -> "${item.minorText?.trim()} ${item.majorText?.trim()}"
            else -> item.majorText?.trim() ?: ""
        })

        val splittingChars = charArrayOf(' ', '،', '!', '؛', ':', '؟', '.')
        val searchQuery =
            makeTextBiErab(searchViewModel.submittedSearch.filter { it.isLetterOrDigit() || it.isWhitespace()})
        val splittedQuery = searchQuery.split(*splittingChars)
        val splittedText = text.split(*splittingChars)

        val indices = mutableListOf<List<Int>>()
        splittedQuery.forEach {
            val allIndex = splittedText.allIndexOf(it)
            if (allIndex.isNotEmpty())
                indices.add(allIndex)
        }

        val foundPhrases = indices.map { it.size }.sum()        // number of search phrases in this result
        var phraseCharCount = 0f                                // total number of found phrases characters in this result
        indices.forEach {
            phraseCharCount += (splittedText.filterIndexed { index, _ -> it.contains(index) }
                .map { it.length }).average().toFloat()
        }

        var distance = 0f
        for (i in 0 until indices.size - 1){
            val lengthArray = mutableListOf<Float>()
            indices[i].forEach { first ->
                indices[i+1].forEach { second ->
                    val minIndex = minOf(first, second)
                    val maxIndex = maxOf(first, second)
                    val charDistance =
                        (splittedText.filterIndexed{ind, _ -> ind < maxIndex}.map { it.length }.sum() + maxIndex) -
                        (splittedText.filterIndexed{ind, _ -> ind < minIndex}.map { it.length }.sum()
                                + minIndex + splittedQuery[i].length)
                    lengthArray.add(if (first < second) charDistance.toFloat() else 1.5f * charDistance)
                }
            }
            distance += lengthArray.minOrNull() ?: 0f
        }

        val exactPhrasePoint = if (text.contains(searchQuery)) 100 else 0

        return when{
            indices.size <= 1 -> 4 * foundPhrases - phraseCharCount
            else -> 3*(foundPhrases - phraseCharCount) - distance + exactPhrasePoint
        }
    }




}