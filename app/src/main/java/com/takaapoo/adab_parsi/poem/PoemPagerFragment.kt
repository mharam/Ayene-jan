package com.takaapoo.adab_parsi.poem

import android.animation.ValueAnimator
import android.app.ProgressDialog.show
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.bookmark.BookmarkViewModel
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.database.FavoriteContent
import com.takaapoo.adab_parsi.database.Poem
import com.takaapoo.adab_parsi.database.Verse
import com.takaapoo.adab_parsi.databinding.PagerPoemBinding
import com.takaapoo.adab_parsi.favorite.FavoriteDetailFragment
import com.takaapoo.adab_parsi.favorite.FavoriteViewModel
import com.takaapoo.adab_parsi.search.SearchViewModel
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.*
import com.takaapoo.adab_parsi.util.Orientation
import com.takaapoo.adab_parsi.util.custom_views.TextSelectableView
import com.takaapoo.adab_parsi.util.fastScroll.PoemFastScrollViewHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.poem_item.view.*
import kotlinx.android.synthetic.main.search_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import timber.log.Timber
import java.io.File
import java.lang.reflect.Field
import java.util.*
import kotlin.math.max


@AndroidEntryPoint
class PoemPagerFragment : Fragment() {

    val poemViewModel: PoemViewModel by activityViewModels()
    val searchViewModel: SearchViewModel by activityViewModels()
    private val bookmarkViewModel: BookmarkViewModel by activityViewModels()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()
    val settingViewModel: SettingViewModel by activityViewModels()

    private var _binding: PagerPoemBinding? = null
    val binding get() = _binding!!
    private var adapter: PoemContentAdapter? = null

    var mesraMaxWidth = 0
    val mesraWidth = mutableMapOf<Int, Int>()
    var beitOneLine = false
    var mesraOverlapWidth = 0
    var paragraphWidth = 0

    private var startMargin = 0
    private var endMargin = 0
    private var guidWidth = 0
    private var verseSeparation = 0
    private var versePadding = 0

    private var viewHelper: PoemFastScrollViewHelper? = null
    private var layoutManager: LinearLayoutManager? = null
    private var lastItem = 0
    private var firstItem = 0
    var verses = emptyList<Verse>()

    lateinit var poemItem: Content
    private var itemRoot = mutableListOf<String?>()
    var resultPositions = mutableListOf<Int>()
    var resultCount = 0
    private var smoothScroller: SmoothScroller? = null

    private var bookmarkAddress = ""

//    private var actionMode: ActionMode? = null
    private var mainActivity: MainActivity? = null
    private var collapseButton: ImageButton? = null
    private var searchMenuItem: MenuItem? = null
    private var mDetector: GestureDetectorCompat? = null
    private var mSelectedVerses = listOf<Int>()

    private var commentHeight = 0
    private var imm : InputMethodManager? = null
    lateinit var poemHilighter: PoemHilighter

    var textMenuTextView: TextView? = null
    private val fragLocation = IntArray(2){0}

    private var firstObservation = true
    private var mediaPlayer: MediaPlayer? = null
    private var mySearchView: SearchView? = null

    var poemExporter: PoemExporter? = null


