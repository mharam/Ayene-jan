package com.takaapoo.adab_parsi.book

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.takaapoo.adab_parsi.NavGraphDirections
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.databinding.PagerBook2Binding
import com.takaapoo.adab_parsi.poem.DARK_ALPHA_MAX
import com.takaapoo.adab_parsi.poem.PoemViewModel
import com.takaapoo.adab_parsi.poet.PoetViewModel
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.*
import com.takaapoo.adab_parsi.util.fastScroll.BookContentFastScrollViewHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.lang.reflect.Field
import java.util.*
import kotlin.collections.set
import kotlin.math.roundToInt


@AndroidEntryPoint
class BookPagerFragment : Fragment() {

    val poetViewModel: PoetViewModel by activityViewModels()
    val bookViewModel: BookViewModel by activityViewModels()
    val poemViewModel: PoemViewModel by activityViewModels()
    val settingViewModel: SettingViewModel by activityViewModels()

    private var _binding: PagerBook2Binding? = null
    val binding get() = _binding!!
    private var adapter: BookContentAdapter? = null
    private lateinit var navController: NavController
    private var layoutManager: LinearLayoutManager? = null
    lateinit var contentItem: Content
    var scrollViewHelper: BookContentFastScrollViewHelper? = null

    val rect = Rect()

    private lateinit var newList: MutableList<Content>
    private var sortedItems = List(0){Content()}
    private lateinit var sortedItemsHeight : HashMap<Int, Int>
    private var newListAggregatedHeight = mutableListOf<Int>()

