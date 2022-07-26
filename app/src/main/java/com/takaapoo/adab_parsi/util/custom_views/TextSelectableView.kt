package com.takaapoo.adab_parsi.util.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.findFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.takaapoo.adab_parsi.poem.PoemPagerFragment
import com.takaapoo.adab_parsi.poem.PoemViewModel
import com.takaapoo.adab_parsi.setting.SettingViewModel
import dagger.hilt.android.internal.managers.FragmentComponentManager
import kotlin.math.max


interface MySelection{
    fun myOnSelectionChanged(selStart: Int, selEnd: Int)
}

open class TextSelectableView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs)
    , MySelection {

    private val mContext = FragmentComponentManager.findActivity(context)

    val settingViewModel = ViewModelProvider(mContext as ViewModelStoreOwner).get(SettingViewModel::class.java)
    val poemViewModel = ViewModelProvider(mContext as ViewModelStoreOwner)[PoemViewModel::class.java]


    private val erabChars = "ًٌٍَُِّ"
    private val separatorChars = ".،؛:\"][«»)(-+*= ؟!"
    var selStart = 0
    var selEnd = 0
    var hasSel = false
    var canvasScaleX = 1f

    private var initialRightCharX = 0f
    private var initialRightCharY = 0f
    private var initialLeftCharX = 0f
    private var initialLeftCharY = 0f


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || text?.filter{it.isLetterOrDigit()}.isNullOrBlank())
            return super.onTouchEvent(event)

        var charOffset = getOffsetForPosition(event.x, event.y)

        if (charOffset == 0){
            selStart = text.indexOfFirst { !separatorChars.contains(it) }
            selEnd = max(text.subSequence(selStart, text.length)
                .indexOfFirst { separatorChars.contains(it) } - 1 + selStart , selStart) + 1
        } else {
            while (charOffset < text.length && separatorChars.contains(text[charOffset - 1]))
                charOffset++

            if (charOffset == text.length){
                selEnd = text.indexOfLast { !separatorChars.contains(it) } + 1
                selStart = text.subSequence(0, selEnd - 1).indexOfLast { separatorChars.contains(it) } + 1
            } else {
                selStart = text.subSequence(0, charOffset).indexOfLast { separatorChars.contains(it) } + 1
                val blankIndex = text.subSequence(charOffset, text.length)
                    .indexOfFirst { separatorChars.contains(it) }

                selEnd = if (blankIndex == -1) text.length else blankIndex + charOffset
            }
        }
        saveInitialCharsXY()

        return super.onTouchEvent(event)
    }

    override fun myOnSelectionChanged(selStart: Int, selEnd: Int) {
        try {
            val fragment = findFragment<PoemPagerFragment>()

            if (hasSel)
                fragment.setTextMenuParams(this)
            else if (fragment.textMenuTextView == this)
                fragment.textMenuTextView = null
        } catch (e: Exception){}
    }

    fun saveInitialCharsXY(){
        initialRightCharX = layout.getPrimaryHorizontal(selStart )
        initialRightCharY = layout.getLineBottom(layout.getLineForOffset(selStart)).toFloat()
        initialLeftCharX = layout.getPrimaryHorizontal(selEnd - 1)
        initialLeftCharY = layout.getLineBaseline(layout.getLineForOffset(selEnd)).toFloat()
    }

    fun rightHandleMove(dx: Float, dy: Float) {
        var charOffset = (getOffsetForPosition(initialRightCharX + dx, initialRightCharY + dy) - 1)
            .coerceIn(0, text.length - 1)
        if (charOffset >= selEnd)
            charOffset = (getOffsetForPosition(initialRightCharX + dx, initialRightCharY - lineHeight/2) - 1)
                .coerceIn(0, text.length - 1)

        if (erabChars.contains(text[charOffset]) && charOffset > 0) charOffset--

        if (charOffset != selStart && charOffset < selEnd && !text[charOffset].isWhitespace()
            && text[charOffset] != 'ـ') {
            selStart = charOffset
            poemViewModel.textMenuStart = selStart
            invalidate()
            poemViewModel.doRefreshTextMenu()
        }
    }

    fun leftHandleMove(dx: Float, dy: Float){
        var charOffset = getOffsetForPosition(initialLeftCharX + dx, initialLeftCharY + dy)
            .coerceIn(0, text.length)

        if (charOffset <= selStart)
            charOffset = getOffsetForPosition(initialLeftCharX + dx, initialLeftCharY)
                .coerceIn(0, text.length)

        if (charOffset != selEnd && charOffset > selStart && !text[charOffset - 1].isWhitespace()) {

            charOffset = text.substring(0, charOffset)
                .indexOfLast { c -> c != 'ـ' && !c.isWhitespace() } + 1

            selEnd = charOffset
            poemViewModel.textMenuEnd = selEnd
            invalidate()
            poemViewModel.doRefreshTextMenu()
        }
    }


}