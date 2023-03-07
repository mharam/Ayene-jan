package com.takaapoo.adab_parsi.poem

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.bookmark.BookmarkDetailFragment
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.databinding.FragmentPoemBinding
import com.takaapoo.adab_parsi.poem.dictionary.WordMeaningDialog
import com.takaapoo.adab_parsi.poem.touch_handler.PoemGestureListener
import com.takaapoo.adab_parsi.util.custom_views.TextSelectableView
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.makeTextBiErab
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*


const val DARK_ALPHA_MAX = 0.35f

@AndroidEntryPoint
abstract class BasePoemFragment: Fragment() {

    private var _binding: FragmentPoemBinding? = null
    val binding get() = _binding!!
    val poemViewModel: PoemViewModel by activityViewModels()

    private var _viewPager: ViewPager2? = null
    val viewPager get() = _viewPager!!
    private var firstFragEntrance = false
    val help: Help by lazy { Help(this) }

    var start = 0
    var end = 0
    var initialTouchX = 0f
    var initialTouchY = 0f

    var visibleChildFrag: PoemPagerFragment? = null
    private var onPauseCalled = false
    private val hideSystemBarsRunnable = Runnable { (activity as MainActivity).hideSystemBars() }
//    private val windowHeightMethod =
//        InputMethodManager::class.java.getMethod("getInputMethodWindowVisibleHeight")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentPoemBinding.inflate(inflater, container, false)
        _viewPager = binding.bookViewPager

        binding.poemViewModel = poemViewModel
        poemViewModel.gestureDetector = GestureDetectorCompat(
            requireActivity().applicationContext,
            PoemGestureListener(poemViewModel)
        )
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setTooltip()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                poemViewModel.uiEvent.collect { event ->
                    poemUiEventHandler(event)
                }
            }
        }

        binding.rightHandle.setOnTouchListener { _, event ->
            poemViewModel.reportEvent(PoemEvent.OnRightHandleMove(event))
            true
        }
        binding.leftHandle.setOnTouchListener { _, event ->
            poemViewModel.reportEvent(PoemEvent.OnLeftHandleMove(event))
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val keyBoardIsOpen = windowInsets.isVisible(WindowInsetsCompat.Type.ime())

            val focusOwnerView = binding.root.findFocus()
            val itemView = focusOwnerView?.parent?.parent as? ViewGroup
            if (!keyBoardIsOpen && focusOwnerView?.id == R.id.comment_text &&
                itemView?.findViewById<Button>(R.id.save)?.isVisible == true){
                val note = (focusOwnerView as EditText).text.toString()
                visibleChildFrag?.run {
                    saveComment(
                        note = note,
                        verseOrder = binding.poemList.getChildItemId(itemView).toInt(),
                        saveButton = itemView.findViewById(R.id.save)
                    )
                }
            }

            val deviceIsPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            if ((this is PoemFragment || this is BookmarkDetailFragment) && deviceIsPortrait) {
                // Should be used for API level below 29 because windowInsets.isVisible() is approximate.
                val navBarVisible =
                    requireActivity().window?.decorView?.systemUiVisibility?.and(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0

                if (keyBoardIsOpen)
                    (activity as MainActivity).showSystemBars()
                else {
                    if (poemViewModel.keyboardIsOpen) {
                        (activity as MainActivity).hideSystemBars()
                    } else if ((windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars()) || navBarVisible)
                        && !onPauseCalled) {
                        view.postDelayed( hideSystemBarsRunnable , 3500 )
                    }
                }
                poemViewModel.keyboardIsOpen = keyBoardIsOpen
            }
            windowInsets
        }

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        firstFragEntrance = preferenceManager.getBoolean("poemFragFirstEnter", true)
        if (firstFragEntrance) {
            poemViewModel.reportEvent(PoemEvent.OnShowHelp(PoemHelpState.PAGING))
            preferenceManager.edit().putBoolean("poemFragFirstEnter", false).apply()
        }