    val callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.poem_contextual_action_menu, menu)
            ValueAnimator.ofArgb(if (settingViewModel.currentNightMode == Configuration.UI_MODE_NIGHT_NO)
                settingViewModel.paperColor else requireContext().getColorFromAttr(R.attr.colorSurface),
                requireContext().getColorFromAttr(R.attr.colorBeitSelect)).apply {
                addUpdateListener { updatedAnimation ->
                    requireActivity().window.statusBarColor = updatedAnimation.animatedValue as Int
                }
            }.start()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            binding.poemList.requestFocus()

            return when (item?.itemId) {
                R.id.favorite -> {
                    applyFavorite()
                    true
                }
                R.id.note -> {
                    addNote(mSelectedVerses[0])
                    finishActionMode()
                    true
                }
                R.id.hilight -> {
                    poemHilighter.fullHilight(mSelectedVerses)
                    true
                }
                R.id.copy -> {
                    val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    var text = ""
                    mSelectedVerses.sorted().let {
                        for (i in it.indices){
                            text += if (i == 0) verses[it[0]-1].text else "\n${verses[it[i]-1].text}"
                            when (verses[it[i]-1].position){
                                0 -> {
                                    if (it[i]<verses.size && verses[it[i]].position == 1)
                                        text += "          ${verses[it[i]].text}"
                                }
                                2 -> {
                                    if (it[i]<verses.size && verses[it[i]].position == 3) {
                                        text += "\n${verses[it[i]].text}"
                                    }
                                }
                            }
                        }
                    }
                    val clip = ClipData.newPlainText("sher", text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            poemViewModel.selectedVerses[poemItem.id]?.value = mutableListOf()
            (activity as MainActivity).statusBarColoring()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        _binding = PagerPoemBinding.inflate(inflater, container, false)
//        binding.root.setBackgroundColor(requireContext().getColorFromAttr(R.attr.colorSurface))
        adapter = PoemContentAdapter(this)
        binding.poemList.adapter = adapter
        binding.poemList.edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)
        binding.poemList.itemAnimator = null
//        binding.poemList.recycledViewPool.setMaxRecycledViews(1,0)
//        binding.poemList.setItemViewCacheSize(200)
        binding.upButton.isEnabled = false
        binding.downButton.isEnabled = false
        searchMenuItem = binding.poemToolbar.menu.findItem(R.id.search)
        poemViewModel.poemListLayoutCompleted.value = false

        mySearchView = searchMenuItem!!.actionView as SearchView
        mySearchView!!.maxWidth = Int.MAX_VALUE
        mySearchView!!.search_bar.layoutDirection = LAYOUT_DIRECTION_LTR
        mySearchView!!.search_plate.layoutDirection = LAYOUT_DIRECTION_RTL
        val searchPlate = mySearchView!!.findViewById<LinearLayout>(androidx.appcompat.R.id.search_plate)
        val searchText = mySearchView!!.findViewById<AutoCompleteTextView>(androidx.appcompat.R.id.search_src_text)


        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val navController = findNavController()
            binding.poemToolbar.setupWithNavController(navController,
                AppBarConfiguration.Builder(navController.graph).build())
        }
        smoothScroller = object : LinearSmoothScroller(requireContext()) {
            override fun getVerticalSnapPreference() = SNAP_TO_START
//            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?) =
//                (displayMetrics?.density ?: 1f) / 10

            override fun calculateTimeForScrolling(dx: Int): Int {
                return dx.coerceIn(200, 1500)
            }

            override fun calculateTimeForDeceleration(dx: Int): Int {
                return 900
            }

            override fun onStop() { // To cause App bar lift up
                super.onStop()
                adapter!!.notifyDataSetChanged()
            }
        }

        searchMenuItem!!.setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    return true // Return true to collapse action view
                }

                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    binding.toolbarSubtitle.visibility = View.INVISIBLE
                    binding.resultDashboard.visibility = View.VISIBLE
                    val searchPlateParams = searchPlate.layoutParams

                    ValueAnimator.ofFloat(0f, 1f).apply {
                        addUpdateListener { updatedAnimation ->
                            searchPlateParams.width =
                                ((updatedAnimation.animatedValue as Float) * mySearchView!!.width).toInt()
                            searchPlate.layoutParams = searchPlateParams
                        }
                        doOnEnd {
                            searchText.requestFocus()
                            binding.toolbarTitle.visibility = View.INVISIBLE
                            try {
                                val field: Field =
                                    Toolbar::class.java.getDeclaredField("mCollapseButtonView")
                                field.isAccessible = true
                                collapseButton = field.get(binding.poemToolbar) as ImageButton

                                collapseButton?.setOnClickListener {
                                    searchText.text.clear()
                                    ValueAnimator.ofFloat(1f, 0f).apply {
                                        addUpdateListener { updatedAnimation ->
                                            searchPlateParams.width =
                                                ((updatedAnimation.animatedValue as Float) *
                                                        mySearchView!!.width).toInt()
                                            searchPlate.layoutParams = searchPlateParams
                                        }
                                        doOnEnd { searchMenuItem!!.collapseActionView() }
                                    }.start()

                                    binding.toolbarSubtitle.visibility = View.VISIBLE
                                    binding.toolbarTitle.visibility = View.VISIBLE
                                    binding.resultDashboard.visibility = View.GONE
                                    poemViewModel.searchQuery = ""
                                }
                            } catch (e: Exception) {
                                Timber.e(e.toString())
                            }
                        }
                    }.start()

                    return true // Return true to expand action view
                }
            })



        val sharedPreference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        mySearchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                resultCount = searchResultCount(newText)
                if (!sharedPreference.getBoolean("sound", false) && resultCount == 0 &&
                        !newText.isNullOrBlank() && newText.length > (poemViewModel.searchQuery?.length ?: 0)) {
                    mediaPlayer = MediaPlayer.create(requireContext(), R.raw.not_found)?.apply {
                        setVolume(0.7f, 0.7f)
                        start()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(250,
                            VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        //deprecated in API 26
                        vibrator.vibrate(250)
                    }
                }

                poemViewModel.searchQuery = newText
                adapter!!.notifyDataSetChanged()
                searchPlate.isActivated = false

                if (newText.isNullOrEmpty() || resultCount == 0) {
                    binding.upButton.isEnabled = false
                    binding.downButton.isEnabled = false
                } else {
                    binding.upButton.isEnabled = true
                    binding.downButton.isEnabled = true
                }

                if (!newText.isNullOrEmpty()) {
                    if (resultCount != 0) {
                        poemViewModel.positionPointer = 0
                        smoothScroller!!.targetPosition = resultPositions[0]
                        binding.poemList.layoutManager?.startSmoothScroll(smoothScroller)
                    } else {
                        searchPlate.isActivated = true
                    }
                }

                binding.toolbarResultReport.text = when {
                    newText.isNullOrEmpty() -> ""
                    resultCount == 0 -> resources.getString(R.string.search_count, "۰", "۰")
                    else -> resources.getString(R.string.search_count, engNumToFarsiNum(poemViewModel.positionPointer+1),
                        engNumToFarsiNum(resultCount))
                }
                return true
            }
        })

        binding.downButton.setOnClickListener {
            poemViewModel.positionPointer++
            if (poemViewModel.positionPointer == resultPositions.size)
                poemViewModel.positionPointer = 0
            binding.toolbarResultReport.text = resources.getString(R.string.search_count,
                engNumToFarsiNum(poemViewModel.positionPointer+1), engNumToFarsiNum(resultCount))
            adapter!!.notifyDataSetChanged()

            smoothScroller!!.targetPosition = resultPositions[poemViewModel.positionPointer]
            binding.poemList.layoutManager?.startSmoothScroll(smoothScroller)
        }
        binding.upButton.setOnClickListener {
            poemViewModel.positionPointer--
            if (poemViewModel.positionPointer < 0)
                poemViewModel.positionPointer = resultPositions.size - 1
            binding.toolbarResultReport.text = resources.getString(R.string.search_count,
                engNumToFarsiNum(poemViewModel.positionPointer+1), engNumToFarsiNum(resultCount))
            adapter!!.notifyDataSetChanged()

            smoothScroller!!.targetPosition = resultPositions[poemViewModel.positionPointer]
            binding.poemList.layoutManager?.startSmoothScroll(smoothScroller)
        }

        searchText.textSize = 14f
        searchText.setTextColor(requireContext().getColorFromAttr(R.attr.colorOnSurface))
        searchPlate.apply{
            background = ResourcesCompat.getDrawable(resources, R.drawable.search_background, context.theme)
            backgroundTintMode = PorterDuff.Mode.MULTIPLY
            layoutParams.width = 0
            layoutParams.height = 36.dpTOpx(resources).toInt()
            pivotX = 0f
        }

        if (!mySearchView!!.isActivated)  poemViewModel.searchQuery = ""

        binding.bookMarkToggle.setOnClickListener{
            if ((it as ToggleButton).isChecked) {
                poemViewModel.updateBookmark(Calendar.getInstance().timeInMillis, poemItem.id)
                if (bookmarkViewModel.selectedBookmarkItem != null) bookmarkItemMeasure(true)
            } else {
                poemViewModel.updateBookmark(null, poemItem.id)
                if (bookmarkViewModel.selectedBookmarkItem != null) bookmarkItemMeasure(false)
            }
        }

        binding.poemToolbar.menu.findItem(R.id.help).isVisible =
            findNavController().currentDestination?.id == R.id.poemFragment

        binding.poemToolbar.setOnMenuItemClickListener { menuItem ->
            File(requireContext().filesDir, "poem").let {
                if (it.exists())
                    it.listFiles()?.forEach { file -> file.delete() }
            }
            when(menuItem.itemId){
                R.id.share -> {
                    ShareTypeChooseDialog().show(parentFragmentManager, "share_choose_type")
                }
                R.id.search -> {
//                    navController.navigate(NavGraphDirections.actionGlobalSearchFragment(poemItem.id, -1))
                }
                R.id.export -> {
                    PoemExportDialog().show(parentFragmentManager, "export_dialog")
                }
                R.id.setting -> {
                    PoemSettingDialog().show(parentFragmentManager, "poem_setting")
                }
                R.id.bookmark -> {
                    if (menuItem.title == resources.getString(R.string.bookmark_add_hint)) {
                        poemViewModel.updateBookmark(Calendar.getInstance().timeInMillis, poemItem.id)
                        menuItem.title = resources.getString(R.string.bookmark_remove_hint)
                        if (bookmarkViewModel.selectedBookmarkItem != null) bookmarkItemMeasure(true)
                    } else {
                        poemViewModel.updateBookmark(null, poemItem.id)
                        menuItem.title = resources.getString(R.string.bookmark_add_hint)
                        if (bookmarkViewModel.selectedBookmarkItem != null) bookmarkItemMeasure(false)
                    }
                }
                R.id.help -> {
                    poemViewModel.doShowHelp()
                }
            }
            true
        }

        startMargin = resources.getDimensionPixelSize(R.dimen.poem_item_start_margin)
        endMargin = resources.getDimensionPixelSize(R.dimen.poem_item_end_margin)
        guidWidth = resources.getDimensionPixelSize(R.dimen.poem_item_guid_width)
        verseSeparation = resources.getDimensionPixelSize(R.dimen.verse_separation)
        versePadding = resources.getDimensionPixelSize(R.dimen.verse_padding)
        commentHeight = resources.getDimension(R.dimen.comment_height).toInt()

        layoutManager = object : LinearLayoutManager(context, VERTICAL, false) {
            override fun onLayoutCompleted(state: State?) {
                super.onLayoutCompleted(state)
                state?.itemCount?.let {
                    if (it > 0 && !state.isMeasuring && !state.isPreLayout)
                        poemViewModel.poemListLayoutCompleted.value = true
                }
            }
        }
        binding.poemList.layoutManager = layoutManager

