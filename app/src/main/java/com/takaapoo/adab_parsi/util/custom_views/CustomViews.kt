package com.takaapoo.adab_parsi.util.custom_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.NestedScrollingParent
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.getColorFromAttr
import com.takaapoo.adab_parsi.util.spTOpx


class ParagraphView(context: Context, attrs: AttributeSet) : TextSelectableView(context, attrs) {

    private val paint2: Paint
    var spanIndex: List<Int>? = null
    var searchHilightColor = context.getColorFromAttr(R.attr.colorSecondary)
    var searchBackSpanIndex = listOf<Int>()
    var hilightBackSpanIndex = mutableListOf<Int>()
    private var backColor = context.getColorFromAttr(R.attr.colorSearchBack)
    private var selectBackColor = context.getColorFromAttr(R.attr.colorSearchSelectBack)
    var selectSpanNumber: Int? = null
    private val backPaint = Paint()
    private val rect = Rect()


    var right: Float = 0f
    var left: Float = 0f
    var top: Float = 0f
    var bottom: Float = 0f
    private var xInitial = 0f
    private var multilineHilight = false

    private var toBeNormalized = true

    init {
        val padding = resources.getDimension(R.dimen.not_nastaliq_vertical_padding).toInt()
        paint.color = currentTextColor
        // textSize == 0f -> This is for subItems in table content
        paint.textSize = /*if (textSize == 0f) (settingViewModel.fontSize - 2).spTOpx(context.resources) else*/
            settingViewModel.fontSize.spTOpx(context.resources)
        paint.typeface = settingViewModel.font
        paint2 = Paint(paint)

        when (settingViewModel.fontPref){
            0 -> {
                setLineSpacing(0f, 1f)
                updatePadding(left = paddingEnd, top = 0, right = paddingStart, bottom = 0)
            }
            else -> {
                setLineSpacing(resources.getDimension(R.dimen.not_nastaliq_line_spacing), 1f)
                updatePadding(left = paddingEnd, top = padding, right = paddingStart, bottom = padding)
            }
        }

        context.theme.obtainStyledAttributes(attrs, R.styleable.ParagraphView, 0, 0).apply {
            try {
                toBeNormalized = getBoolean(R.styleable.ParagraphView_toBeNormalized, true)
            } finally {
                recycle()
            }
        }
    }


    override fun onDraw(canvas: Canvas) {
        val spanIndices = spanIndex?.subList(1, spanIndex?.size?.minus(1) ?: 1) ?: listOf()
        multilineHilight = multilineHilight()

        when(textAlignment){
            TextView.TEXT_ALIGNMENT_CENTER -> {
                paint.textAlign = Paint.Align.RIGHT

                for (i in 0 until layout.lineCount) {
                    val lineStart = layout.getLineStart(i)
                    val lineEnd = layout.getLineEnd(i)
                    val line = text.substring(lineStart, lineEnd).trim()
                    xInitial = (width + paint2.measureText(line) - paddingStart + paddingEnd) / 2f
                    val baseLine: Int = layout.getLineBaseline(i)
                    drawThisLine(canvas, lineStart, lineEnd, baseLine, xInitial, i, spanIndices)
                }
            }
            else -> {
                paint.textAlign = Paint.Align.RIGHT
                xInitial = width.toFloat() - totalPaddingStart

                for (i in 0 until layout.lineCount) {
                    val lineStart = layout.getLineStart(i)
                    val lineEnd = layout.getLineEnd(i)
                    val line = text.substring(lineStart, lineEnd).trim(' ')
                    val baseLine: Int = layout.getLineBaseline(i)
                    if (i < layout.lineCount - 1 && toBeNormalized && line.isNotEmpty() && line.last() != '\n') {
                        val scaleX = (layout.width - settingViewModel.mWidth/2) / paint.measureText(line)
                        canvas.scale(scaleX, 1f, xInitial, 0f)
                        drawThisLine(canvas, lineStart, lineEnd, baseLine, xInitial, i, spanIndices, scale = scaleX)
                        canvas.scale(1 / scaleX, 1f, xInitial, 0f)
                    } else
                        drawThisLine(canvas, lineStart, lineEnd, baseLine, xInitial, i, spanIndices)
                }
            }
        }
        if (multilineHilight)
            poemViewModel.apply {
                textMenuX = ((layout.getLineLeft(0)+layout.getLineRight(0))/2).toInt()
                textMenuY = layout.getLineTop(layout.getLineForOffset(selStart))
                doRefreshTextMenu()
            }
    }

