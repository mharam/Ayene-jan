package com.takaapoo.adab_parsi.util.custom_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.textview.MaterialTextView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.makeTextBiErab
import com.takaapoo.adab_parsi.util.spTOpx
import dagger.hilt.android.internal.managers.FragmentComponentManager
import kotlin.math.roundToInt

class TitleView(context: Context, attrs: AttributeSet) : MaterialTextView(context, attrs) {

    private var cornerDrawable = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.border,
        null
    )!!
    private var frameCorner = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.frame5_1_2,
        null
    )!!
    private var frameSide = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.frame5_2_2,
        null
    )!!

    private var frameCornerDim = 72.49f
    private var frameSideWidth = 137.84f
    private var frameSideHeight = 41.46f

    var scale = 0f
    private var horFrameSideCount = 0
    private var horFrameSideWidth = 0f
    private val mContext = FragmentComponentManager.findActivity(context)

    //    private val paint2: Paint
    private val settingViewModel = ViewModelProvider(mContext as ViewModelStoreOwner)
        .get(SettingViewModel::class.java)

    init {
        paint.textAlign = Paint.Align.CENTER
        paint.color = currentTextColor
//        paint.textSize = settingViewModel.fontSize.spTOpx(context.resources)
//        paint.typeface = Typeface.create(
//            settingViewModel.font,
//            if (settingViewModel.fontPref == 0) Typeface.NORMAL else Typeface.BOLD
//        )
//        paint2 = Paint(paint)
    }

    override fun onDraw(canvas: Canvas?) {
        paint.textSize = settingViewModel.fontSize.spTOpx(context.resources)
        paint.typeface = Typeface.create(
            settingViewModel.font,
            if (settingViewModel.fontPref == 0) Typeface.NORMAL else Typeface.BOLD
        )

        scale = height / (frameCornerDim + 2 * frameSideWidth)
        frameCornerDim *= scale
        frameSideWidth *= scale
        frameSideHeight *= scale
        horFrameSideCount = ((width/2f - frameCornerDim + frameSideHeight/2)/frameSideWidth - 0.1f).roundToInt()
        horFrameSideWidth = (width/2f - frameCornerDim + frameSideHeight/2) / horFrameSideCount

        cornerDrawable.setBounds(
            (frameSideHeight / 2).toInt(), (frameSideHeight).toInt(),
            height - (frameSideHeight / 2).toInt(), height
        )
        frameCorner.setBounds(
            (-frameSideHeight / 2).toInt(), 0,
            (-frameSideHeight / 2 + frameCornerDim).toInt(), frameCornerDim.toInt()
        )
        canvas?.let {
            drawText(it)
            drawBorder(it)
            it.scale(-1f, 1f, width / 2f, 0f)
            drawBorder(it)
            it.scale(-1f, 1f, width / 2f, 0f)
        }
    }

    private fun drawBorder(canvas: Canvas){
        cornerDrawable.draw(canvas)
        frameCorner.draw(canvas)
        canvas.rotate(-90f, 0f, 0f)
        canvas.scale(-1f, 1f, 0f, 0f)
        for (i in 1 .. 2){
            frameSide.setBounds(
                (frameCornerDim + (i - 1) * frameSideWidth - 1).toInt(),
                (-frameSideHeight / 2).toInt(),
                (frameCornerDim + i * frameSideWidth + 1).toInt(),
                (frameSideHeight / 2).toInt()
            )
            frameSide.draw(canvas)
        }
        canvas.scale(-1f, 1f, 0f, 0f)
        canvas.rotate(90f, 0f, 0f)
        for (i in 1 .. horFrameSideCount){
            frameSide.setBounds(
                (frameCornerDim - frameSideHeight / 2 + (i - 1) * horFrameSideWidth - 1).toInt(),
                0,
                (frameCornerDim - frameSideHeight / 2 + i * horFrameSideWidth + 1).toInt(),
                frameSideHeight.toInt()
            )
            frameSide.draw(canvas)
        }
    }

    private fun drawText(canvas: Canvas){
        val l11 = width - height
        val l21 = width - 4 * height / 3
        val l22 = width - 2 * height / 3
        val l31 = width - 6 * height / 4
        val l33 = width - height / 2
        val l41 = width - 8 * height / 5
        val l42 = width - 6 * height / 5
        val l43 = width - 4 * height / 5
        val l44 = width - 2 * height / 5

        val title = text.toString()
        val titleBiErab = makeTextBiErab(title)
        var textWidth = paint.measureText(title)
        while (textWidth > l31 + l11 + l33 && paint.textSize > 18.spTOpx(context.resources)){
            paint.textSize -= 1.spTOpx(context.resources)
            textWidth = paint.measureText(title)
        }
        val middleHeight = frameSideHeight + height/2f
        val rectangle = Rect()
        Paint(paint).getTextBounds(titleBiErab, 0, titleBiErab.length, rectangle)
        val separation = (if (settingViewModel.fontPref == 0) 0.9f else 1.4f) * rectangle.height()

        when {
            textWidth < l11 -> {
                canvas.drawText(title, width / 2f, middleHeight, paint)
            }
            textWidth < l21 + l22 -> {
                val endChar = title.indexOf(' ', text.length * l21 / (l21 + l22))
                canvas.drawText(text, 0, endChar, width / 2f, middleHeight - separation / 2, paint)
                canvas.drawText(
                    text,
                    endChar + 1,
                    text.length,
                    width / 2f,
                    middleHeight + separation / 2,
                    paint
                )
            }
            textWidth < l31 + l11 + l33 || settingViewModel.fontPref == 0 -> {
                val newText = if (textWidth > l31 + l11 + l33)
                    textCutter(text.toString(), paint, l31 + l11 + l33) else text
                val endChar1 = newText.indexOf(' ', newText.length * l31 / (l31 + l11 + l33))
                val endChar2 = newText.indexOf(
                    ' ',
                    newText.length * (l31 + l11) / (l31 + l11 + l33)
                )
                canvas.drawText(newText, 0, endChar1, width / 2f, middleHeight - separation, paint)
                canvas.drawText(newText, endChar1 + 1, endChar2, width / 2f, middleHeight, paint)
                canvas.drawText(
                    newText, endChar2 + 1, newText.length, width / 2f,
                    middleHeight + separation, paint
                )
            }
            else -> {
                val newText = if (textWidth > l41 + l42 + l43 + l44)
                    textCutter(text.toString(), paint, l41 + l42 + l43 + l44) else text
                val endChar1 = newText.indexOf(' ', newText.length * l41 / (l41 + l42 + l43 + l44))
                val endChar2 = newText.indexOf(
                    ' ',
                    newText.length * (l41 + l42) / (l41 + l42 + l43 + l44)
                )
                val endChar3 = newText.indexOf(
                    ' ',
                    newText.length * (l41 + l42 + l43) / (l41 + l42 + l43 + l44)
                )
                canvas.drawText(
                    newText,
                    0,
                    endChar1,
                    width / 2f,
                    middleHeight - 3 * separation / 2,
                    paint
                )
                canvas.drawText(
                    newText,
                    endChar1 + 1,
                    endChar2,
                    width / 2f,
                    middleHeight - separation / 2,
                    paint
                )
                canvas.drawText(
                    newText,
                    endChar2 + 1,
                    endChar3,
                    width / 2f,
                    middleHeight + separation / 2,
                    paint
                )
                canvas.drawText(
                    newText, endChar3 + 1, newText.length, width / 2f,
                    middleHeight + 3 * separation / 2, paint
                )

            }
        }
    }

    private fun textCutter(inputText: String, textPaint: TextPaint, maxLength: Int): String{
        var outputText = inputText.substringBeforeLast(' ')
        while (textPaint.measureText(outputText) > maxLength)
            outputText = outputText.substringBeforeLast(' ')

        return context.resources.getString(R.string.truncated_text, outputText)
    }

}