//        binding.poemList.layoutManager as LinearLayoutManager

        setBackgroundColor()
        imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (!searchViewModel.openedFromDetailFrag && ! favoriteViewModel.openedFromFavoriteDetailFrag)
            fullBookPage()

        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val actionBarSize = requireContext().getDimenFromAttr(R.attr.actionBarSize)
        val titleSubTitleOverlap = 16.dpTOpx(resources).toInt()
        val wholeTitleHeight = 2 * actionBarSize - titleSubTitleOverlap
        val collapseToolbarHeight =
            wholeTitleHeight + topPadding + resources.getDimensionPixelSize(R.dimen.book_content_height)

        (binding.poemToolbar.layoutParams as ViewGroup.MarginLayoutParams).let {
            it.topMargin = topPadding
            it.height = wholeTitleHeight
            it.bottomMargin = resources.getDimensionPixelSize(R.dimen.book_content_height) -
                    (it.topMargin + it.height)
        }

        (binding.collapseToolbar.layoutParams as ViewGroup.MarginLayoutParams).height =
            collapseToolbarHeight
        (binding.poemTitle.layoutParams as ViewGroup.MarginLayoutParams).height =
            resources.getDimensionPixelSize(R.dimen.book_content_height)
        (binding.toolbarTitle.layoutParams as ViewGroup.MarginLayoutParams).topMargin = topPadding
        (binding.toolbarSubtitle.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
            actionBarSize + topPadding - titleSubTitleOverlap
        (binding.resultDashboard.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
            actionBarSize + topPadding - titleSubTitleOverlap
        (binding.bookMarkToggle.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
            (wholeTitleHeight + topPadding - 3.dpTOpx(resources)).toInt()


        binding.item = Content(7, 0, " ", 1)
        binding.root.getLocationInWindow(fragLocation)

        val argumentsPoemPosition = arguments?.getInt(ARG_POEM_POSITION) ?: return
        poemItem = arguments?.getParcelable(ARG_POEM_ITEM) ?: return

//            poemItem = poemViewModel.poemList[getInt(ARG_POEM_POSITION)]
        binding.poemLayout.transitionName = poemViewModel.resultRowId1
            .getOrElse(argumentsPoemPosition) { poemItem.id }.toString()
        binding.poemTitle.text = poemItem.text

        poemViewModel.apply {

            if (!selectedVerses.containsKey(poemItem.id))
                selectedVerses[poemItem.id] = MutableLiveData<MutableList<Int>>()
            if (!noteOpenedVerses.containsKey(poemItem.id))
                noteOpenedVerses[poemItem.id] = mutableListOf()

            selectedVerses[poemItem.id]?.observe(viewLifecycleOwner){
//                    if (isResumed){
                    val nItems = it.size
                    it.forEach { id ->
                        binding.poemList.findViewHolderForItemId(id.toLong())?.itemView?.isActivated = true
                    }
                    mSelectedVerses.subtract(it).forEach { id ->
                        if (binding.poemList.findViewHolderForItemId(id.toLong())?.itemView != null)
                            binding.poemList.findViewHolderForItemId(id.toLong()).itemView.isActivated = false
                        else
                            adapter!!.notifyItemChanged(verseOrderToItemPosition(id))
                    }
                    mSelectedVerses = it.map { data -> data }
                    mainActivity = requireActivity() as? MainActivity

                    if (nItems == 0){
                        if (mainActivity?.poemActionMode != null)
                            finishActionMode()
                    } else {
                        if (mainActivity?.poemActionMode == null) {
                            mainActivity?.poemActionMode = mainActivity?.startSupportActionMode(callback)
                            binding.toolbarSubtitle.visibility = View.INVISIBLE
                        }
                        mainActivity?.poemActionMode?.title = nItems.toString()
                        mainActivity?.poemActionMode?.menu?.findItem(R.id.note)?.isEnabled = (nItems == 1)
                        mainActivity?.poemActionMode?.menu?.findItem(R.id.note)?.isVisible = (nItems == 1)
                    }
//                    }
            }

            binding.poemList.addOnItemTouchListener(object : OnItemTouchListener{
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    val itemView = rv.findChildViewUnder(e.x, e.y)
                    itemView?.let {
                        touchedViewID = rv.getChildItemId(it).toInt()
                        val commentRect = Rect(it.comment.left, it.comment.top, it.comment.right,
                            it.comment.bottom)
                        val margin = 10
                        val saveRect = Rect(it.save.left - margin, it.save.top - margin,
                            it.save.right + margin, it.save.bottom + margin)

                        val xPos = (e.x - it.x).toInt()
                        val yPos = (e.y - it.y).toInt()

                        return if (commentRect.contains(xPos, yPos) || saveRect.contains(xPos, yPos))
                            false
                        else
                            mDetector!!.onTouchEvent(e)
                    }
                    return false
                }

                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })

            binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener {
                    appBarLayout, verticalOffset ->
                if (textMenuTextView != null){
                    textMenuTextView?.getLocationInWindow(textMenuLocation)
                    textMenuLocation[0] -= fragLocation[0]
                    textMenuLocation[1] -= fragLocation[1]
                    doRefreshTextMenu()
                }
            })

            val allCats = allUpCategories(poemItem.parentID).map {
                    elem -> allCategory.find { it.id == elem } }.reversed()

            val field: Field = Toolbar::class.java.getDeclaredField("mTitleTextView")
            field.isAccessible = true
            (field.get(binding.poemToolbar) as TextView).doOnPreDraw {
                binding.toolbarTitle.updatePadding(
                    right = (view.width - it.right).coerceAtLeast(64.dpTOpx(resources).toInt())
                )
                binding.toolbarSubtitle.updatePadding(
                    right = (view.width - it.right).coerceAtLeast(64.dpTOpx(resources).toInt())
                )
            }

            binding.toolbarTitle.text = if (searchViewModel.openedFromDetailFrag)
                resources.getString(R.string.numbered_title,
                    engNumToFarsiNum(argumentsPoemPosition + 1),
                    allCats.getOrNull(0)?.text?.substringBefore("*") ?: "")
            else
                allCats.getOrNull(0)?.text?.substringBefore("*") ?: ""

            bookmarkAddress = ""
            allCats.getOrNull(0)?.text?.let { bookmarkAddress += "${it.substringBefore('*')}، " }

            if (allCats.size == 1){
                if (allCategory.count { it.parentID == allCats[0]?.id } == 0){ // if book count == 0
                    binding.toolbarSubtitle.text = "مجموعه آثار"
                    itemRoot = mutableListOf("مجموعه آثار", allCats[0]?.text ?: "")
                } else {
                    binding.toolbarSubtitle.text = "سایر آثار"
                    itemRoot = mutableListOf("سایر آثار", allCats[0]?.text ?: "")
                }
            } else if (allCats.size > 1) {
                binding.toolbarSubtitle.text = allCats.subList(1, allCats.size).map { it?.text }
                    .joinToString("، ") { it!! }
                itemRoot = allCats.reversed().map { it?.text }.toMutableList()

                allCats.subList(1, allCats.size).forEach { elem -> bookmarkAddress += "${elem?.text}، " }
            }
            bookmarkAddress += poemItem.text


            getPoemBookmark(poemItem.id).observe(viewLifecycleOwner) {
                binding.bookMarkToggle.isChecked = (it != null)
                binding.poemToolbar.menu.findItem(R.id.bookmark).title =
                    resources.getString(if (it != null) R.string.bookmark_remove_hint else R.string.bookmark_add_hint)
            }
        }

        viewHelper = PoemFastScrollViewHelper(binding.poemList,null, poemViewModel,
            this@PoemPagerFragment)
        FastScrollerBuilder(binding.poemList)/*.apply { disableScrollbarAutoHide() }*/
            .setPadding(0, 0, 0, 0)
            .setThumbDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_thumb, context?.theme)!!)
            .setTrackDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.fast_scroll_track, context?.theme)!!)
            .setViewHelper(viewHelper)
            .build()

        poemViewModel.getVerseWithPoemID(poemItem.id).observe(viewLifecycleOwner) { verseItems ->
            if (!verseItems.isNullOrEmpty()) {
                verses = verseItems.map { it.copy() }
                poemHilighter = PoemHilighter(verses, poemItem, poemViewModel, settingViewModel)

                mDetector = GestureDetectorCompat(
                    requireContext(),
                    PoemGestureListener(poemViewModel, this@PoemPagerFragment)
                )

                lifecycleScope.launch(Dispatchers.Default){
                    val modifiedVerseItems = MutableList<Verse?>(verseItems.size) { null }
                    var i = 0
                    while (i < verseItems.size - 1) {
                        when (verseItems[i].position) {
                            0 -> {
                                if (verseItems[i + 1].position == 1) {
                                    mesraWidth[i] =
                                        settingViewModel.paint.measureText(verseItems[i].text?.trim())
                                            .toInt()
                                    mesraWidth[i + 1] =
                                        settingViewModel.paint.measureText(verseItems[i + 1].text?.trim())
                                            .toInt()
                                    modifiedVerseItems[i] = verseItems[i].apply {
                                        text = text?.trim() + "¶" + verseItems[i + 1].text?.trim()
                                        hilight = (hilight ?: "") + "¶" + (verseItems[i + 1].hilight
                                            ?: "")
                                    }
                                    i++
                                } else
                                    modifiedVerseItems[i] = verseItems[i]
                            }
                            2 -> {
                                if (verseItems[i + 1].position == 3) {
                                    mesraWidth[i] =
                                        settingViewModel.paint.measureText(verseItems[i].text?.trim())
                                            .toInt()
                                    mesraWidth[i + 1] =
                                        settingViewModel.paint.measureText(verseItems[i + 1].text?.trim())
                                            .toInt()
                                    modifiedVerseItems[i] = verseItems[i].apply {
                                        text = text?.trim() + "¶" + verseItems[i + 1].text?.trim()
//                                    hilight = (hilight ?: "") + "¶" + (verseItems[i + 1].hilight ?: "")
                                    }
                                    i++
                                } else
                                    modifiedVerseItems[i] = verseItems[i]
                            }
                            else -> modifiedVerseItems[i] = verseItems[i]
                        }
                        i++
                    }

                    if (!(verseItems.last().position == 1 && verseItems[verseItems.size - 2].position == 0)
                        && !(verseItems.last().position == 3 && verseItems[verseItems.size - 2].position == 2)
                    )
                        modifiedVerseItems[i] = verseItems.last()


                    binding.poemList.post {
                        measurePoemStructureDimensions()
                        poemExporter = PoemExporter(
                            requireActivity(), verses, mesraWidth, poemItem.text, itemRoot, settingViewModel
                        )

                        val submittedList = modifiedVerseItems.filterNotNull()
//                            if (!this@PoemPagerFragment::itemViewHeights.isInitialized)
//                                itemViewHeights = IntArray(submittedList.size) { 0 }

                        binding.poemList.requestFocus()
                        adapter!!.submitList(submittedList)
                        parentFragment?.startPostponedEnterTransition()

                        if ((activity as? MainActivity)?.binding?.drawerLayout?.tag == "land_scape" &&
                            firstObservation
                        ) {
                            if (searchViewModel.openedFromDetailFrag &&
                                argumentsPoemPosition == searchViewModel.poemPosition
                            ) {
                                binding.poemList.doOnLayout { moveToSearchedPosition() }
                            } else if (favoriteViewModel.openedFromFavoriteDetailFrag &&
                                argumentsPoemPosition == favoriteViewModel.poemPosition
                            ) {
                                binding.poemList.doOnLayout { moveToFavoritePosition() }
                            }
                            firstObservation = false
                        }

                        if (submittedList.size < 20)
                            poemHeight(poemItem.id)
                    }
                }


                binding.root.post {
                    binding.poemList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
//                            totalScroll += dy
//                            if (submittedList.size >= 20) largePoemHeight(poemItem.id, totalScroll)
                            if (textMenuTextView != null) {
                                poemViewModel.apply {
                                    textMenuTextView?.getLocationInWindow(textMenuLocation)
                                    if (textMenuLocation[0] == 0) // textMenuTextView has gone out of window
                                        deselectText()
                                    textMenuLocation[0] -= fragLocation[0]
                                    textMenuLocation[1] -= fragLocation[1]
                                    doRefreshTextMenu()
                                }
                            }
                        }

                        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                            super.onScrollStateChanged(recyclerView, newState)
                            if (!recyclerView.canScrollVertically(1)) {
                                viewHelper!!.modifyScroll()
                            }
                            if (!recyclerView.canScrollVertically(-1)) {
                                viewHelper!!.apply {
                                    scroll = 0
                                    initialOffset = 0
                                }
                            }
                        }
                    })
                }
            }
        }


        binding.poemToolbar.navigationContentDescription = resources.getString(R.string.navigation_up)

