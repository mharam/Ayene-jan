package com.takaapoo.adab_parsi.util.custom_views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.getColorFromAttr
import com.takaapoo.adab_parsi.util.spTOpx


class HelpFlasher(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val arrowPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(6.015f, 0f)
        lineTo(13.976f, 8f)
        lineTo(6.015f, 16f)
        lineTo(0f, 16f)
        lineTo(7.961f, 8f)
        close()
    }
    private val arrowPathRight = Path(arrowPath)
    private val arrowPathLeft = Path(arrowPath)

    var offseted = false
    private val myPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val textPaint = Paint().apply {
        color = context.getColorFromAttr(R.attr.colorSurface)
        typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.iransans_medium), Typeface.BOLD)
        textSize = 18.spTOpx(resources)
        textAlign = Paint.Align.CENTER
    }
    private val helpScreamColor = context.getColorFromAttr(R.attr.colorHelpScream)

    private var l = 0f
    private var r = 0f
    private var up = 0
    private var down = 0
    private var center = 0
    private val border = 50.dpTOpx(resources)
    private val rect = Rect()
    private val arrowArrayRight: MutableList<Path>
    private lateinit var arrowArrayRightX: MutableList<Float>
    private val arrowArrayLeft: MutableList<Path>
    private lateinit var arrowArrayLeftX: MutableList<Float>

    private val reg = Region()
    private val rectReg = Region()
    private val speed = 32.dpTOpx(resources)
    private var disp = 0f
    private val arrowCount = 7
    private var prevTime = 0L
    private var elapsedTime = 0L

    private val textUp: String?
    private val textDown: String?

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val matR = Matrix()
        matR.preScale(3.dpTOpx(resources), 3.dpTOpx(resources))
        arrowPathRight.set(arrowPath)
        arrowPathRight.transform(matR)
        arrowArrayRight = MutableList(arrowCount){ Path(arrowPathRight) }

        val matL = Matrix()
        matL.preScale((-3).dpTOpx(resources), 3.dpTOpx(resources))
        arrowPathLeft.set(arrowPath)
        arrowPathLeft.transform(matL)
        arrowArrayLeft = MutableList(arrowCount){ Path(arrowPathLeft) }

        context.theme.obtainStyledAttributes(attrs, R.styleable.HelpFlasher, 0, 0).apply {
            try {
                textUp = getString(R.styleable.HelpFlasher_textUp)
                textDown = getString(R.styleable.HelpFlasher_textDown)
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        val currentTime = System.nanoTime()
        elapsedTime = currentTime - prevTime
        prevTime = currentTime

        disp = elapsedTime * speed / 1000000000

        canvas?.let {
            it.drawColor(helpScreamColor)
            for (i in arrowArrayRight.indices){
                arrowArrayRightX[i] += disp
                arrowArrayLeftX[i] -= disp
                arrowArrayRight[i].offset(disp, 0f)
                arrowArrayLeft[i].offset(-disp, 0f)

                if (arrowArrayRightX[i] > r){
                    arrowArrayRightX[i] -= 7 * 36.dpTOpx(resources)
                    arrowArrayRight[i].offset(-7 * 36.dpTOpx(resources), 0f)
                }

                if (arrowArrayLeftX[i] < l){
                    arrowArrayLeftX[i] += 7 * 36.dpTOpx(resources)
                    arrowArrayLeft[i].offset(7 * 36.dpTOpx(resources), 0f)
                }

                reg.setPath(arrowArrayRight[i], rectReg)
                it.drawPath(reg.boundaryPath, myPaint)
                it.drawText(textUp ?: "", center.toFloat(), up - 40.dpTOpx(resources), textPaint)

                reg.setPath(arrowArrayLeft[i], rectReg)
                it.drawPath(reg.boundaryPath, myPaint)
                it.drawText(textDown ?: "", center.toFloat(), down + 48.dpTOpx(resources), textPaint)
            }

        }

        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        l = MeasureSpec.getSize(widthMeasureSpec)/2 - 100.dpTOpx(resources)
        r = MeasureSpec.getSize(widthMeasureSpec)/2 + 100.dpTOpx(resources)
        up = MeasureSpec.getSize(heightMeasureSpec) / 5
        down = 4 * MeasureSpec.getSize(heightMeasureSpec) / 5
        center = MeasureSpec.getSize(widthMeasureSpec)/2
        rect.apply {
            left = l.toInt()
            top = 0
            right = r.toInt()
            bottom = MeasureSpec.getSize(heightMeasureSpec)
        }
        rectReg.set(rect)
        arrowArrayRightX = MutableList(arrowCount){ i -> l - border + i * 36.dpTOpx(resources) }
        arrowArrayLeftX = MutableList(arrowCount){ i -> r + border - i * 36.dpTOpx(resources) }


//        if (/*!offseted*/true) {
            arrowArrayRight.forEach { it.set(arrowPathRight) }
            arrowArrayLeft.forEach { it.set(arrowPathLeft) }
            for (i in arrowArrayRight.indices) {
                arrowArrayRight[i].offset(arrowArrayRightX[i], up.toFloat() - 24.dpTOpx(resources))
                arrowArrayLeft[i].offset(arrowArrayLeftX[i], down.toFloat() - 24.dpTOpx(resources))
            }
//        }

        offseted = true
        prevTime = System.nanoTime()
    }
}