    private fun currentLineHilightFilter(hilightBackSpanIndex: List<Int>, lineStart: Int, lineEnd: Int)
    : List<Int> {
        val output = mutableListOf<List<Int>>()

        for (i in hilightBackSpanIndex.indices step 3){
            val tempList = IntArray(3){0}
            when{
                hilightBackSpanIndex[i] < lineStart ->{
                    when{
                        hilightBackSpanIndex[i+1] < lineStart ->{
                            continue
                        }
                        hilightBackSpanIndex[i+1] in lineStart .. lineEnd -> {
                            tempList[0] = lineStart
                            tempList[1] = hilightBackSpanIndex[i+1]
                        }
                        hilightBackSpanIndex[i+1] > lineEnd ->{
                            tempList[0] = lineStart
                            tempList[1] = lineEnd
                        }
                    }
                }
                hilightBackSpanIndex[i] in lineStart .. lineEnd -> {
                    when{
                        hilightBackSpanIndex[i+1] in lineStart .. lineEnd -> {
                            tempList[0] = hilightBackSpanIndex[i]
                            tempList[1] = hilightBackSpanIndex[i+1]
                        }
                        hilightBackSpanIndex[i+1] > lineEnd ->{
                            tempList[0] = hilightBackSpanIndex[i]
                            tempList[1] = lineEnd
                        }
                    }
                }
                hilightBackSpanIndex[i] > lineEnd ->{
                    continue
                }
            }
            tempList[2] = hilightBackSpanIndex[i+2]
            output.add(tempList.toList())
        }

        return output.flatten()
    }