//        ViewCompat.setWindowInsetsAnimationCallback(view,
//            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
//                var startBottom = 0f
//                var endBottom = 0f
//
//                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
//                    startBottom = binding.poemList.height.toFloat()
////                    if (binding.commentText.hasFocus())
//                        Timber.i("first bottom = ${startBottom}")
//                }
//
//                override fun onStart(animation: WindowInsetsAnimationCompat,
//                                     bounds: WindowInsetsAnimationCompat.BoundsCompat):
//                        WindowInsetsAnimationCompat.BoundsCompat {
//                    endBottom = binding.poemList.height.toFloat()
////                    if (binding.commentText.hasFocus())
//                        Timber.i("start bottom = ${endBottom}")
//
//                    return bounds
//                }
//
//                override fun onProgress(
//                    insets: WindowInsetsCompat,
//                    runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
//                    val imeAnimation = runningAnimations.find {
//                        it.typeMask and WindowInsetsCompat.Type.ime() != 0
//                    } ?: return insets
//
//                    // Offset the view based on the interpolated fraction of the IME animation.
////                    view.translationY =
////                        (startBottom - endBottom) * (1 - imeAnimation.interpolatedFraction)
//
//                    return insets
//                }
//            }
//        )

    }

    override fun onStop() {
        val mEditText = binding.poemList.findFocus()
        if (mEditText is EditText){
            val note = mEditText.text.toString()
            val vOrder = binding.poemList.findContainingViewHolder(mEditText)?.itemId
            if (vOrder != null) {
                poemViewModel.updateNote(
                    note.ifEmpty { null },
                    poemItem.id, vOrder.toInt())
            }
        }
        finishActionMode()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        imm = null
        mDetector = null
        smoothScroller = null
        searchMenuItem = null
        textMenuTextView = null
        viewHelper = null
        layoutManager = null
        adapter = null
        mySearchView = null

        _binding = null
    }

    fun fullBookPage(){
        binding.appBar.setExpanded(false)
    }

    fun finishActionMode(){
        mainActivity?.poemActionMode?.finish()
        mainActivity?.poemActionMode = null
        if (searchMenuItem?.isActionViewExpanded != true)
            binding.toolbarSubtitle.visibility = View.VISIBLE
    }

    fun addNote(verseOrder: Int){
        poemViewModel.commentTextFocused = true
        poemViewModel.noteOpenedVerses[poemItem.id]?.let{
            if (!it.contains(verseOrder)) {
                openNote(verseOrder)
            } else {
                binding.poemList.findViewHolderForItemId(verseOrder.toLong())?.itemView
                    ?.comment_text?.apply {
                        requestFocus()
                        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
//                        (activity as MainActivity).showKeyBoard()
                    }
            }
        }
    }

    fun openNote(viewId: Int){
        val mItemView = binding.poemList.findViewHolderForItemId(viewId.toLong())?.itemView ?: return
        val mComment = mItemView.comment
        poemViewModel.noteOpenedVerses[poemItem.id]?.add(viewId)

//        itemViewHeights[verseOrderToItemPosition(viewId)] += (commentHeight + 8.dpTOpx(resources).toInt())

        mItemView.apply {
            mComment.visibility = View.VISIBLE
            val params = mComment.layoutParams

            ValueAnimator.ofFloat(0f, 1f).apply {
                addUpdateListener { updatedAnimation ->
                    params.height =
                        ((updatedAnimation.animatedValue as Float) * commentHeight).toInt()
                    mComment.layoutParams = params
                }
                doOnEnd {
                    if (poemViewModel.commentTextFocused) {
                        comment_text.requestFocus()
                        imm?.showSoftInput(comment_text, InputMethodManager.SHOW_IMPLICIT)
//                        (activity as MainActivity).showKeyBoard()
                    }
                    poemViewModel.poemContentHeight[poemItem.id] =
                        (poemViewModel.poemContentHeight[poemItem.id] ?: 0) + commentHeight +
                                8.dpTOpx(resources).toInt()
                }
            }.start()
        }
    }

    fun closeNote(viewId: Int){
        poemViewModel.noteOpenedVerses[poemItem.id]?.remove(viewId)
        val mItemView = binding.poemList.findViewHolderForItemId(viewId.toLong())?.itemView ?: return
        val mComment = mItemView.comment

        if (mItemView.save.isVisible)
            saveComment(mItemView.comment_text.text.toString(), viewId, mItemView.save)

//        itemViewHeights[verseOrderToItemPosition(viewId)] -= (commentHeight + 8.dpTOpx(resources).toInt())

        mItemView.apply {
            val params = mComment.layoutParams

            ValueAnimator.ofFloat(1f, 0f).apply {
                addUpdateListener { updatedAnimation ->
                    params.height =
                        ((updatedAnimation.animatedValue as Float) * commentHeight).toInt()
                    mComment.layoutParams = params
                }
                doOnEnd {
                    mComment.visibility = View.GONE
                    poemViewModel.poemContentHeight[poemItem.id] =
                        (poemViewModel.poemContentHeight[poemItem.id] ?: 0) - commentHeight -
                                8.dpTOpx(resources).toInt()

                    if (poemViewModel.keyboardIsOpen)
//                        (activity as MainActivity).hideKeyBoard()
                        imm?.hideSoftInputFromWindow(windowToken, 0)
                }
            }.start()
        }
    }

    fun saveComment(note: String?, verseOrder: Int, saveButton: Button){
        poemViewModel.updateNote(if (note.isNullOrEmpty()) null else note, poemItem.id, verseOrder)
        saveButton.isVisible = false
    }

    fun refreshContent(){
        refreshMesraWidth()
        binding.poemList.adapter = adapter
        lastItem = 0
        viewHelper!!.apply {
            scroll = 0
            initialOffset = 0
        }
        binding.poemTitle.invalidate()
    }

    private fun refreshMesraWidth(){
        var i = 0
        while (i < verses.size - 1) {
            when (verses[i].position) {
                0 -> {
                    if (verses[i + 1].position == 1) {
                        mesraWidth[i] = settingViewModel.paint.measureText(verses[i].text?.trim()).toInt()
                        mesraWidth[i + 1] = settingViewModel.paint.measureText(verses[i + 1].text?.trim()).toInt()
                        i++
                    }
                }
                2 -> {
                    if (verses[i + 1].position == 3) {
                        mesraWidth[i] = settingViewModel.paint.measureText(verses[i].text?.trim()).toInt()
                        mesraWidth[i + 1] = settingViewModel.paint.measureText(verses[i + 1].text?.trim()).toInt()
                        i++
                    }
                }
            }
            i++
        }
        measurePoemStructureDimensions()
    }

    private fun measurePoemStructureDimensions(){
        val mesraContainerWidth = (binding.root.measuredWidth
                - (startMargin + endMargin + guidWidth + verseSeparation)) / 2

        mesraMaxWidth = (mesraWidth.values.maxOrNull() ?: 0).coerceAtMost(
            85 *(binding.root.measuredWidth - (startMargin + endMargin + guidWidth))/100
        )
        beitOneLine = mesraContainerWidth > mesraMaxWidth + versePadding + 4
        mesraOverlapWidth = max(
            mesraMaxWidth / 2 + 2 * versePadding,
            2 * (mesraMaxWidth + 2 * versePadding) - (binding.root.measuredWidth
                    - (startMargin + endMargin + guidWidth))
        )
        paragraphWidth = binding.root.measuredWidth -
                (startMargin + endMargin + 2 * versePadding + endMargin)
    }

    private fun searchResultCount(query: String?): Int {
        var count = 0
        var position = -1

        resultPositions.clear()
        poemViewModel.resultVersePositions.clear()
        var num = 0

        for (i in verses.indices){
            if (intArrayOf(-1, 0, 2, 4).contains(verses[i].position ?: -2))
                position++

            val inc = (verses[i].text?.findSpanIndex3(query)?.size ?: 0) /2
            count += inc
            if (inc != 0){
                resultPositions.addAll(List(inc){position})
                if (verses[i].position == 2)
                    num = inc

                val list =
                    if (verses[i].position == 3) List(inc){ j -> intArrayOf(verses[i-1].verseOrder, num+j)}
                    else List(inc){ j -> intArrayOf(verses[i].verseOrder, j)}
                poemViewModel.resultVersePositions.addAll(list)
            }
        }
        return count
    }

    private fun verseOrderToItemPosition(vOrder: Int): Int {
        var position = -1
        if (vOrder <= verses.size)
            for (i in 0 until vOrder){
                if (intArrayOf(-1, 0, 2, 4).contains(verses[i].position ?: -20))
                    position++
            }

        return position
    }

    fun moveToSearchedPosition(){
        val resultItem = searchViewModel.searchResultList[searchViewModel.poemPosition]
        Handler(Looper.getMainLooper()).postDelayed({
            smoothScroller!!.targetPosition = verseOrderToItemPosition(resultItem.verseOrder)
            if (smoothScroller!!.targetPosition != -1)
                binding.poemList.layoutManager?.startSmoothScroll(smoothScroller)
        }, 500)
        binding.poemList.postDelayed({adapter!!.notifyDataSetChanged()}, 600)
    }

    fun moveToFavoritePosition(){
        val favoriteItem = favoriteViewModel.allFavorites[favoriteViewModel.poemPosition]
        Handler(Looper.getMainLooper()).postDelayed({
            smoothScroller!!.targetPosition = verseOrderToItemPosition(favoriteItem.verse1Order)
            if (smoothScroller!!.targetPosition != -1)
                binding.poemList.layoutManager?.startSmoothScroll(smoothScroller)
        }, 500)
        binding.poemList.postDelayed({adapter!!.notifyDataSetChanged()}, 600)
    }

    private fun poemHeight(poemID: Int){
        binding.poemList.apply {
            measure(
                View.MeasureSpec.makeMeasureSpec(binding.poemList.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            poemViewModel.poemContentHeight[poemID] = measuredHeight - paddingTop - paddingBottom
        }
    }

    fun largePoemHeight(poemID: Int, totalScroll: Int){
        val newLastItem = layoutManager!!.findLastVisibleItemPosition()
        if (lastItem < newLastItem){
            lastItem = newLastItem
            firstItem = layoutManager!!.findFirstVisibleItemPosition()

            val displayedItemsCount = lastItem - firstItem
            val rect = Rect()

            binding.poemList.getDecoratedBoundsWithMargins(
                binding.poemList.getChildAt(displayedItemsCount), rect
            )
            poemViewModel.poemContentHeight[poemID] =
                ((rect.bottom + totalScroll)*adapter!!.itemCount/(lastItem+1f)).toInt()
        }
    }

    fun resultItemMeasure(){
        searchViewModel.apply {
            val resultItem = searchResultList[poemPosition]
            binding.resultSample.number.text = "${engNumToFarsiNum(poemPosition+1)}."
            binding.resultSample.address.text = resultAddress(resultItem)
            when(resultItem.position){
                0, 1 -> {
                    val mesra1Width = settingViewModel.paint.measureText(resultItem.majorText).toInt()
                    val mesra2Width = settingViewModel.paint.measureText(resultItem.minorText).toInt()
                    val mesraMaxWidth = maxOf(mesra1Width, mesra2Width)
                    val beitOneLine = mesraContainerWidth > mesraMaxWidth + versePadding + 4
                    if (!beitOneLine){
                        val params = binding.resultSample.mesra2Text.layoutParams as ConstraintLayout.LayoutParams
                        params.updateMargins(top = (settingViewModel.textHeight * settingViewModel.verseVertSep).toInt())
                        binding.resultSample.mesra2Text.layoutParams = params
                    }
                }
                2, 3 -> binding.resultSample.paragText.text =
                    requireContext().getString(R.string.beit2, resultItem.majorText, resultItem.minorText)

                else -> {
                    val splittedQuery =
                        makeTextBiErab(submittedSearch.filter { it.isLetterOrDigit() || it.isWhitespace()})
                            .splitToSequence(' ').toList()
                    binding.resultSample.paragText.text =
                    requireContext().getString(R.string.tabbedText, resultItem.majorText?.trim() ?: "")
                        .shorten(splittedQuery, 200)
                }
            }
        }

        binding.resultSample.searchResult.post {
            if (searchViewModel.poemPosition >= searchViewModel.lastResultOpened) {
                searchViewModel.apply {
                    bottomViewedResultHeight = binding.resultSample.searchResult.measuredHeight
                    resultListDisplace += (bottomViewedResultHeight + topViewedResultHeight)
                    topViewedResultHeight = 0
                    lastResultOpened = poemPosition
                }
            } else {
                searchViewModel.apply {
                    topViewedResultHeight = binding.resultSample.searchResult.measuredHeight
                    resultListDisplace -= (topViewedResultHeight + bottomViewedResultHeight)
                    bottomViewedResultHeight = 0
                    lastResultOpened = poemPosition
                }
            }
        }
    }

    private fun bookmarkItemMeasure(addOrRemove: Boolean){
        val item = bookmarkViewModel.allBookmarks.find { it.poemm.id == poemItem.id }

        bookmarkViewModel.apply {
            binding.resultSample.searchResult.updateLayoutParams {
                width = rootWidth
            }
            binding.resultSample.number.text = if (item == null) "${engNumToFarsiNum(bookmarkCount+1)}."
            else "${engNumToFarsiNum(allBookmarks.indexOf(item)+1)}."
            binding.resultSample.address.text = bookmarkAddress

            when(verses[0].position){
                0, 1 -> {
                    val mesra1Width = settingViewModel.paint.measureText(verses[0].text).toInt()
                    val mesra2Width = settingViewModel.paint.measureText(verses[1].text).toInt()
                    val mesraMaxWidth = maxOf(mesra1Width, mesra2Width)
                    val beitOneLine = mesraContainerWidth > mesraMaxWidth + versePadding + 4
                    if (!beitOneLine){
                        val params = binding.resultSample.mesra2Text.layoutParams as ConstraintLayout.LayoutParams
                        params.updateMargins(top = (settingViewModel.textHeight * settingViewModel.verseVertSep).toInt())
                        binding.resultSample.mesra2Text.layoutParams = params
                    }
                }
                2, 3 -> binding.resultSample.paragText.text =
                    requireContext().getString(R.string.beit2, verses[0].text, verses[1].text)

                else -> binding.resultSample.paragText.text = if (verses[0].text!!.length <= 200) verses[0].text else
                    verses[0].text!!.substring(0, 200).substringBeforeLast(' ') + " ..."
            }
        }

        binding.resultSample.searchResult.post {
            bookmarkViewModel.apply {
                bookmarkListAddedScroll = binding.resultSample.searchResult.measuredHeight
                when {
                    addOrRemove -> { // The item is being added
                        bookmarkListDisplace += bookmarkListAddedScroll
                    }
                    allBookmarks.indexOf(item) < allBookmarks.indexOf(selectedBookmarkItem) -> {
                        bookmarkListDisplace -= bookmarkListAddedScroll
                        bookmarkListAddedScroll *= -1
                    }
//                    else ->
//                        bookmarkListDisplace += binding.resultSample.searchResult.measuredHeight
                }
            }
        }
    }

    fun favoriteItemMeasure(favoriteItem: FavoriteContent, favoriteAdded: Boolean){
        favoriteViewModel.apply {
//            val favoriteItem = allFavorites[poemPosition]
            binding.resultSample.searchResult.updateLayoutParams {
                width = LayoutParams.MATCH_PARENT
            }
            binding.resultSample.number.text = "${engNumToFarsiNum(poemPosition+1)}."
            binding.resultSample.address.text = bookmarkAddress

            when(favoriteItem.verse1Position){
                0, 1 -> {
                    binding.resultSample.paragText.isVisible = false
                    binding.resultSample.mesra1Text.isVisible = true
                    binding.resultSample.mesra2Text.isVisible = true
                    val mesra1Width = settingViewModel.paint.measureText(favoriteItem.verse1Text).toInt()
                    val mesra2Width = settingViewModel.paint.measureText(favoriteItem.verse2Text).toInt()
                    val mesraMaxWidth = maxOf(mesra1Width, mesra2Width)
                    val beitOneLine = mesraContainerWidth > mesraMaxWidth + versePadding + 4
                    if (!beitOneLine){
                        val params = binding.resultSample.mesra2Text.layoutParams as ConstraintLayout.LayoutParams
                        params.updateMargins(top = (settingViewModel.textHeight * settingViewModel.verseVertSep).toInt())
                        binding.resultSample.mesra2Text.layoutParams = params
                    }
                }
                2, 3 -> {
                    binding.resultSample.paragText.isVisible = true
                    binding.resultSample.mesra1Text.isVisible = false
                    binding.resultSample.mesra2Text.isVisible = false
                    binding.resultSample.paragText.text = requireContext().getString(R.string.beit2,
                            favoriteItem.verse1Text, favoriteItem.verse2Text)
                }
                else -> {
                    binding.resultSample.paragText.isVisible = true
                    binding.resultSample.mesra1Text.isVisible = false
                    binding.resultSample.mesra2Text.isVisible = false
                    binding.resultSample.paragText.text =
                        if (favoriteItem.verse1Text!!.length <= 200) favoriteItem.verse1Text else
                            favoriteItem.verse1Text.substring(0, 200).substringBeforeLast(' ') + " ..."
                }
            }
        }

        binding.resultSample.searchResult.doOnLayout {
            val mHeight = binding.resultSample.searchResult.measuredHeight
            favoriteViewModel.apply {
                when {
                    poemPosition >= lastResultOpened -> {
                        if (favoriteAdded)
                            favoriteListAddedScroll += mHeight
                        else {
                            bottomViewedResultHeight = mHeight
                            favoriteListDisplace += (bottomViewedResultHeight + topViewedResultHeight)
                            topViewedResultHeight = 0
                        }
                    }
                    else -> {
                        if (favoriteAdded)
                            favoriteListAddedScroll -= mHeight
                        else {
                            topViewedResultHeight = mHeight
                            favoriteListDisplace -= (topViewedResultHeight + bottomViewedResultHeight)
                            bottomViewedResultHeight = 0
                        }
                    }
                }
                poemPosition = poemPosition.coerceAtLeast(0)
                lastResultOpened = poemPosition
            }
        }
    }

    fun applyFavorite(){
        val favoriteList = verses.filter { mSelectedVerses.contains(it.verseOrder)}.map { it.favorite }

        if (parentFragment is FavoriteDetailFragment){
            favoriteViewModel.apply {
                if (favoriteList.contains(null)) {
                    val favoriteContent = mSelectedVerses.map { it }
                        .filter { verses[it - 1].favorite == null }.map {
                            FavoriteContent(verses[it - 1].text, verses[it - 1].position,
                                verses[it - 1].verseOrder, verses.getOrNull(it)?.text ?: "", Poem())
                        }

                    allFavorites.addAll(0, favoriteContent)
                    poemList.addAll(0, List(favoriteContent.size){poemItem})
                    poemCount = allFavorites.size
                    poemPosition += favoriteContent.size

                    favoriteContent.forEachIndexed { index, fc ->
                        Handler(Looper.getMainLooper()).postDelayed(
                            { favoriteItemMeasure(fc, true) } , 30L * index)
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        poemViewModel.updateFavorite(Calendar.getInstance().timeInMillis, poemItem.id,
                            mSelectedVerses.filter { verses[it - 1].favorite == null }
                                .map { it.toString() }.toList())
                        favoriteViewModel.comeFromFavoriteFragment = false
                        (parentFragment as FavoriteDetailFragment).setViewPagerItem()
                    } , 30L * favoriteContent.size)

                } else {
                    val favoriteContent = mSelectedVerses.filter { verses[it - 1].favorite!! >=
                                verses[allFavorites[poemPosition].verse1Order - 1].favorite!! }
                        .map {
                            FavoriteContent(verses[it - 1].text, verses[it - 1].position,
                                verses[it - 1].verseOrder, verses.getOrNull(it)?.text ?: "", Poem())
                        }

                    val selectedVerseIndices = verses.asSequence().filter { it.favorite != null }
                        .sortedByDescending { it.favorite }.map { it.verseOrder }.mapIndexed { index, vOrder ->
                            if (mSelectedVerses.contains(vOrder)) index else -1 }.filter { it != -1 }

                    val toBeRemovedIndices = poemList.mapIndexed { index, content ->
                        if (content == poemItem) index else -1 }.filterNot { it == -1 }
                        .filterIndexed { index, _ -> selectedVerseIndices.contains(index) }

                    val updatedPoemList = MutableList(poemList.size - toBeRemovedIndices.size) {Content()}
                    var i = 0
                    var j = 0
                    while (i < poemList.size){
                        if (!toBeRemovedIndices.contains(i)){
                            updatedPoemList[j] = poemList[i]
                            j++
                        } else
                            allFavorites.removeAt(j)

                        i++
                    }
                    poemList = updatedPoemList
                    poemCount = poemList.size
                    poemPosition = (poemPosition - favoriteContent.size)/*.coerceIn(0, poemList.size-1)*/

                    favoriteContent.forEachIndexed { index, fc ->
                        Handler(Looper.getMainLooper()).postDelayed(
                            { favoriteItemMeasure(fc, true) } , 30L * index)
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
//                        finishActionMode()
                        poemViewModel.updateFavorite(null, poemItem.id,
                            mSelectedVerses.map { it.toString() }.toList())
                        favoriteViewModel.comeFromFavoriteFragment = false
                        (parentFragment as FavoriteDetailFragment).setViewPagerItem()
                    } , 30L * favoriteContent.size)

                }
            }
        } else
            poemViewModel.updateFavorite(
                if (favoriteList.contains(null)) Calendar.getInstance().timeInMillis else null,
                poemItem.id, mSelectedVerses.map { it.toString() }.toList())
    }

    private fun setBackgroundColor(){
        settingViewModel.paperColorPref.observe(viewLifecycleOwner){
            settingViewModel.currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (settingViewModel.currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                binding.poemLayout.setBackgroundColor(settingViewModel.paperColor)
                binding.appBar.setBackgroundColor(settingViewModel.paperColor)
            }
        }

        settingViewModel.brightness.observe(viewLifecycleOwner){
            binding.darkener.alpha = DARK_ALPHA_MAX * (1 - it / 100)
        }
    }

    fun setTextMenuParams(view: TextView){
        textMenuTextView = view
        poemViewModel.apply {
            textMenuText = view.text.toString()
            textMenuStart = (view as TextSelectableView).selStart
            textMenuEnd = view.selEnd
            view.getLocationInWindow(textMenuLocation)
            textMenuLocation[0] -= fragLocation[0]
            textMenuLocation[1] -= fragLocation[1]
        }
    }

    fun deselectText(){
        (textMenuTextView as? TextSelectableView)?.hasSel = false
        textMenuTextView?.invalidate()
        poemViewModel.apply {
            textMenuVisible = false
            textVerseOrder = 0
            textNoteVerseOrder = 0
            doRefreshTextMenu()
        }
    }

    fun firstVerseRectangle(): Rect {
        val rect = Rect()
        val pos = IntArray(2)
        val firstItem = layoutManager!!.findFirstCompletelyVisibleItemPosition()
        val firstItemView = binding.poemList.findViewHolderForAdapterPosition(firstItem)?.itemView
        if (firstItemView != null) {
            firstItemView.getLocationInWindow(pos)
            binding.poemList.getDecoratedBoundsWithMargins(firstItemView, rect)
            rect.set(pos[0] + firstItemView.marginLeft, pos[1] + firstItemView.marginTop,
                pos[0] + rect.width() - 3*firstItemView.marginLeft,
                pos[1] + rect.height() - firstItemView.marginBottom - firstItemView.marginTop)
        } else {
            rect.set(16.dpTOpx(resources).toInt(),
                (binding.poemList.top + 16.dpTOpx(resources)).toInt(),
                (binding.root.width - 16.dpTOpx(resources)).toInt(),
                (binding.poemList.top + 120.dpTOpx(resources)).toInt()
            )
        }
        return rect
    }

    fun onShareDialogPositiveClick() {
        if (poemViewModel.shareOutFiles.contains(true)) {
//            val poemExporter = PoemExporter(
//                requireActivity(), verses, mesraWidth, poemItem.text, itemRoot, settingViewModel
//            )
            val reference =
//                if (itemRoot.size == 2) "${itemRoot[0]} ${itemRoot[1]?.substringBefore("*")}"
//                else "${itemRoot[1]} ${itemRoot[2]?.substringBefore("*")}، ${itemRoot[0]}"
                itemRoot.reversed().joinToString("، ") { it?.substringBefore('*') ?: "" }


            var fileSuccess = false
            var filePath = ""
            val filesUri = ArrayList<Uri?>()

            for (i in 0 .. 1) {
                if (poemViewModel.shareOutFiles[i]) {
                    when (i) {
                        0 -> {
                            fileSuccess = poemExporter?.exportToPDF(false) == true
                            filePath = "/poem/${poemItem.text}.pdf"
                        }
                        1 -> {
                            fileSuccess = poemExporter?.exportToJPG(false) == true
                            filePath = "/poem/${poemItem.text}.jpg"
                        }
                    }
                    if (fileSuccess) {
                        val attachFile = File(context?.filesDir, filePath)
                        filesUri.add(
                            try {
                                FileProvider.getUriForFile(requireContext(),
                                    "com.takaapoo.com.fileprovider",
                                    attachFile
                                )
                            } catch (e: IllegalArgumentException) {
                                Timber.e("The selected file can't be shared: $attachFile")
                                null
                            }
                        )
                    }
                }
            }
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(
                    Intent.EXTRA_SUBJECT, getString(
                        R.string.email_subject_2,
                        poemItem.text,
                        reference
                    )
                )

            if (poemViewModel.shareOutFiles[2]) {
                var sher = ""
                verses.forEach { verse ->
                    sher += if (verse.verseOrder == 1) verse.text
                    else {
                        if (verse.position == 0 || verse.position == 2) "\n \n ${verse.text}"
                        else "\n ${verse.text}"
                    }
                }
                sher += "\n \n \n ${getString(R.string.email_subject_2, poemItem.text, reference)}"
                sher += "\n ${getString(R.string.produced)}"
                shareIntent.putExtra(Intent.EXTRA_TEXT, sher)
            }
            shareIntent
                .setType(if (filesUri.isNotEmpty() && filesUri[0] != null)
                    context?.contentResolver?.getType(filesUri[0]!!) else "*/*")
                .putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList(filesUri.filterNotNull())
                )
                .putExtra(
                    Intent.EXTRA_MIME_TYPES, arrayListOf(
                        "text/plain",
                        "image/*, application/pdf"
                    )
                )

            startActivity(Intent.createChooser(shareIntent, "share via"))
        }
    }

    fun onExportDialogPositiveClick() {
        when (poemViewModel.exportOutFile) {
            0 -> poemExporter?.exportToPDF(true)
            1 -> poemExporter?.exportToJPG(true)
            2 -> poemExporter?.exportToText(true)
        }
    }

    fun firebaseLog(){
        if (::poemItem.isInitialized){
            (activity as? MainActivity)?.analyticsLogEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, "Poem: ${poemItem.text}")
                }
            )
            Firebase.crashlytics.setCustomKey(
                "Enter Screen",
                "Poet = ${binding.toolbarTitle.text} , Book = ${binding.toolbarSubtitle.text} " +
                        ", Poem = ${poemItem.text}"
            )
        }
    }

}