package com.takaapoo.adab_parsi.util.custom_views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.getColorFromAttr


class HelpCircleFocus(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val mPath = Path().apply { fillType = Path.FillType.INVERSE_EVEN_ODD }
    private val backColor = context.getColorFromAttr(R.attr.colorHelpScream)

    var radius = 1500.dpTOpx(resources)
    var centerX = 0f
    var centerY = 0f

    override fun onDraw(canvas: Canvas?) {
        mPath.reset()
        mPath.addCircle(centerX, centerY, radius, Path.Direction.CW)

        canvas?.let {
            it.clipPath(mPath)
            it.drawColor(backColor)
        }
    }
}

class HelpRectFocus(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val mPath = Path().apply { fillType = Path.FillType.INVERSE_EVEN_ODD }
    private val backColor = context.getColorFromAttr(R.attr.colorHelpScream)

    private val radius = 8.dpTOpx(resources)
    var rect = RectF()

    override fun onDraw(canvas: Canvas?) {
        mPath.reset()
        mPath.addRoundRect(rect, radius, radius, Path.Direction.CW)

        canvas?.let {
            it.clipPath(mPath)
            it.drawColor(backColor)
        }
    }
}