    private fun drawThisLine(
        canvas: Canvas, lineStart: Int, lineEnd: Int, baseLine: Int,
        xInitial: Float, i: Int, spanIndices: List<Int>, mText: CharSequence = text, scale: Float = 1f){

        var x = xInitial
        val currentSpanIndices = mutableListOf(lineStart, lineEnd)
        currentSpanIndices.addAll(1, spanIndices.filter { it in lineStart until lineEnd })

        val currentBackSpanIndex = searchBackSpanIndex.filter { it in lineStart..lineEnd }.toMutableList()
        if (searchBackSpanIndex.filter { it < lineStart }.size % 2 != 0)
            currentBackSpanIndex.add(0, lineStart)
        if (searchBackSpanIndex.filter { it > lineEnd }.size % 2 != 0)
            currentBackSpanIndex.add(lineEnd)

        val currentHilightBackSpanIndex = currentLineHilightFilter(
                hilightBackSpanIndex, lineStart,
                lineEnd - (if (i < layout.lineCount - 1) 1 else 0)
            )

        paint.getTextBounds(mText.toString(), lineStart, lineEnd, rect)
        val rightMost = x
        bottom = baseLine + totalPaddingTop.toFloat() + rect.bottom
        top = bottom - rect.height()


        for (j in 0 until currentHilightBackSpanIndex.size - 1 step 3) {
            right = rightMost - paint.getRunAdvance(
                mText, lineStart, currentHilightBackSpanIndex[j], lineStart,
                (currentHilightBackSpanIndex[j] + 2).coerceAtMost(mText.length), true,
                currentHilightBackSpanIndex[j]
            )
            left = rightMost - paint.getRunAdvance(
                mText,
                lineStart,
                currentHilightBackSpanIndex[j + 1],
                lineStart,
                (currentHilightBackSpanIndex[j + 1] + 2).coerceAtMost(mText.length),
                true,
                currentHilightBackSpanIndex[j + 1]
            )
            canvas.drawRect(left, top, right, bottom, backPaint.apply {
                color = settingViewModel.hilightColors[currentHilightBackSpanIndex[j + 2]]
            })
        }
        for (j in 0 until currentBackSpanIndex.size - 1 step 2) {
            right = rightMost - paint.getRunAdvance(
                mText,
                lineStart,
                currentBackSpanIndex[j],
                lineStart,
                (currentBackSpanIndex[j] + 2).coerceAtMost(mText.length),
                true,
                currentBackSpanIndex[j]
            )
            left = rightMost - paint.getRunAdvance(
                mText,
                lineStart,
                currentBackSpanIndex[j + 1],
                lineStart,
                (currentBackSpanIndex[j + 1] + 2).coerceAtMost(mText.length),
                true,
                currentBackSpanIndex[j + 1]
            )
            canvas.drawRect(left, top, right, bottom, backPaint.apply {
                color = when {
                    selectSpanNumber == null -> backColor
                    currentBackSpanIndex[j] == searchBackSpanIndex[2 * selectSpanNumber!!] -> selectBackColor
                    else -> backColor
                }
            })
        }
        if (hasSel && selStart < lineEnd && selEnd > lineStart) {
            val textLength1 = paint.getRunAdvance(
                mText, lineStart, lineEnd,
                lineStart, lineEnd, true, selStart.coerceAtLeast(lineStart)
            )
            val textLength2 = paint.getRunAdvance(
                mText, lineStart, lineEnd,
                lineStart, lineEnd, true, selEnd.coerceAtMost(lineEnd)
            )
            right = rightMost - textLength1
            left = rightMost - textLength2

            hilightThisLine(canvas, i, left, right, rightMost - textLength2 * scale,
                rightMost - textLength1 * scale)
        }


        if (currentSpanIndices.size == 2) {
            for (j in 0 until currentSpanIndices.size - 1) {
                canvas.drawText(mText,
                    currentSpanIndices[j],
                    currentSpanIndices[j + 1],
                    x,
                    baseLine + totalPaddingTop.toFloat(),
                    paint.apply {
                        color = if (j % 2 == 0) currentTextColor else searchHilightColor
                    })
                x -= paint.measureText(mText, currentSpanIndices[j], currentSpanIndices[j + 1])
            }
        } else {
            for (j in 0 until currentSpanIndices.size - 1) {
                canvas.drawTextRun(mText,
                    currentSpanIndices[j],
                    currentSpanIndices[j + 1],
                    (currentSpanIndices[j] - 2).coerceAtLeast(0),
                    (currentSpanIndices[j + 1] + 2).coerceAtMost(mText.length),
                    x,
                    baseLine + totalPaddingTop.toFloat(),
                    true,
                    paint.apply {
                        color = if (j % 2 == 0) currentTextColor else searchHilightColor
                    })
                x -= paint.getRunAdvance(
                    mText, currentSpanIndices[j], currentSpanIndices[j + 1],
                    (currentSpanIndices[j] - 2).coerceAtLeast(0),
                    (currentSpanIndices[j + 1] + 2).coerceAtMost(mText.length), true,
                    currentSpanIndices[j + 1]
                )
            }
        }
    }

    private fun multilineHilight(): Boolean{
        if (hasSel){
            val startline = layout.getLineForOffset(selStart)
            val endline = layout.getLineForOffset(selEnd)
            return startline != endline
        }
        return false
    }


    private fun hilightThisLine(canvas: Canvas, i: Int, left: Float, right: Float,
                                handLeft: Float, handRight: Float){
        canvas.drawRect(left, layout.getLineTop(i).toFloat(), right,
            layout.getLineBottom(i).toFloat(), backPaint.apply {
                color = ResourcesCompat.getColor(resources, R.color.secondary_40, context.theme)
            })
        if (!multilineHilight)
            poemViewModel.apply {
                leftHandleX = handLeft
                leftHandleY = layout.getLineBottom(i).toFloat()
                rightHandleX = handRight
                rightHandleY = leftHandleY
                textMenuX = ((right+left)/2).toInt()
                textMenuY = layout.getLineTop(i)
                doRefreshTextMenu()
            }
        else
            poemViewModel.apply {
                if (i == layout.getLineForOffset(selStart)){
                    rightHandleX = handRight
                    rightHandleY = layout.getLineBottom(i).toFloat()
                } else if (i == layout.getLineForOffset(selEnd)){
                    leftHandleX = handLeft
                    leftHandleY = layout.getLineBottom(i).toFloat()
                }
            }
    }

}