    private val contentScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (!recyclerView.canScrollVertically(1)) {
                scrollViewHelper?.modifyScroll()
            }
//            if (!recyclerView.canScrollVertically(-1)) {
//                scrollViewHelper?.scroll = 0
//            }
        }
    }

    private lateinit var holderJob: Job
    private lateinit var holderScope: CoroutineScope


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?
                              , savedInstanceState: Bundle?): View {

        _binding = PagerBook2Binding.inflate(inflater, container, false)
        navController = findNavController()
        holderJob = Job()
        holderScope = CoroutineScope(Dispatchers.Main + holderJob)

        binding.bookToolbar
            .setupWithNavController(navController, AppBarConfiguration.Builder(navController.graph).build())

        binding.bookToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId){
                R.id.search -> {
                    try {
                        requireParentFragment().exitTransition = MaterialFadeThrough()
                        requireParentFragment().reenterTransition = MaterialFadeThrough()
                        navController.navigate(NavGraphDirections
                            .actionGlobalSearchFragment(-2, contentItem.id))
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            true
        }

        //noinspection RestrictedApi
        bookViewModel.bookWidthMultiplier.let {
            (binding.bookTitle.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = (resources.getDimension(R.dimen.book_title_margin_start) * it).toInt()
                marginEnd = (resources.getDimension(R.dimen.book_title_margin_end) * it).toInt()
            }

            binding.bookTitle.setAutoSizeTextTypeUniformWithConfiguration(
            (resources.getDimension(R.dimen.book_title_min_size) * it).roundToInt(),
            (resources.getDimension(R.dimen.book_title_max_size) * it).roundToInt(),
            (resources.getDimension(R.dimen.book_title_size_step) * it).roundToInt(),
            TypedValue.COMPLEX_UNIT_PX)
        }


        binding.bookCover.doOnPreDraw {
            binding.pivX = it.measuredWidth.toFloat()
            binding.pivY = it.measuredHeight / 2f
            it.cameraDistance = 30f * it.measuredWidth
        }
        binding.bookToolbar.setNavigationOnClickListener {
            closeBook(navController)
        }
        binding.bookToolbar.navigationContentDescription = resources.getString(R.string.navigation_up)

        binding.bookContentList.addOnScrollListener(contentScrollListener)

        GlideApp.with(this).load(R.drawable.book_open1).into(binding.backBook)
        GlideApp.with(this).load(R.drawable.book_open2).into(binding.backBookPaper)
        GlideApp.with(this).load(R.drawable.bookcover2).into(binding.bookCoverImage)


        settingViewModel.paperColorPref.observe(viewLifecycleOwner){
            settingViewModel.currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (settingViewModel.currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                binding.bookContent.setBackgroundColor(settingViewModel.paperColor)
                binding.appBar.setBackgroundColor(settingViewModel.paperColor)
            }
        }
        settingViewModel.brightness.observe(viewLifecycleOwner){
            binding.darkener.alpha = DARK_ALPHA_MAX * (1 - it / 100)
        }

        layoutManager = binding.bookContentList.layoutManager as LinearLayoutManager

        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val actionBarSize = requireContext().getDimenFromAttr(R.attr.actionBarSize)
        val titleSubTitleOverlap = 16.dpTOpx(resources).toInt()
        val wholeTitleHeight = 2 * actionBarSize - titleSubTitleOverlap
        val collapseToolbarHeight =
            wholeTitleHeight + topPadding + resources.getDimensionPixelSize(R.dimen.book_content_height)

        (binding.bookToolbar.layoutParams as ViewGroup.MarginLayoutParams).let {
            it.topMargin = topPadding
            it.height = wholeTitleHeight
            it.bottomMargin = resources.getDimensionPixelSize(R.dimen.book_content_height) -
                    (it.topMargin + it.height)
        }

        (binding.collapseToolbar.layoutParams as ViewGroup.MarginLayoutParams).height = collapseToolbarHeight
        (binding.fehrestTitle.layoutParams as ViewGroup.MarginLayoutParams).height =
            resources.getDimensionPixelSize(R.dimen.book_content_height)
        (binding.toolbarTitle.layoutParams as ViewGroup.MarginLayoutParams).topMargin = topPadding
        (binding.toolbarSubtitle.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
            actionBarSize + topPadding - titleSubTitleOverlap

        val field: Field = Toolbar::class.java.getDeclaredField("mTitleTextView")
        field.isAccessible = true
        (field.get(binding.bookToolbar) as TextView).doOnPreDraw {
            binding.toolbarTitle.updatePadding(right = view.width - it.right)
            binding.toolbarSubtitle.updatePadding(right = view.width - it.right)
        }


        scrollViewHelper = BookContentFastScrollViewHelper(
            binding.bookContentList, null, this@BookPagerFragment
        )
        FastScrollerBuilder(binding.bookContentList).apply { disableScrollbarAutoHide() }
            .setThumbDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_thumb, context?.theme)!!
            )
            .setTrackDrawable(ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_track,
                context?.theme)!!
            )
            .setViewHelper(scrollViewHelper)
            .build()


        binding.item = Content(7, 0, " ", 1)

        arguments?.takeIf { it.containsKey(ARG_BOOK_POSITION) }?.apply {
            contentItem = poetViewModel.bookListItems[getInt(ARG_BOOK_POSITION)]!!
            binding.title = contentItem.text
            adapter = BookContentAdapter(this@BookPagerFragment)
            binding.bookContentList.adapter = adapter
            binding.bookContentList.edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)

            (if (contentItem.parentID == 0) contentItem.id else contentItem.parentID)?.let { parentId ->
                binding.toolbarTitle.text = allCategory.find { it.id == parentId }?.text?.substringBefore('*')
                poetViewModel.updatePoetDate(Calendar.getInstance().timeInMillis, parentId)
            }

            holderScope.launch {
                withContext(Dispatchers.IO){
                    sortedItems = sortContent(poetViewModel.getAllPoemWithCatID(contentItem.id))
                }
                poetViewModel.poemListItems[contentItem.id] =
                    if (contentItem.parentID == 0)
                        sortedItems.filter { item ->
                            item.parentID == contentItem.id && item.rowOrder == 1 }
                    else
                        sortedItems.filter { item -> item.rowOrder == 1 }

                val poemItems = sortedItems.filter { item -> item.parentID == contentItem.id }

                newList = if (contentItem.parentID == 0)
                    poemItems.filterNot { item -> item.rowOrder == 2 }.toMutableList()
                else poemItems.toMutableList()

                if (contentItem.parentID != 0)
                    sortedItems.filter { elem -> elem.rowOrder == 2 }.forEach { item ->
                        (bookViewModel.listOpen[item.id] ?: false).let { open ->
                            val subItems = sortedItems.filter { elem -> elem.parentID == item.id }
                            subItems.forEach { elem -> elem.rank = item.rank + 1 }
                            poetViewModel.poemListSubItems[item.id] = subItems
                            if (open)
                                newList.addAll(newList.indexOfFirst { elem -> elem.id == item.id } + 1,
                                    poetViewModel.poemListSubItems[item.id] ?: emptyList()
                                )
                        }
                    }

                adapter?.submitList(newList)
                parentFragment?.startPostponedEnterTransition()

                binding.bookCover.apply {
                    doOnPreDraw {
                        if (bookViewModel.bookFirstOpening) {
                            bookViewModel.bookFirstOpening = false
                            ObjectAnimator.ofFloat(this, "rotationY", 0f, 90f).apply {
                                startDelay = 700
                                duration = 500
                                interpolator = LinearInterpolator()
                            }.start()

                            val scaleXOutValue = TypedValue()
                            val scaleYOutValue = TypedValue()
                            resources.getValue(R.integer.open_book_init_scale_x, scaleXOutValue, true)
                            resources.getValue(R.integer.open_book_init_scale_y, scaleYOutValue, true)
                            binding.bookLayout.animate()
                                .setDuration(300)
                                .withEndAction {
                                    (parentFragment as BookFragment).changeBackgroundImage()
                                }
                                .setStartDelay(1000)
                                .scaleX(1 / scaleXOutValue.float)
                                .scaleY(1 / scaleYOutValue.float)
                                .setInterpolator(LinearInterpolator())
                                .start()
                        }
                    }
                }

                bookViewModel.bookContentScrollPosition[contentItem.id]?.let { positionItem ->
                    layoutManager?.scrollToPosition(newList.indexOf(positionItem))

                    binding.bookContentList.doOnLayout {
                        ((binding.bookContentList.findViewHolderForItemId(positionItem.id.toLong())
                            ?.itemView as? ViewGroup)?.getChildAt(0) as TextView?)?.apply {
                            pivotX = width.toFloat()
                            pivotY = height/2f
                            scaleX = 1.1f
                            scaleY = 1.1f

                            val currentColor = currentTextColor
                            ObjectAnimator.ofArgb(this
                                , "textColor", Color.argb(0xff,0,0xff,0)
                                , currentColor).apply {
                                startDelay = 500
                                duration = 2000
                                interpolator = LinearInterpolator()
                            }.start()
                            animate().scaleX(1f).scaleY(1f)
                                .setStartDelay(500).setInterpolator(LinearInterpolator())
                                .setDuration(500).start()
                        }
                    }
                }

                withContext(Dispatchers.Default) {
                    var staticLayout: StaticLayout
                    val layoutPaint = TextPaint().apply {
                        textSize = settingViewModel.fontSize.spTOpx(resources)
                        typeface = settingViewModel.font
                        textAlign = Paint.Align.RIGHT
                    }
                    val lineSpacing = if (settingViewModel.fontPref == 0) 0f
                        else resources.getDimension(R.dimen.not_nastaliq_line_spacing)
                    val vertPadding = (2 * if (settingViewModel.fontPref == 0) 0f
                        else resources.getDimension(R.dimen.not_nastaliq_vertical_padding)).toInt()

                    sortedItemsHeight = HashMap(sortedItems.size)
                    while (parentFragment?.view?.width == null || requireParentFragment().requireView().width < 100)
                        delay(5)

                    sortedItems.forEach { item ->
                        val width = requireParentFragment().requireView().width -
                                    resources.getDimension(R.dimen.content_item_end_margin) -
                                    resources.getDimension(R.dimen.content_item_start_padding) -
                                    item.rank * resources.getDimension(R.dimen.book_content_margin)

                        val text = item.text ?: ""
                        staticLayout = StaticLayout.Builder.obtain(
                            text, 0, text.length, layoutPaint, width.toInt())
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(lineSpacing, 1f)
                            .build()
                        sortedItemsHeight[item.id] = staticLayout.height + vertPadding
                    }

                    modifyAggregatedHeight(newList)
                }
                // It crashed with java.lang.IllegalStateException: DataBinding must be created
                // in view's UI Thread. So I was forced to bring it out of withContext.
                binding.bookContentList.scrollBy(0, 0)
            }
        }
    }

    override fun onPause() {
        super.onPause()
//        bookViewModel.bookContentScrollHeight[contentItem.id] = scrollViewHelper!!.scroll
        bookViewModel.bookContentScrollPosition.remove(contentItem.id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        holderJob.cancel()
        binding.bookToolbar.setOnMenuItemClickListener(null)
        binding.bookToolbar.setNavigationOnClickListener(null)
        binding.bookContentList.removeOnScrollListener(contentScrollListener)

        layoutManager = null
        binding.bookContentList.adapter = null
        adapter = null
        scrollViewHelper = null

        _binding = null
    }


    fun backCallback(){
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            closeBook(navController)
        }
    }

