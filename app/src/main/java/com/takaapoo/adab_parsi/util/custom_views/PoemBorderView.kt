package com.takaapoo.adab_parsi.util.custom_views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.preference.PreferenceManager
import com.takaapoo.adab_parsi.R
import kotlin.math.roundToInt

class PoemBorderView(context: Context, attrs: AttributeSet) : NestedScrollView(context, attrs) {

    private var borderType: Int = PreferenceManager.getDefaultSharedPreferences(context)
        .getInt("paper_border", 1)

    private val frameSide = listOf(
        ResourcesCompat.getDrawable(resources, R.drawable.frame5_2_2, context.theme)!!,
        ResourcesCompat.getDrawable(resources, R.drawable.frame6_2, context.theme)!!,
        ResourcesCompat.getDrawable(resources, R.drawable.frame7_2, context.theme)!!
    )

    private val frameCornerDimRatio = listOf(72.49f, 128f, 200f)
    private val frameSideWidthRatio = listOf(137.84f, 424f, 156f)
    private val frameSideHeightRatio = listOf(41.46f, 117f, 71f)

    private var frameCornerDim = 0f
    private var frameSideWidth = 0f
    private var frameSideHeight = 0f

    private var scale = 0f
    private var frameSideCount = 0
    private val poemTitleHeight = resources.getDimensionPixelSize(R.dimen.book_content_height)


    override fun onDraw(canvas: Canvas) {
        if (borderType > 0) {
            scale =
                poemTitleHeight / (frameCornerDimRatio[borderType - 1] + 2 * frameSideWidthRatio[borderType - 1])
            frameCornerDim = frameCornerDimRatio[borderType - 1] * scale
            frameSideWidth = frameSideWidthRatio[borderType - 1] * scale
            frameSideHeight = frameSideHeightRatio[borderType - 1] * scale
            frameSideCount = (height / frameSideWidth).toInt() + 1

            canvas.let {
                myDraw(it)
                it.scale(-1f, 1f, width / 2f, 0f)
                myDraw(it)
                it.scale(-1f, 1f, width / 2f, 0f)
            }
        }
    }

    private fun myDraw(canvas: Canvas){
        canvas.rotate(-90f, 0f, 0f)
        canvas.scale(-1f, 1f, 0f, 0f)
        for (i in 1 .. frameSideCount){
            frameSide[borderType - 1].setBounds(
                ((i - 1) * frameSideWidth - 1).toInt(),
                (- frameSideHeight / 2).roundToInt(),
                (i * frameSideWidth + 1).toInt(),
                (frameSideHeight / 2).roundToInt()
            )
            frameSide[borderType - 1].draw(canvas)
        }
        canvas.scale(-1f, 1f, 0f, 0f)
        canvas.rotate(90f, 0f, 0f)
    }

    fun setBorderType(mBorderType: Int){
        borderType = mBorderType
        invalidate()
        requestLayout()
    }
}