class Verse1View(context: Context, attrs: AttributeSet) : TextSelectableView(context, attrs) {

    var spanIndex: List<Int>? = null
    var searchHilightColor = context.getColorFromAttr(R.attr.colorSecondary)
    var searchBackSpanIndex = listOf<Int>()
    var hilightBackSpanIndex = listOf<Int>()
    private var backColor = context.getColorFromAttr(R.attr.colorSearchBack)
    private var selectBackColor = context.getColorFromAttr(R.attr.colorSearchSelectBack)
    var selectSpanNumber: Int? = null
    private val backPaint = Paint()
    private val rect = Rect()


    init {
        paint.textAlign = Paint.Align.LEFT
        paint.color = currentTextColor
        paint.textSize = settingViewModel.fontSize.spTOpx(context.resources)
        paint.typeface = settingViewModel.font

        height = when (settingViewModel.fontPref){
            0 -> (1.4f * lineHeight).toInt()
            else -> 2 * lineHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (text.isNullOrBlank())
            return
        val spanIndices = spanIndex ?: listOf(0, text.length)

        val baseLine: Int = layout.getLineBaseline(0)
        var x = totalPaddingEnd.toFloat()
        val layoutWidth = width - 2*totalPaddingEnd


        var right: Float
        var left: Float
        paint.getTextBounds(text.toString(), 0, text.length, rect)
        val bottom = baseLine + totalPaddingTop.toFloat() + rect.bottom
        val top = bottom - rect.height()
        val rightMost = paint.measureText(text, 0, text.length) + totalPaddingEnd
        canvasScaleX = layoutWidth / paint.measureText(text.toString())
        canvas.scale(canvasScaleX, 1f, x, 0f)

        for (i in 0 until hilightBackSpanIndex.size - 1 step 3) {
            right = rightMost - paint.getRunAdvance(
                text,
                0,
                hilightBackSpanIndex[i],
                0,
                (hilightBackSpanIndex[i] + 2).coerceAtMost(text.length),
                true,
                hilightBackSpanIndex[i]
            )
            left = rightMost - paint.getRunAdvance(
                text,
                0,
                hilightBackSpanIndex[i + 1],
                0,
                (hilightBackSpanIndex[i + 1] + 2).coerceAtMost(text.length),
                true,
                hilightBackSpanIndex[i + 1]
            )
            canvas.drawRect(left, top, right, bottom, backPaint.apply {
                color = settingViewModel.hilightColors[hilightBackSpanIndex[i + 2]]
            })
        }
        for (i in 0 until searchBackSpanIndex.size - 1 step 2) {
            right = rightMost - paint.getRunAdvance(
                text,
                0,
                searchBackSpanIndex[i],
                0,
                (searchBackSpanIndex[i] + 2).coerceAtMost(text.length),
                true,
                searchBackSpanIndex[i]
            )
            left = rightMost - paint.getRunAdvance(
                text,
                0,
                searchBackSpanIndex[i + 1],
                0,
                (searchBackSpanIndex[i + 1] + 2).coerceAtMost(text.length),
                true,
                searchBackSpanIndex[i + 1]
            )
            canvas.drawRect(left, top, right, bottom, backPaint.apply {
                color = when {
                    selectSpanNumber == null -> backColor
                    searchBackSpanIndex[i] == searchBackSpanIndex[2 * selectSpanNumber!!] -> selectBackColor
                    else -> backColor
                }
            })
        }
        if (hasSel) {
            val textLength1 = paint.getRunAdvance(
                text, selStart, text.length,
                0, text.length, true, text.length
            )
            val textLength2 = paint.getRunAdvance(
                text, selEnd, text.length,
                0, text.length, true, text.length
            )

            right = x + textLength1
            left = x + textLength2

            hilightThisLine(canvas, left, right, x + textLength2 * canvasScaleX,
                x + textLength1 * canvasScaleX)
        }

        if (spanIndices.size == 2) {
            for (j in spanIndices.size - 2 downTo 0) {
                canvas.drawText(text,
                    spanIndices[j],
                    spanIndices[j + 1],
                    x,
                    baseLine + totalPaddingTop.toFloat(),
                    paint.apply {
                        color = if (j % 2 == 0) currentTextColor else searchHilightColor
                    })
                x += paint.measureText(text, spanIndices[j], spanIndices[j + 1])
            }
        } else {
            for (j in spanIndices.size - 2 downTo 0) {
                canvas.drawTextRun(text,
                    spanIndices[j],
                    spanIndices[j + 1],
                    (spanIndices[j] - 2).coerceAtLeast(0),
                    (spanIndices[j + 1] + 2).coerceAtMost(text.length),
                    x,
                    baseLine + totalPaddingTop.toFloat(),
                    true,
                    paint.apply {
                        color = if (j % 2 == 0) currentTextColor else searchHilightColor
                    })
                x += paint.getRunAdvance(
                    text, spanIndices[j], spanIndices[j + 1],
                    (spanIndices[j] - 2).coerceAtLeast(0),
                    (spanIndices[j + 1] + 2).coerceAtMost(text.length), true, spanIndices[j + 1]
                )
            }
        }
    }

    fun reset(){
        spanIndex = null
        searchBackSpanIndex = listOf()
        hilightBackSpanIndex = listOf()
        selectSpanNumber = null
    }

    private fun hilightThisLine(canvas: Canvas, left: Float, right: Float,
                                handLeft: Float, handRight: Float){
        canvas.drawRect(left, layout.getLineTop(0).toFloat(), right,
            layout.getLineBottom(0).toFloat(), backPaint.apply {
                color = ResourcesCompat.getColor(resources, R.color.secondary_40, context.theme)
            })
        poemViewModel.apply {
            leftHandleX = handLeft
            leftHandleY = layout.getLineBottom(0).toFloat()
            rightHandleX = handRight
            rightHandleY = leftHandleY
            textMenuX = ((right+left)/2).toInt()
            textMenuY = layout.getLineTop(0)
            doRefreshTextMenu()
        }
    }

}

class Verse2View(context: Context, attrs: AttributeSet) : TextSelectableView(context, attrs) {

