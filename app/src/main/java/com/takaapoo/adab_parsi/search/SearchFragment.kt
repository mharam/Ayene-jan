package com.takaapoo.adab_parsi.search

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.RecentSearch
import com.takaapoo.adab_parsi.database.SearchSuggest
import com.takaapoo.adab_parsi.databinding.FragmentSearchBinding
import com.takaapoo.adab_parsi.util.*
import com.takaapoo.adab_parsi.util.eventHandler.EventObserver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.search_layout.view.*
import java.util.*

@AndroidEntryPoint
class SearchFragment: Fragment() {

    val args: SearchFragmentArgs by navArgs()

    private val searchViewModel: SearchViewModel by activityViewModels()

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private var adapter: SearchListAdapter? = null
//    private var imm: InputMethodManager? = null
//    private val windowHeightMethod = InputMethodManager::class.java.
//            getMethod("getInputMethodWindowVisibleHeight")

    val omittingChars = "ًٌٍَُِّْٔ،!؛:؟»«."


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        postponeEnterTransition()

        val navController = findNavController()
        binding.toolbar.setupWithNavController(navController,
            AppBarConfiguration.Builder(navController.graph).build())

        binding.viewModel = searchViewModel
        searchViewModel.poemID = args.poemID
        searchViewModel.catID = args.catID
        searchViewModel.poemPosition = 0

        when (args.catID) {
            -1 -> {
                motionFromHomeInit()
                if (args.poemID == -1) {
                    binding.searchView.queryHint = "جستجو در آثار شعرای بارگذاری شده"
                    searchViewModel.searchDomain = (binding.searchView.queryHint as String).substring(6)
                }
            }
            else -> {
                motionNotFromHomeInit()
                when (args.poemID) {
                    -1 -> {
                        val poetName = allCategory.find { it.id == args.catID }?.text?.substringBefore('*')
                        binding.searchView.queryHint = "جستجو در آثار $poetName"
                        searchViewModel.searchDomain = (binding.searchView.queryHint as String).substring(6)
                    }
                    -2 -> {
                        val allCats = allUpCategories(args.catID).map {
                                elem -> allCategory.find { it.id == elem } }
                        binding.searchView.queryHint = if (allCats.size == 1) {
                            if (allCategory.count { it.parentID == allCats.last()?.id } == 0) // if book count == 0
                                "جستجو در مجموعه آثار ${allCats.last()?.text?.substringBefore('*')}"
                            else
                                "جستجو در سایر آثار ${allCats.last()?.text?.substringBefore('*')}"
                        } else
                            "جستجو در ${allCats[allCats.size - 2]?.text} ${allCats.last()?.text?.substringBefore('*')}"

                        searchViewModel.searchDomain = (binding.searchView.queryHint as String).substring(6)

                    }
                }
            }
        }

        adapter = SearchListAdapter(searchViewModel)
        binding.searchList.adapter = adapter
        binding.searchList.edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)
        binding.searchList.itemAnimator = null
        if (searchViewModel.searchQuery.value.isNullOrEmpty())
            searchViewModel.getHistorySuggest(null).observe(viewLifecycleOwner) {
                adapter!!.submitList(it.subList(0, it.size.coerceAtMost(5)))
            }

        searchViewModel.searchQuery.observe(viewLifecycleOwner) {
            binding.searchView.setQuery(it, searchViewModel.searchSubmit)
        }

