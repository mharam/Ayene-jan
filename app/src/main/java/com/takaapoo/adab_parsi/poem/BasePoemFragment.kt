package com.takaapoo.adab_parsi.poem

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.bookmark.BookmarkDetailFragment
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.databinding.FragmentPoemBinding
import com.takaapoo.adab_parsi.util.custom_views.TextSelectableView
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.makeTextBiErab
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.poem_item.view.*
import kotlinx.coroutines.launch


const val DARK_ALPHA_MAX = 0.35f

@AndroidEntryPoint
abstract class BasePoemFragment: Fragment(), PoemTextMenu.FragmentPreparer {

    private var _binding: FragmentPoemBinding? = null
    val binding get() = _binding!!
    val poemViewModel: PoemViewModel by activityViewModels()

    private var _viewPager: ViewPager2? = null
    val viewPager get() = _viewPager!!

    private var mPoemTextMenu: PoemTextMenu? = null
    var start = 0
    var end = 0

    var visibleChildFrag: PoemPagerFragment? = null
    private var onPauseCalled = false
    private val hideSystemBarsRunnable = Runnable { (activity as MainActivity).hideSystemBars() }
//    private val windowHeightMethod =
//        InputMethodManager::class.java.getMethod("getInputMethodWindowVisibleHeight")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentPoemBinding.inflate(inflater, container, false)
        _viewPager = binding.bookViewPager

        mPoemTextMenu = PoemTextMenu(poemViewModel, this)
        binding.ptm = mPoemTextMenu

//        activity?.let {
//            KeyboardVisibilityEvent.setEventListener(it, viewLifecycleOwner){ isOpen ->
//
//            }
//        }

        return binding.root
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                poemViewModel.uiEvent.collect { event ->
                    when(event){
                        is PoemEvent.OnExportDialogPositiveClick -> {
                            visibleChildFrag?.onExportDialogPositiveClick()
                        }
                        is PoemEvent.OnShareDialogPositiveClick -> {
                            visibleChildFrag?.onShareDialogPositiveClick()
                        }
                    }
                }
            }
        }
        poemViewModel.apply {
            refreshContent.observe(viewLifecycleOwner){
                if (it == true){
                    childFragmentManager.fragments.forEach { fragment ->
                        if (fragment is PoemPagerFragment)
                            fragment.refreshContent()
                    }
                    poemViewModel.doneRefreshing()
                }
            }
            refreshTextMenu.observe(viewLifecycleOwner){
                if (it == true && textMenuText != null){
                    if (textMenuVisible && !textMenuHide){
                        val width = binding.textMenu.width
                        val height = binding.textMenu.height

//                        textMenuTextView?.getLocationInWindow(outLocation)
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

                        mPoemTextMenu!!.start = start
                        mPoemTextMenu!!.end = end

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
                        setTooltip()

                    } else {
                        binding.textMenu.visibility = View.GONE
                        binding.leftHandle.visibility = View.GONE
                        binding.rightHandle.visibility = View.GONE
                    }

                    doneRefreshingTextMenu()
                }
            }
        }

        var initialTouchX = 0f
        var initialTouchY = 0f
        binding.rightHandle.setOnTouchListener { _, event ->
            when (event.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    (visibleChildFrag?.textMenuTextView as TextSelectableView)
                        .rightHandleMove(event.rawX - initialTouchX, event.rawY - initialTouchY)
                }
                MotionEvent.ACTION_UP -> {
                    (visibleChildFrag?.textMenuTextView as TextSelectableView).saveInitialCharsXY()
                }
            }
            true
        }
        binding.leftHandle.setOnTouchListener { _, event ->
            when (event.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    (visibleChildFrag?.textMenuTextView as TextSelectableView)
                        .leftHandleMove(event.rawX - initialTouchX, event.rawY - initialTouchY)
                }
                MotionEvent.ACTION_UP -> {
                    (visibleChildFrag?.textMenuTextView as TextSelectableView).saveInitialCharsXY()
                }
            }
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val keyBoardIsOpen = windowInsets.isVisible(WindowInsetsCompat.Type.ime())

            val focusOwnerView = binding.root.findFocus()
            val itemView = focusOwnerView?.parent?.parent as? ViewGroup
            if (!keyBoardIsOpen && focusOwnerView?.id == R.id.comment_text && itemView?.save?.isVisible == true){
                val note = (focusOwnerView as EditText).text.toString()
                visibleChildFrag?.run {
                    saveComment(note,
                        binding.poemList.getChildItemId(itemView).toInt(), itemView.save)
                }
            }

            if ((this is PoemFragment || this is BookmarkDetailFragment) &&
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
//                    it.window?.decorView?.systemUiVisibility?.let { sysUiVis ->
//                        it.window?.decorView?.systemUiVisibility =
//                            if (isOpen)
//                                sysUiVis and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
//                            else
//                                sysUiVis or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    }


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

//        barsPreparation()
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
        viewPager.adapter = null
        mPoemTextMenu = null
        visibleChildFrag = null
        _viewPager = null

        _binding = null
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

