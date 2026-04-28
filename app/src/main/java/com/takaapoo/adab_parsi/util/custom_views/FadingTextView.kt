package com.takaapoo.adab_parsi.util.custom_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.google.android.material.textview.MaterialTextView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.util.getColorFromAttr

class FadingTextView(context: Context, attrs: AttributeSet) : MaterialTextView(context, attrs) {

    private val newPaint: Paint
    init {
        paint.color = context.getColorFromAttr(R.attr.colorOnSurface)
        newPaint = Paint(paint)
    }

    override fun onDraw(canvas: Canvas) {
        paint.textAlign = Paint.Align.RIGHT
        newPaint.textAlign = Paint.Align.RIGHT

        for (i in 0 until layout.lineCount) {
            val line = text.substring(layout.getLineStart(i), layout.getLineEnd(i)).trim()
            val y = (layout.getLineBaseline(i) + paddingTop).toFloat()

            canvas.drawText(line, width.toFloat(), y,
                if (layout.height > height) newPaint.apply {
                    color = Color.argb((255 * 2 * (height-y)/(height)).toInt()
                        .coerceAtMost(255).coerceAtLeast(0), color.red, color.green, color.blue)
                } else paint
            )
        }
    }
}