class HelpFlasherDown(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val arrowPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, 6.015f)
        lineTo(8f, 13.976f)
        lineTo(16f, 6.015f)
        lineTo(16f, 0f)
        lineTo(8f, 7.961f)
        close()
    }

    var offseted = false
    private val myPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val helpScreamColor = context.getColorFromAttr(R.attr.colorHelpScream)

    private var l = 0
    private var r = 0
    private var up = 0
    private var down = 0f
    private val border = 50.dpTOpx(resources)
    private val rect = Rect()
    private val arrowArrayRight: MutableList<Path>
    private lateinit var arrowArrayY: MutableList<Float>
    private val arrowArrayLeft: MutableList<Path>

    private val reg = Region()
    private val rectReg = Region()
    private val speed = 32.dpTOpx(resources)
    private var disp = 0f
    private val arrowCount = 7
    private var prevTime = 0L
    private var elapsedTime = 0L


    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val mat = Matrix()
        mat.preScale(3.dpTOpx(resources), 3.dpTOpx(resources))
        arrowPath.transform(mat)
        arrowArrayRight = MutableList(arrowCount){ Path(arrowPath) }
        arrowArrayLeft = MutableList(arrowCount){ Path(arrowPath) }
    }

    override fun onDraw(canvas: Canvas?) {
        val currentTime = System.nanoTime()
        elapsedTime = currentTime - prevTime
        prevTime = currentTime

        disp = elapsedTime * speed / 1000000000

        canvas?.let {
            it.drawColor(helpScreamColor)
            for (i in arrowArrayRight.indices){
                this.arrowArrayY[i] += disp
                arrowArrayRight[i].offset(0f, disp)
                arrowArrayLeft[i].offset(0f, disp)

                if (this.arrowArrayY[i] > down){
                    this.arrowArrayY[i] -= 7 * 36.dpTOpx(resources)
                    arrowArrayRight[i].offset(0f,-7 * 36.dpTOpx(resources))
                    arrowArrayLeft[i].offset(0f,-7 * 36.dpTOpx(resources))
                }

                reg.setPath(arrowArrayRight[i], rectReg)
                it.drawPath(reg.boundaryPath, myPaint)

                reg.setPath(arrowArrayLeft[i], rectReg)
                it.drawPath(reg.boundaryPath, myPaint)
            }
        }

        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        up = MeasureSpec.getSize(heightMeasureSpec) / 5
        down = up + 200.dpTOpx(resources)
        l = MeasureSpec.getSize(widthMeasureSpec)/5
        r = 4 * MeasureSpec.getSize(widthMeasureSpec)/5

        rect.apply {
            left = 0
            top = up
            right = MeasureSpec.getSize(widthMeasureSpec)
            bottom = down.toInt()
        }
        rectReg.set(rect)
        this.arrowArrayY = MutableList(arrowCount){ i -> up - border + i * 36.dpTOpx(resources) }

//        if (/*!offseted*/true) {
            arrowArrayRight.forEach { it.set(arrowPath) }
            arrowArrayLeft.forEach { it.set(arrowPath) }
            for (i in arrowArrayRight.indices) {
                arrowArrayRight[i].offset(l.toFloat() - 24.dpTOpx(resources), arrowArrayY[i])
                arrowArrayLeft[i].offset(r.toFloat() - 24.dpTOpx(resources), arrowArrayY[i])
            }
//        }

        offseted = true
        prevTime = System.nanoTime()
    }
}