//        barsPreparation()
    }

    override fun onResume() {
        super.onResume()
        onPauseCalled = false
    }

    override fun onPause() {
        super.onPause()
        view?.removeCallbacks(hideSystemBarsRunnable)
        onPauseCalled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, null)
        }
        poemViewModel.gestureDetector = null
        viewPager.adapter = null
        visibleChildFrag = null
        _viewPager = null

        _binding = null
    }

    private fun poemUiEventHandler(event: PoemEvent){
        when(event){
            is PoemEvent.OnDoubleTap -> {
                visibleChildFrag?.let { poemPagerFragment ->
                    val tempList = poemViewModel
                        .selectedVerses[poemPagerFragment.poemItem.id]?.value ?: mutableListOf()
                    if (!tempList.remove(poemViewModel.touchedViewID))
                        tempList.add(poemViewModel.touchedViewID)

                    poemViewModel.selectedVerses[poemPagerFragment.poemItem.id]?.value = tempList
                    poemPagerFragment.deselectText()
                }
            }
            is PoemEvent.OnSingleTap -> {
                visibleChildFrag?.let { poemPagerFragment ->
                    poemPagerFragment.deselectText()
                    val tempList = poemViewModel.selectedVerses[poemPagerFragment.poemItem.id]?.value
                        ?: mutableListOf()
                    if (tempList.isNotEmpty()){
                        if (!tempList.remove(poemViewModel.touchedViewID))
                            tempList.add(poemViewModel.touchedViewID)

                        poemViewModel.selectedVerses[poemPagerFragment.poemItem.id]?.value = tempList
                    } else {
                        val hasNote = !poemPagerFragment.verses
                            .find { it.verseOrder == poemViewModel.touchedViewID }?.note.isNullOrEmpty()
                        val isNoteOpen = poemViewModel.noteOpenedVerses[poemPagerFragment.poemItem.id]
                            ?.contains(poemViewModel.touchedViewID) == true

                        if (isNoteOpen){
                            poemPagerFragment.closeNote(poemViewModel.touchedViewID)
                        } else if (hasNote) {
                            poemPagerFragment.openNote(poemViewModel.touchedViewID)
                            poemViewModel.commentTextFocused = false
                        }
                    }
                }
            }
            is PoemEvent.TextMenu -> {
                when(event){
                    is PoemEvent.TextMenu.OpenDictionary -> {
                        childFragmentManager.let { WordMeaningDialog().show(it, "word_meaning") }
                    }
                    is PoemEvent.TextMenu.Copy -> {
                        val clipboard =
                            context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val copiedText =
                            poemViewModel.textMenuText
                                ?.subSequence(poemViewModel.textMenuStart, poemViewModel.textMenuEnd)
                                .toString()
                                .replace("ـ", "")
                                .replace("\\s+".toRegex(), " ")
                                .trim()

                        val clip = ClipData.newPlainText("sher", copiedText)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.copied, Toast.LENGTH_LONG).show()
                    }
                    is PoemEvent.TextMenu.AddNote -> {
                        visibleChildFrag?.addNote(verseOrder = poemViewModel.textNoteVerseOrder)
                    }
                    is PoemEvent.TextMenu.Marker1 -> {
                        visibleChildFrag?.poemHilighter?.hilight(
                            verseOrder = poemViewModel.textVerseOrder,
                            start = start,
                            end = end,
                            hilightColorPref = 0
                        )
                    }
                    is PoemEvent.TextMenu.Marker2 -> {
                        visibleChildFrag?.poemHilighter?.hilight(
                            verseOrder = poemViewModel.textVerseOrder,
                            start = start,
                            end = end,
                            hilightColorPref = 1
                        )
                    }
                    is PoemEvent.TextMenu.Marker3 -> {
                        visibleChildFrag?.poemHilighter?.hilight(
                            verseOrder = poemViewModel.textVerseOrder,
                            start = start,
                            end = end,
                            hilightColorPref = 2
                        )
                    }
                    is PoemEvent.TextMenu.Eraser -> {
                        visibleChildFrag?.poemHilighter?.eraseHilight(
                            verseOrder = poemViewModel.textVerseOrder,
                            start = start,
                            end = end
                        )
                    }
                    else -> {}
                }
                visibleChildFrag?.deselectText()
            }
            is PoemEvent.OnMenuItemClicked -> {
                when(event.menuItem.itemId){
                    R.id.share -> {
                        ShareTypeChooseDialog().show(childFragmentManager, "share_choose_type")
                    }
                    R.id.export -> {
                        PoemExportDialog().show(childFragmentManager, "export_dialog")
                    }
                    R.id.setting -> {
                        PoemSettingDialog().show(childFragmentManager, "poem_setting")
                    }
                    R.id.bookmark -> {
                        visibleChildFrag?.let { poemPagerFragment ->
                            val shouldAddBookmark =
                                event.menuItem.title == resources.getString(R.string.bookmark_add_hint)
                            poemViewModel.updateBookmark(
                                state = if (shouldAddBookmark) Calendar.getInstance().timeInMillis else null,
                                poemId = poemPagerFragment.poemItem.id
                            )
                        }
                    }
                    R.id.help -> {
                        poemViewModel.reportEvent(PoemEvent.OnShowHelp(PoemHelpState.PAGING))
                    }
                }
            }
            is PoemEvent.OnToggleButtonClicked -> {
                visibleChildFrag?.let { poemPagerFragment ->
                    poemViewModel.updateBookmark(
                        state = if (event.isChecked) Calendar.getInstance().timeInMillis else null,
                        poemId = poemPagerFragment.poemItem.id
                    )
                }
            }
            is PoemEvent.OnContextMenuItemClicked -> {
                visibleChildFrag?.let { poemPagerFragment ->
                    when (event.menuItem?.itemId) {
                        R.id.favorite -> {
                            poemPagerFragment.applyFavorite()
                            true
                        }
                        R.id.note -> {
                            poemPagerFragment.addNote(poemPagerFragment.mSelectedVerses[0])
                            poemPagerFragment.finishActionMode()
                            true
                        }
                        R.id.hilight -> {
                            poemPagerFragment.poemHilighter.fullHilight(poemPagerFragment.mSelectedVerses)
                            true
                        }
                        R.id.copy -> {
                            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(
                                "sher",
                                poemPagerFragment.selectedVersesText()
                            )
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                            true
                        }
                        else -> {}
                    }
                }
            }
            is PoemEvent.OnShowHelp -> {
                help.showHelp(event.state, if (firstFragEntrance) 2500 else 500)
            }
            is PoemEvent.OnRefreshContent -> {
                childFragmentManager.fragments.forEach { fragment ->
                    if (fragment is PoemPagerFragment)
                        fragment.refreshContent()
                }
            }
            is PoemEvent.OnRefreshTextMenu -> {
                poemViewModel.apply {
                    textMenuText ?: return@apply
                    if (textMenuVisible){
                        val width = binding.textMenu.width
                        val height = binding.textMenu.height

                        binding.textMenu.x =
                            (textMenuLocation[0].toFloat() + textMenuX - width/2)
                                .coerceAtMost(binding.root.width - width - 16.dpTOpx(resources))
                                .coerceAtLeast(16.dpTOpx(resources))
                        binding.textMenu.y = textMenuLocation[1].toFloat() + textMenuY - height
                        binding.rightHandle.x = textMenuLocation[0].toFloat() + rightHandleX
                        binding.rightHandle.y = textMenuLocation[1].toFloat() + rightHandleY
                        binding.leftHandle.x = textMenuLocation[0].toFloat() + leftHandleX - binding.leftHandle.width
                        binding.leftHandle.y = textMenuLocation[1].toFloat() + leftHandleY

                        binding.textMenu.visibility = View.VISIBLE
                        binding.leftHandle.visibility = View.VISIBLE
                        binding.rightHandle.visibility = View.VISIBLE

                        val hilightSegments = textHilight?.split(',')?.map { str -> str.toInt() }
                            ?.chunked(3)
                        start = textMenuText!!.substring(0, textMenuStart)
                            .replace("ـ", "")
                            .replace("\\s+".toRegex(), " ").length
                        end = textMenuText!!.substring(0, textMenuEnd)
                            .replace("ـ", "")
                            .replace("\\s+".toRegex(), " ").length

                        if (hilightSegments?.map { mList -> mList[0] <= start && mList[1] >= end }?.contains(true) == true)
                            binding.eraser.visibility = View.VISIBLE
                        else
                            binding.eraser.visibility = View.GONE

                        meaning.value = emptyList()
                        val textLength = textMenuText!!.length
                        meanWord = textMenuText
                            ?.subSequence(textMenuStart.coerceIn(0, textLength), textMenuEnd.coerceIn(0, textLength))
                            .toString()
                            .replace("ـ", "")
                            .replace("\\s+".toRegex(), " ")
                            .trim()

                        binding.dictionary.visibility =  if (meanWord.length > 31) View.GONE else View.VISIBLE

                        val meanWordBiErab = makeTextBiErab(meanWord)
                            .replace("\u200C" , "")
                            .replace(" ".toRegex(), "")

                        getMeaning(meanWordBiErab)

                    } else {
                        binding.textMenu.visibility = View.GONE
                        binding.leftHandle.visibility = View.GONE
                        binding.rightHandle.visibility = View.GONE
                    }
                }
            }
            is PoemEvent.OnRightHandleMove -> {
                when (event.motionEvent.actionMasked){
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.motionEvent.rawX
                        initialTouchY = event.motionEvent.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        (visibleChildFrag?.textMenuTextView as TextSelectableView).rightHandleMove(
                            dx = event.motionEvent.rawX - initialTouchX,
                            dy = event.motionEvent.rawY - initialTouchY
                        )
                    }
                    MotionEvent.ACTION_UP -> {
                        (visibleChildFrag?.textMenuTextView as TextSelectableView).saveInitialCharsXY()
                    }
                }
            }
            is PoemEvent.OnLeftHandleMove -> {
                when (event.motionEvent.actionMasked){
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.motionEvent.rawX
                        initialTouchY = event.motionEvent.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        (visibleChildFrag?.textMenuTextView as TextSelectableView).leftHandleMove(
                            dx = event.motionEvent.rawX - initialTouchX,
                            dy = event.motionEvent.rawY - initialTouchY
                        )
                    }
                    MotionEvent.ACTION_UP -> {
                        (visibleChildFrag?.textMenuTextView as TextSelectableView).saveInitialCharsXY()
                    }
                }
            }
            is PoemEvent.OnExportDialogPositiveClick -> {
                visibleChildFrag?.onExportDialogPositiveClick()
            }
            is PoemEvent.OnShareDialogPositiveClick -> {
                visibleChildFrag?.onShareDialogPositiveClick()
            }

        }
    }

    private fun setTooltip(){
        TooltipCompat.setTooltipText(binding.dictionary, resources.getString(R.string.dictionary_hint))
        TooltipCompat.setTooltipText(binding.copy, resources.getString(R.string.copy_hint))
        TooltipCompat.setTooltipText(binding.addNote, resources.getString(R.string.note_hint))
        TooltipCompat.setTooltipText(binding.marker1, resources.getString(R.string.hilight1_hint))
        TooltipCompat.setTooltipText(binding.marker2, resources.getString(R.string.hilight2_hint))
        TooltipCompat.setTooltipText(binding.marker3, resources.getString(R.string.hilight3_hint))
        TooltipCompat.setTooltipText(binding.eraser, resources.getString(R.string.delete_hilight_hint))
    }

    fun setBookImageBitmap(bitmap: Bitmap?){
        _binding?.bookImage?.setImageBitmap(bitmap)
    }

}


const val ARG_POEM_ITEM = "poem_item"
const val ARG_POEM_POSITION = "poem_position"

class PoemAdapter(
    fragManager: FragmentManager,
    lifeCycle: Lifecycle,
    private val itemNumber: Int,
    private val poemList: List<Content?>
    ) : FragmentStateAdapter(fragManager, lifeCycle) {

    override fun getItemCount(): Int = itemNumber

    override fun createFragment(position: Int): Fragment {
        val fragment = PoemPagerFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_POEM_POSITION, position)
            putParcelable(ARG_POEM_ITEM, poemList[position])
        }
        return fragment
    }
}