//    fun bookContentHeight(runAnyway: Boolean): Int {
//        if (binding.bookContentList.isEmpty())
//            return 0
//        val newLastItem = layoutManager!!.findLastVisibleItemPosition()
//
//        if ((lastItem < newLastItem || lastItemCount != adapter!!.itemCount) || runAnyway) {
//            lastItem = newLastItem
//            lastItemCount = adapter!!.itemCount
////            firstItem = layoutManager.findFirstVisibleItemPosition()
////            val displayedItemsCount = lastItem - firstItem
//            val lastChild = binding.bookContentList.children.last()
//            val passedItemsCount =
//                binding.bookContentList.findContainingViewHolder(lastChild)?.bindingAdapterPosition ?: 0
//
//            if (passedItemsCount != RecyclerView.NO_POSITION){
//                binding.bookContentList.getDecoratedBoundsWithMargins(lastChild, rect)
//                bookViewModel.bookContentHeight[contentItem.id] =
//                    ((rect.bottom + scrollViewHelper!!.scroll)*adapter!!.itemCount/(passedItemsCount+1f)).toInt()
//            }
//        }
//        return bookViewModel.bookContentHeight[contentItem.id] ?: 0
//    }

    fun exactBookContentHeight(): Int{
        return newListAggregatedHeight.lastOrNull() ?: binding.bookContentList.height
    }

    suspend fun modifyAggregatedHeight(currentList: MutableList<Content>){
        while (!::sortedItemsHeight.isInitialized || !sortedItemsHeight.containsKey(sortedItems.last().id))
            delay(20)
        newListAggregatedHeight = MutableList(currentList.size){ 0 }
        newListAggregatedHeight[0] = sortedItemsHeight[currentList[0].id]!!
        for (i in 1 until currentList.size){
            newListAggregatedHeight[i] = newListAggregatedHeight[i-1] +
                    sortedItemsHeight[currentList[i].id]!!
        }
    }

    fun getScrollOffset(): Int{
        layoutManager?.findFirstVisibleItemPosition()?.let { firstItem ->
            if (firstItem == RecyclerView.NO_POSITION)
                return 0
            val firstViewTop = layoutManager!!.findViewByPosition(firstItem)!!.top
            return newListAggregatedHeight.getOrElse(firstItem - 1){0} - firstViewTop
        }
        return 0
    }

    fun setTransitionType(){
        requireParentFragment().exitTransition = null
        requireParentFragment().reenterTransition = null
    }

    private fun closeBook(navController: NavController) {
        binding.bookCover.animate()
            .setDuration(500)
            .setStartDelay(100)
            .rotationY(0f)
            .setInterpolator(LinearInterpolator())
            .withEndAction {navController.popBackStack()}
            .start()

        val scaleX1 = binding.bookLayout.scaleX
        val scaleY1 = binding.bookLayout.scaleY
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = LinearInterpolator()
            addUpdateListener {
                binding.bookLayout.scaleX = (it.animatedValue as Float)*(1 - scaleX1) + scaleX1
                binding.bookLayout.scaleY = (it.animatedValue as Float)*(1 - scaleY1) + scaleY1
            }
        }.start()
    }

    fun searchIconCenter(): IntArray{
        val location = IntArray(2){0}
        val fragLocation = IntArray(2){0}
        val searchView = binding.bookToolbar.findViewById<View>(R.id.search)

        searchView.getLocationInWindow(location)
        binding.bookContent.getLocationInWindow(fragLocation)

        location[0] = location[0] - fragLocation[0] + searchView.width/2
        location[1] = location[1] - fragLocation[1] + searchView.height/2

        return location
    }

    private fun sortContent(input: List<Content>): List<Content>{
        val outList = input.filter { elem -> elem.rowOrder == 2 }.toMutableList()
        var upCats = outList.map { allUpCategories(it.id).toMutableList() }
        val maxUpCatsLength = upCats.map { it.size }.maxOrNull() ?: 0
        upCats = upCats.map {
            it.addAll(0, MutableList(maxUpCatsLength - it.size){0})
            it }

        val mapUpCats = mutableMapOf(*Array(upCats.size){i -> Pair(outList[i], upCats[i])})

        for (i in 0 until maxUpCatsLength){
            outList.sortBy { mapUpCats[it]?.get(i) }
        }
        outList.addAll(0, input.filter { it.parentID == contentItem.id && it.rowOrder == 1 })

        outList.filter { elem -> elem.rowOrder == 2 }.forEach { item ->
            val subItems = input.filter { elem -> elem.parentID == item.id && elem.rowOrder == 1 }
            outList.addAll(outList.indexOfFirst { elem -> elem.id == item.id } + 1, subItems)
        }

        return outList
    }

    fun refreshContent(){
        binding.bookContentList.adapter = adapter
        scrollViewHelper?.apply {
//            scroll = 0
            initialOffset = 0
        }
        binding.fehrestTitle.invalidate()
    }

}