    var spanIndex: List<Int>? = null
    var searchHilightColor = context.getColorFromAttr(R.attr.colorSecondary)
    var searchBackSpanIndex = listOf<Int>()
    var hilightBackSpanIndex = listOf<Int>()
    private var backColor = context.getColorFromAttr(R.attr.colorSearchBack)
    private var selectBackColor = context.getColorFromAttr(R.attr.colorSearchSelectBack)
    var selectSpanNumber: Int? = null
    private val backPaint = Paint()
    private val rect = Rect()


    init {
        paint.textAlign = Paint.Align.RIGHT
        paint.color = currentTextColor
        paint.textSize = settingViewModel.fontSize.spTOpx(context.resources)
        paint.typeface = settingViewModel.font

        when (settingViewModel.fontPref){
            0 -> {
                updatePadding(left = paddingEnd, top = 0, right = paddingStart, bottom = 0)
                height = (1.4f * lineHeight).toInt()
            }
            else -> {
                updatePadding(
                    left = paddingEnd, top = 0, right = paddingStart, bottom = 4.dpTOpx(resources).toInt()
                )
                height = 2 * lineHeight
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (text.isNullOrBlank())
            return
        val spanIndices = spanIndex ?: listOf(0, text.length)

        val baseLine: Int = layout.getLineBaseline(0)
        var x = width.toFloat() - totalPaddingStart
        val layoutWidth = width - 2*totalPaddingStart

        val rightMost = x
        var right: Float
        var left: Float
        paint.getTextBounds(text.toString(), 0, text.length, rect)
        val bottom = baseLine + totalPaddingTop.toFloat() + rect.bottom
        val top = bottom - rect.height()
        canvasScaleX = layoutWidth / paint.measureText(text.toString())
        canvas.scale(canvasScaleX, 1f, x, 0f)

        for (i in 0 until hilightBackSpanIndex.size - 1 step 3) {
            right = rightMost - paint.getRunAdvance(
                text,
                0,
                hilightBackSpanIndex[i],
                0,
                (hilightBackSpanIndex[i] + 2).coerceAtMost(text.length),
                true,
                hilightBackSpanIndex[i]
            )
            left = rightMost - paint.getRunAdvance(
                text,
                0,
                hilightBackSpanIndex[i + 1],
                0,
                (hilightBackSpanIndex[i + 1] + 2).coerceAtMost(text.length),
                true,
                hilightBackSpanIndex[i + 1]
            )
            canvas.drawRect(left, top, right, bottom, backPaint.apply {
                color = settingViewModel.hilightColors[hilightBackSpanIndex[i + 2]]
            })
        }
        for (i in 0 until searchBackSpanIndex.size-1 step 2) {
            right = rightMost - paint.getRunAdvance(
                text,
                0,
                searchBackSpanIndex[i],
                0,
                (searchBackSpanIndex[i] + 2).coerceAtMost(text.length),
                true,
                searchBackSpanIndex[i]
            )
            left = rightMost - paint.getRunAdvance(
                text,
                0,
                searchBackSpanIndex[i + 1],
                0,
                (searchBackSpanIndex[i + 1] + 2).coerceAtMost(text.length),
                true,
                searchBackSpanIndex[i + 1]
            )
            canvas.drawRect(left, top, right, bottom, backPaint.apply {
                color = when {
                    selectSpanNumber == null -> backColor
                    searchBackSpanIndex[i] == searchBackSpanIndex[2 * selectSpanNumber!!] -> selectBackColor
                    else -> backColor
                }
            })
        }
        if (hasSel) {
            val textLength1 = paint.getRunAdvance(
                text, 0, text.length,
                0, text.length, true, selStart
            )
            val textLength2 = paint.getRunAdvance(
                text, 0, text.length,
                0, text.length, true, selEnd
            )

            right = rightMost - textLength1
            left = rightMost - textLength2

            hilightThisLine(canvas, left, right, rightMost - textLength2 * canvasScaleX,
                rightMost - textLength1 * canvasScaleX)
        }

        if (spanIndices.size == 2) {
            for (j in 0 until spanIndices.size - 1) {
                canvas.drawText(text,
                    spanIndices[j],
                    spanIndices[j + 1],
                    x,
                    baseLine + totalPaddingTop.toFloat(),
                    paint.apply {
                        color = if (j % 2 == 0) currentTextColor else searchHilightColor
                    })
                x -= paint.measureText(text, spanIndices[j], spanIndices[j + 1])
            }
        } else {
            for (j in 0 until spanIndices.size - 1) {
                canvas.drawTextRun(text,
                    spanIndices[j],
                    spanIndices[j + 1],
                    (spanIndices[j] - 2).coerceAtLeast(0),
                    (spanIndices[j + 1] + 2).coerceAtMost(text.length),
                    x,
                    baseLine + totalPaddingTop.toFloat(),
                    true,
                    paint.apply {
                        color = if (j % 2 == 0) currentTextColor else searchHilightColor
                    })
                x -= paint.getRunAdvance(
                    text, spanIndices[j], spanIndices[j + 1],
                    (spanIndices[j] - 2).coerceAtLeast(0),
                    (spanIndices[j + 1] + 2).coerceAtMost(text.length), true, spanIndices[j + 1]
                )
            }
        }
    }

    private fun hilightThisLine(canvas: Canvas, left: Float, right: Float, handLeft: Float,
                                handRight: Float){
        canvas.drawRect(left, layout.getLineTop(0).toFloat(), right,
            layout.getLineBottom(0).toFloat(), backPaint.apply {
                color = ResourcesCompat.getColor(resources, R.color.secondary_40, context.theme)
            })
        poemViewModel.apply {
            leftHandleX = handLeft
            leftHandleY = layout.getLineBottom(0).toFloat()
            rightHandleX = handRight
            rightHandleY = leftHandleY
            textMenuX = ((right+left)/2).toInt()
            textMenuY = layout.getLineTop(0)
            doRefreshTextMenu()
        }
    }

    fun reset(){
        spanIndex = null
        searchBackSpanIndex = listOf()
        hilightBackSpanIndex = listOf()
        selectSpanNumber = null
    }

}


class NestedRecyclerView : RecyclerView, NestedScrollingParent {

    private var nestedScrollTarget: View? = null
    private var nestedScrollTargetIsBeingDragged = false
    private var nestedScrollTargetWasUnableToScroll = false
    private var skipsTouchInterception = false


    constructor(context: Context) :
            super(context)


    constructor(context: Context, attrs: AttributeSet?) :
            super(context, attrs)


    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val temporarilySkipsInterception = nestedScrollTarget != null
        if (temporarilySkipsInterception) {
            // If a descendent view is scrolling we set a flag to temporarily skip our onInterceptTouchEvent implementation
            skipsTouchInterception = true
        }

        // First dispatch, potentially skipping our onInterceptTouchEvent
        var handled = super.dispatchTouchEvent(ev)

        if (temporarilySkipsInterception) {
            skipsTouchInterception = false

            // If the first dispatch yielded no result or we noticed that the descendent view is unable to scroll in the
            // direction the user is scrolling, we dispatch once more but without skipping our onInterceptTouchEvent.
            // Note that RecyclerView automatically cancels active touches of all its descendents once it starts scrolling
            // so we don't have to do that.
            if (!handled || nestedScrollTargetWasUnableToScroll) {
                handled = super.dispatchTouchEvent(ev)
            }
        }

        return handled
    }


    // Skips RecyclerView's onInterceptTouchEvent if requested
    override fun onInterceptTouchEvent(e: MotionEvent) =
        !skipsTouchInterception && super.onInterceptTouchEvent(e)


    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
        if (target === nestedScrollTarget && !nestedScrollTargetIsBeingDragged) {
            if (dyConsumed != 0) {
                // The descendent was actually scrolled, so we won't bother it any longer.
                // It will receive all future events until it finished scrolling.
                nestedScrollTargetIsBeingDragged = true
                nestedScrollTargetWasUnableToScroll = false
            }
            else if (dyConsumed == 0 && dyUnconsumed != 0) {
                // The descendent tried scrolling in response to touch movements but was not able to do so.
                // We remember that in order to allow RecyclerView to take over scrolling.
                nestedScrollTargetWasUnableToScroll = true
                target.parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
    }


    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        if (axes and View.SCROLL_AXIS_VERTICAL != 0) {
            // A descendent started scrolling, so we'll observe it.
            nestedScrollTarget = target
            nestedScrollTargetIsBeingDragged = false
            nestedScrollTargetWasUnableToScroll = false
        }

        super.onNestedScrollAccepted(child, target, axes)
    }


    // We only support vertical scrolling.
    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int) =
        (nestedScrollAxes and View.SCROLL_AXIS_VERTICAL != 0)


