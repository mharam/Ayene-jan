package com.takaapoo.adab_parsi.util.custom_views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import com.takaapoo.adab_parsi.R

class PoemBorderView(context: Context, attrs: AttributeSet) : NestedScrollView(context, attrs) {

    //    private val frameCorner = ResourcesCompat.getDrawable(
//        context.resources,
//        R.drawable.frame5_1,
//        null
//    )!!
    private val frameSide = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.frame5_2_2,
        null
    )!!

    private var frameCornerDim = 72.49f
    private var frameSideWidth = 137.84f
    private var frameSideHeight = 41.46f

    var scale = 0f
    private var frameSideCount = 0
//    private val actionBarSize = context.getDimenFromAttr(R.attr.actionBarSize)
    private val poemTitleHeight = resources.getDimensionPixelSize(R.dimen.book_content_height)


    override fun onDraw(canvas: Canvas?) {
        scale = poemTitleHeight / (frameCornerDim + 2 * frameSideWidth)
        frameCornerDim *= scale
        frameSideWidth *= scale
        frameSideHeight *= scale
        frameSideCount = (height / frameSideWidth).toInt() + 1

        canvas?.let {
            myDraw(it)
            it.scale(-1f, 1f, width / 2f, 0f)
            myDraw(it)
            it.scale(-1f, 1f, width / 2f, 0f)
        }
    }

    private fun myDraw(canvas: Canvas){
        canvas.rotate(-90f, 0f, 0f)
        canvas.scale(-1f, 1f, 0f, 0f)
        for (i in 1 .. frameSideCount){
            frameSide.setBounds(
                ((i - 1) * frameSideWidth - 1).toInt(),
                (- frameSideHeight / 2).toInt(),
                (i * frameSideWidth + 1).toInt(),
                (frameSideHeight / 2).toInt()
            )
            frameSide.draw(canvas)
        }
        canvas.scale(-1f, 1f, 0f, 0f)
        canvas.rotate(90f, 0f, 0f)
    }
}