//        imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        binding.root.postDelayed({
            binding.searchView.search_src_text.let { searchText ->
                if (searchText.requestFocus())
                    (activity as MainActivity).showKeyBoard(searchText)
            }
                                 }, 600)

        binding.toolbar.navigationContentDescription = resources.getString(R.string.navigation_up)

        barsPreparation()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setPadding(0, topPadding, 0, 0)
        var doSubmit: Boolean

        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{

            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchViewModel.submittedSearch = query
                    if (query.length <= 2)
                        LongRunningSearchDialogFragment().show(parentFragmentManager, "longSearch")
                    else
                        doSearch()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                doSubmit = true
                val whiteSpaces = newText?.count { it.isWhitespace() } ?: 0
                if (newText.isNullOrEmpty()) {
                    searchViewModel.getHistorySuggest(null).observe(viewLifecycleOwner) {
                        if (doSubmit) {
                            adapter!!.submitList(it.subList(0, it.size.coerceAtMost(5)))
                            doSubmit = false
                            startPostponedEnterTransition()
                        }
                    }
                } else {
                    val newTextBiErab = makeTextBiErab(newText.filter { it.isLetterOrDigit() ||
                            it.isWhitespace() }).trimStart()
                    val searchLiveDate =
                        if (args.poemID == -1 && args.catID == -1)
                            searchViewModel.getAllSearchSuggest(newTextBiErab, whiteSpaces + 1)
                        else
                            searchViewModel.getSearchSuggest(newTextBiErab, args.poemID, args.catID,
                                whiteSpaces + 1)

                    searchLiveDate.observe(viewLifecycleOwner) { resultSearch ->
                        searchViewModel.getHistorySuggest(newText)
                            .observe(viewLifecycleOwner) { resultHistory ->
                                val briefResultHistory = resultHistory.subList(
                                    0, resultHistory.size.coerceAtMost(3)
                                ).map { it.text }
                                val searchSuggest = resultSearch
                                    .map { it.text.filterNot { c -> omittingChars.contains(c) } }
                                    .flatMap { it.trim().mySplitToSequence(newTextBiErab) }
                                    .distinct()
                                    .filterNot { briefResultHistory.contains(it) }
                                    .sortedWith { s1, s2 -> (s1.length) - (s2.length) }
                                    .let {
                                        it.subList(
                                            0, it.size.coerceAtMost(
                                                (6 - resultHistory.size).coerceAtLeast(4)
                                            )
                                        )
                                    }
                                    .map { SearchSuggest(it, false) }.toMutableList()

                                val historySuggest = resultHistory.subList(
                                    0, resultHistory.size.coerceAtMost(6 - searchSuggest.size)
                                )

                                if (doSubmit) {
                                    adapter!!.submitList(searchSuggest.apply {
                                        addAll(
                                            0,
                                            historySuggest
                                        )
                                    })
                                    doSubmit = false
                                    startPostponedEnterTransition()
                                }
                            }
                    }
                }
                return true
            }
        })

        searchViewModel.longSearchDialogPosClick.observe(viewLifecycleOwner, EventObserver{
            doSearch()
        })

    }

//    private fun barsPreparation(){
//        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
//        val myActivity = requireActivity()
//
//        myActivity.window.decorView.systemUiVisibility =
//            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO)
//            myActivity.window.decorView.systemUiVisibility = myActivity.window.decorView.systemUiVisibility or
//                    (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
//    }

    private fun doSearch(){
        searchViewModel.apply {
            insertOrUpdate(RecentSearch(submittedSearch, Calendar.getInstance().timeInMillis))
            scroll = 0
            resultFirstItem = 0
            resultLastItem = 0
            detailPagerPosition = -1
            comeFromDetailFragment = false
            poemPosition = 0
        }
        try {
            findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToResultFragment())
        } catch (e: Exception) { }
    }

    fun String.mySplitToSequence(delimiter: String): List<String> {
        val inputBiErab = makeTextBiErab(this)
        if (!inputBiErab.contains(delimiter))
            return emptyList()
        else {
            val outList = mutableListOf<String>()
            var startIndex = inputBiErab.indexOf(delimiter, 0)

            while (startIndex != -1) {
                val startSpaceNum = inputBiErab.substring(0, startIndex).count { c -> c == ' ' }
                val endIndex = inputBiErab.indexOf(' ', startIndex+delimiter.length)
                val endSpaceNum = if (endIndex == -1) -1 else inputBiErab.substring(0, endIndex+1).count { c -> c == ' ' }

                if (startIndex == 0 || inputBiErab[startIndex-1] == ' ')
                    outList.add(this.mySubString(startSpaceNum, endSpaceNum).trim(' ', '‌' ))
                startIndex = inputBiErab.indexOf(delimiter, startIndex + delimiter.length)
            }
            return outList
        }
    }

    private fun motionFromHomeInit(){
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 500
//            pathMotion = null
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 1f)
        }

        sharedElementReturnTransition = MaterialContainerTransform().apply {
            duration = 500
//            pathMotion = null
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 1f)
        }
    }

    private fun motionNotFromHomeInit(){
        enterTransition = MaterialFadeThrough().apply { duration = 500 }
        returnTransition = MaterialFadeThrough().apply { duration = 500 }
    }

    override fun onStop() {
//        val height = windowHeightMethod.invoke(imm) as Int
//        if (height>100) {
//            imm?.toggleSoftInput(0, 0)
//        }
//        (activity as MainActivity).hideKeyBoard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()

//        imm = null
        binding.searchList.adapter = null
        adapter = null
        _binding = null
    }

    override fun onDestroy() {
        searchViewModel.searchQuery.value = ""
        super.onDestroy()
    }

}


class LongRunningSearchDialogFragment : DialogFragment() {

    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.long_running_search_title)
                .setMessage(R.string.long_search_message)
                .setPositiveButton(R.string.long_search_pos_button) { _, _ -> searchViewModel.runSearch() }
                .setNegativeButton(R.string.long_search_neg_button) { _: DialogInterface, _: Int ->  }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}