    override fun onStopNestedScroll(child: View) {
        // The descendent finished scrolling. Clean up!
        nestedScrollTarget = null
        nestedScrollTargetIsBeingDragged = false
        nestedScrollTargetWasUnableToScroll = false
    }
}

class PoemToolbar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {
    override fun onTouchEvent(ev: MotionEvent?) = false
}


class FadingTextView(context: Context, attrs: AttributeSet) : MaterialTextView(context, attrs) {

    private val newPaint: Paint
    init {
        paint.color = context.getColorFromAttr(R.attr.colorOnSurface)
        newPaint = Paint(paint)
    }

    override fun onDraw(canvas: Canvas?) {
        paint.textAlign = Paint.Align.RIGHT
        newPaint.textAlign = Paint.Align.RIGHT

        for (i in 0 until layout.lineCount) {
            val line = text.substring(layout.getLineStart(i), layout.getLineEnd(i)).trim()
            val y = (layout.getLineBaseline(i) + paddingTop).toFloat()

            canvas?.drawText(line, width.toFloat(), y,
                if (layout.height > height) newPaint.apply {
                    color = Color.argb((255 * 2 * (height-y)/(height)).toInt()
                        .coerceAtMost(255).coerceAtLeast(0)
                    , color.red, color.green, color.blue)
                } else paint
            )
        }
    }
}


