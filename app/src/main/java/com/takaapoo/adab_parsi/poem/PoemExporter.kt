package com.takaapoo.adab_parsi.poem

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Verse
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.charFinder
import com.takaapoo.adab_parsi.util.goalChars
import com.takaapoo.adab_parsi.util.myIndexOfAny
import com.takaapoo.adab_parsi.util.spTOpx
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import kotlin.math.roundToInt


class PoemExporter(val activity: Activity, private val verses: List<Verse>, mesraWidth: Map<Int, Int>,
                   val title: String?, private val itemRoot: List<String?>, val stvm: SettingViewModel) {

    private val sharedPreference = PreferenceManager.getDefaultSharedPreferences(activity)
    private val paperNum = sharedPreference.getInt("paper_color", 0) + 1
    private val frameNumber = sharedPreference.getInt("border", 2)
    private val frameName = "frame${frameNumber}"

    private val paperColor: Int
    private val cornerDrawable: Drawable?
    private val borderDrawable: Drawable?

    private val aspectRatios = arrayOf(136.1/142.9, 300.0/151.3, 235.1/181.9)
    private val hTOd = arrayOf(127.9/142.9, 217.3/151.3, 165.5/181.9)
    private val horizontalBorderCount = arrayOf(7, 9, 6)
    private val m = horizontalBorderCount[frameNumber]
    private val aspectRatio = aspectRatios[frameNumber]          //343/155.7     // l/d

    var w = 0
    private var edge = 0.05f * w
    private var verseSeparation = (if (stvm.fontPref == 0) 0.12f else 0.08f) * w
    private var d = (w - 2*edge)/(m + 2*aspectRatio)
    private var h = (hTOd[frameNumber] * d).toFloat()               //(163.3/155.7 * d)
    private var l = aspectRatio * d

    private var mesraContainerWidth = (w - 2*(edge + h) - verseSeparation - 0.04f*w)/2
    private var paragraphWidth = w - 2*(edge + h) - 0.04f*w
    private var rightVerseEnd = (w + verseSeparation)/2
    private var leftVerseBegin = (w - verseSeparation)/2
    private var paragraphBegin = (w + paragraphWidth)/2

    private val mesraMaxWidth = mesraWidth.values.maxOrNull() ?: 0
    private val mesraMaxKey = mesraWidth.filterValues { it == mesraMaxWidth }.keys.firstOrNull()

    private lateinit var textPaint: Paint
    private lateinit var titlePaint: Paint
    private lateinit var textPaint2: TextPaint
    private lateinit var staticLayout: StaticLayout
    private var initialFontSize = 0f
    private var textHeight = 0
    private var poemHeight = 0
    private var n = 0           // number of vertical borderDrawable

    private val verticalSep = if (stvm.fontPref == 0) 1f else 1.5f
    private var paragVerticalSep = 0f


    init {
        activity.resources.let {
            paperColor = ResourcesCompat.getColor(it,
                it.getIdentifier("paper_${paperNum}", "color", activity.packageName), null)
            cornerDrawable = ResourcesCompat.getDrawable(it,
                it.getIdentifier("${frameName}_1", "drawable", activity.packageName), null)
            borderDrawable = ResourcesCompat.getDrawable(it,
                it.getIdentifier("${frameName}_2", "drawable", activity.packageName), null)
        }

        if (!File(activity.filesDir, "${frameName}_1").exists())
            savePNGFromVectorDrawable(cornerDrawable, "${frameName}_1")
        if (!File(activity.filesDir, "${frameName}_2").exists())
            savePNGFromVectorDrawable(borderDrawable, "${frameName}_2")
    }

    private fun jpgInitialize() {
        w = sharedPreference.getInt("pic_width", 2000)
        edge = 0.05f * w
        verseSeparation = (if (stvm.fontPref == 0) 0.12f else 0.08f) * w
        d = (w - 2*edge)/(m + 2*aspectRatio)
        h = (hTOd[frameNumber] * d).toFloat()               //(163.3/155.7 * d)
        l = aspectRatio * d

        mesraContainerWidth = (w - 2*(edge + h) - verseSeparation - 0.04f*w)/2
        paragraphWidth = w - 2*(edge + h) - 0.04f*w
        rightVerseEnd = (w + verseSeparation)/2
        leftVerseBegin = (w - verseSeparation)/2
        paragraphBegin = (w + paragraphWidth)/2

        val rawFontSize = w/100
        initialFontSize = rawFontSize.spTOpx(activity.resources) * when {
            mesraMaxWidth * rawFontSize /stvm.fontSize < mesraContainerWidth -> 1f
            else -> mesraContainerWidth * stvm.fontSize / (mesraMaxWidth * rawFontSize)
        }

        textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            style = Paint.Style.FILL
            typeface = stvm.font
            textSize = initialFontSize
            color = ResourcesCompat.getColor(activity.resources, R.color.black_900, activity.theme)
        }
        titlePaint = Paint(textPaint).apply {
            textSize = initialFontSize * 1.1f
            textAlign = Paint.Align.CENTER
            if (stvm.fontPref != 0)
                typeface = Typeface.create(stvm.font, Typeface.BOLD)
        }
        textPaint2 = TextPaint(textPaint)

        val rect = Rect()
        textPaint.getTextBounds("کیخسرو آخ گچ", 0, 11, rect)
        textHeight = rect.height()
        paragVerticalSep = if (stvm.fontPref == 0) 0f else verticalSep * textHeight / 5f

        var i = 0
        while (i < verses.size){
            when (verses[i].position){
                0 -> {
                    poemHeight += (verticalSep * textHeight).toInt()
                    if (verses[i + 1].position == 1)
                        i++
                }
//                2 -> poemHeight += textHeight
                2, 3 -> poemHeight += (verticalSep * textHeight).toInt()
                else -> {
                    val source = activity.getString(R.string.tabbedText, verses[i].text?.trim())
                    staticLayout = StaticLayout.Builder.obtain(
                        source, 0, source.length, textPaint2, paragraphWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, verticalSep)
                        .build()

                    poemHeight += staticLayout.height + paragVerticalSep.toInt()
                }
            }
            i++
        }

        title?.let {
            staticLayout = StaticLayout.Builder.obtain(
                it, 0, it.length, TextPaint(titlePaint), paragraphWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, verticalSep)
                .build()
            poemHeight += staticLayout.height + (3 * verticalSep * textHeight/2).toInt()
        }

        poemHeight += (w/4.5f).toInt()
        n = ((poemHeight - 2*(l - h))/d).roundToInt()
        poemHeight = (2*(l - h) + n*d).toInt()
    }

    private fun pdfInitialize(width: Int){
        w = width
        edge = 0.05f * w
        verseSeparation = (if (stvm.fontPref == 0) 0.12f else 0.08f) * w
        d = (w - 2*edge)/(m + 2*aspectRatio)
        h = (hTOd[frameNumber] * d).toFloat()               //(163.3/155.7 * d)
        l = aspectRatio * d

        mesraContainerWidth = (w - 2*(edge + h) - verseSeparation - 0.04f*w)/2
        paragraphWidth = w - 2*(edge + h) - 0.04f*w
        rightVerseEnd = (w + verseSeparation)/2
        leftVerseBegin = (w - verseSeparation)/2
        paragraphBegin = (w + paragraphWidth)/2

        val rawFontSize = 6
        initialFontSize = rawFontSize.spTOpx(activity.resources) * when {
            mesraMaxWidth * rawFontSize /stvm.fontSize < mesraContainerWidth -> 1f
            else -> mesraContainerWidth * stvm.fontSize / (mesraMaxWidth * rawFontSize)
        }

        textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            style = Paint.Style.FILL
            typeface = stvm.font
            textSize = initialFontSize
            color = ResourcesCompat.getColor(activity.resources, R.color.black_900, activity.theme)
        }
        titlePaint = Paint(textPaint).apply {
            textSize = initialFontSize * 1.1f
            textAlign = Paint.Align.CENTER
            if (stvm.fontPref != 0)
                typeface = Typeface.create(stvm.font, Typeface.BOLD)
        }
        textPaint2 = TextPaint(textPaint)

        val rect = Rect()
        textPaint.getTextBounds("کیخسرو آخ گچ", 0, 11, rect)
        textHeight = rect.height()
        paragVerticalSep = if (stvm.fontPref == 0) 0f else verticalSep * textHeight / 2f
    }


    fun exportToJPG(saveInSharedStorage: Boolean): Boolean {
        if (verses.isNotEmpty()) {
            jpgInitialize()

            val verseWidth = textPaint.apply {textScaleX = 1f}.measureText(verses[mesraMaxKey ?: 0].text)
            val poemPicture = Bitmap.createBitmap(
                w, poemHeight + 2 * (edge + h).toInt(), Bitmap.Config.ARGB_8888)


            val canvas = Canvas(poemPicture).apply {
//                drawFilter = PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG, Paint.ANTI_ALIAS_FLAG)
                drawColor(paperColor)

                var y = edge + h
                title?.let {
                    y = w / 4.5f
                    staticLayout = StaticLayout.Builder.obtain(
                        it, 0, it.length, TextPaint(titlePaint), paragraphWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, verticalSep)
                        .build()

                    for (i in 0 until staticLayout.lineCount) {
                        titlePaint.textScaleX = 1f
                        var line = it.substring(
                            staticLayout.getLineStart(i), staticLayout.getLineEnd(i)
                        )
                        line = if (i != 0) line.trim() else line.trimEnd()
                        val baseLine: Int = staticLayout.getLineBaseline(i)

                        drawText(line, w/2f, y + baseLine, titlePaint.apply {
                            textScaleX = if (i < staticLayout.lineCount - 1)
                                paragraphWidth / measureText(line) else 1f
                            textAlign = Paint.Align.CENTER
                        })
                    }

                    y += staticLayout.height + verticalSep * textHeight/2
                }

                verses.forEach { elem ->
                    val vWidth = textPaint.apply {textScaleX = 1f}.measureText(elem.text)
                    when (elem.position){
                        0 -> {
                            y += verticalSep * textHeight
                            if (vWidth > verseWidth) {
                                drawText(elem.text!!.trim(), rightVerseEnd, y, textPaint.apply {
                                    textScaleX = verseWidth / vWidth
                                    textAlign = Paint.Align.LEFT
                                })
                            } else {
                                val normalizedVerse = verseWidthNormalizer(
                                    elem.text!!.trim(), vWidth, verseWidth
                                )
                                val scaleX = verseWidth / textPaint.measureText(normalizedVerse)
                                this.scale(scaleX, 1f)
                                drawText(normalizedVerse, rightVerseEnd / scaleX, y, textPaint.apply {
//                                    textScaleX = verseWidth / this.measureText(normalizedVerse)
                                    textAlign = Paint.Align.LEFT
                                })
                                this.scale(1 / scaleX, 1f)
                            }
                        }
                        1 -> {
                            if (vWidth > verseWidth)
                                drawText(elem.text!!.trim(), leftVerseBegin, y, textPaint.apply {
                                    textScaleX = verseWidth / vWidth
                                    textAlign = Paint.Align.RIGHT
                                })
                            else {
                                val normalizedVerse = verseWidthNormalizer(
                                    elem.text!!.trim(), vWidth, verseWidth
                                )
                                val scaleX = verseWidth / textPaint.measureText(normalizedVerse)
                                this.scale(scaleX, 1f)
                                drawText(normalizedVerse, leftVerseBegin / scaleX, y, textPaint.apply {
//                                    textScaleX = verseWidth / this.measureText(normalizedVerse)
                                    textAlign = Paint.Align.RIGHT
                                })
                                this.scale(1 / scaleX, 1f)

                            }
                        }
                        2 -> {
                            y += verticalSep * textHeight
                            if (vWidth > verseWidth)
                                drawText(elem.text!!.trim(), w / 2f, y, textPaint.apply {
                                    textScaleX = verseWidth / vWidth
                                    textAlign = Paint.Align.CENTER
                                })
                            else {
                                val normalizedVerse = verseWidthNormalizer(
                                    elem.text!!.trim(), vWidth, verseWidth
                                )
                                val scaleX = verseWidth / textPaint.measureText(normalizedVerse)
                                this.scale(scaleX, 1f)
                                drawText(normalizedVerse, w / (2 * scaleX), y, textPaint.apply {
//                                    textScaleX = verseWidth / this.measureText(normalizedVerse)
                                    textAlign = Paint.Align.CENTER
                                })
                                this.scale(1 / scaleX, 1f)
                            }
                        }
                        3 -> {
                            y += verticalSep * textHeight
                            if (vWidth > verseWidth)
                                drawText(elem.text!!.trim(), w / 2f, y, textPaint.apply {
                                    textScaleX = verseWidth / vWidth
                                    textAlign = Paint.Align.CENTER
                                })
                            else {
                                val normalizedVerse = verseWidthNormalizer(
                                    elem.text!!.trim(), vWidth, verseWidth
                                )
                                val scaleX = verseWidth / textPaint.measureText(normalizedVerse)
                                this.scale(scaleX, 1f)
                                drawText(normalizedVerse, w / (2 * scaleX), y, textPaint.apply {
//                                    textScaleX = verseWidth / this.measureText(normalizedVerse)
                                    textAlign = Paint.Align.CENTER
                                })
                                this.scale(1 / scaleX, 1f)
                            }

                        }
                        else -> {
                            val source = activity.getString(R.string.tabbedText, elem.text)
                            staticLayout = StaticLayout.Builder.obtain(
                                source, 0, source.length, textPaint2, paragraphWidth.toInt())
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .setLineSpacing(0f, verticalSep)
                                .build()

                            y += verticalSep * textHeight / 2f + paragVerticalSep
                            for (i in 0 until staticLayout.lineCount) {
                                var line = source.substring(
                                    staticLayout.getLineStart(i), staticLayout.getLineEnd(i)
                                )
                                line = if (i != 0) line.trim() else line.trimEnd()
                                val baseLine: Int = staticLayout.getLineBaseline(i)

                                val scaleX = if (i < staticLayout.lineCount - 1)
                                    paragraphWidth / textPaint2.measureText(line) else 1f
                                this.scale(scaleX, 1f)
                                drawText(line, paragraphBegin / scaleX, y + baseLine, textPaint.apply {
//                                    textScaleX = if (i < staticLayout.lineCount - 1)
//                                        paragraphWidth / textPaint2.measureText(line) else 1f
                                    textAlign = Paint.Align.RIGHT
                                })
                                this.scale(1 / scaleX, 1f)
                            }
                            y += (staticLayout.height - verticalSep * textHeight / 2f + paragVerticalSep)
                        }
                    }
                }
                y = poemHeight + h + edge - w/25
//                val reference = if (itemRoot.size == 2) "${itemRoot[0]} ${itemRoot[1]?.substringBefore("*")}"
//                    else "${itemRoot[1]} ${itemRoot[2]?.substringBefore("*")}، ${itemRoot[0]}"
                val reference =
                    itemRoot.reversed().joinToString("، ") { it?.substringBefore('*') ?: "" }

                drawText(reference, paragraphBegin, y , textPaint.apply {
                    textAlign = Paint.Align.RIGHT
                    typeface = ResourcesCompat.getFont(activity, R.font.iransans_light)
                    textSize = (w/180).spTOpx(activity.resources)
                })

                drawText(activity.resources.getString(R.string.produced), paragraphBegin, y+w/50, textPaint)

            }
            drawFrame(canvas)

            if (saveInSharedStorage){
                val dir = File(Environment.DIRECTORY_PICTURES)
                val pickerInitialUri = Uri.fromFile(dir)

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_TITLE, "$title.jpg")

                    // Optionally, specify a URI for the directory that should be opened in
                    // the system file picker before your app creates the document.
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
                }
                activity.startActivityForResult(intent, 1)

                val pvm = (activity as MainActivity).poemViewModel
                pvm.savePoem.observe(activity, object : Observer<Boolean?> {
                    override fun onChanged(t: Boolean?) {
                        if (t == true){
                            pvm.poemFileUri?.let { uri ->
                                val jpegOutFile = activity.contentResolver.openOutputStream(uri)
                                try {
                                    poemPicture.compress(Bitmap.CompressFormat.JPEG, 100, jpegOutFile)
                                    if (stvm.openExportFile) activity.openPoemFile(uri)
                                } catch (e: IOException) {
                                    Timber.e("JPG file did not created successfully")
                                }
                            }
                        }
                        if (t != null) {
                            pvm.doneSavePoemToFile()
                            pvm.savePoem.removeObserver(this)
                        }
                    }
                })
                return false
            } else {
                File(activity.filesDir, "poem").let { if (!it.exists()) it.mkdir() }
                val jpegOutFile = FileOutputStream("${activity.filesDir}/poem/$title.jpg")
//            val jpegOutFile = FileOutputStream("${context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)}/PoemPic.jpg")
                return poemPicture.compress(Bitmap.CompressFormat.JPEG, 100, jpegOutFile)
            }



        } else
            return false
    }

    fun exportToPDF(saveInSharedStorage: Boolean): Boolean{
        if (verses.isNotEmpty()) {
            pdfInitialize(595)
            val height = 842
            val borderTextPaint = Paint(textPaint).apply {
                textAlign = Paint.Align.RIGHT
                typeface = ResourcesCompat.getFont(activity, R.font.iransans_light)
                textSize = (w/180).spTOpx(activity.resources)
            }
//            val reference =
//                if (itemRoot.size == 2) "${itemRoot[0]} ${itemRoot[1]?.substringBefore("*")}"
//                else "${itemRoot[1]} ${itemRoot[2]?.substringBefore("*")}، ${itemRoot[0]}"
            val reference =
                itemRoot.reversed().joinToString("، ") { it?.substringBefore('*') ?: "" }

            val document = PdfDocument()
            val verseWidth = textPaint.apply {textScaleX = 1f}.measureText(verses[mesraMaxKey ?: 0].text)

            var pageNumber = 0
            var verseCounter = 0
            var pageInfo: PdfDocument.PageInfo
            var page: PdfDocument.Page? = null

            var pageExist = false
            var y = 0f

            while (verseCounter < verses.size){
                if (!pageExist) {
                    pageNumber++
                    y = edge + h
                    pageInfo = PdfDocument.PageInfo.Builder(w, height, pageNumber).create()
                    page = document.startPage(pageInfo)
                    page.canvas.apply {
                        drawColor(Color.WHITE)
                        drawPageFrame(this, height)
                        if (pageNumber == 1) {
                            y += w/25
                            drawText(reference, paragraphBegin, y , borderTextPaint)
                            title?.let {
                                y = w / 3.5f
                                staticLayout = StaticLayout.Builder.obtain(
                                    it, 0, it.length, TextPaint(titlePaint), paragraphWidth.toInt())
                                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                    .setLineSpacing(0f, verticalSep)
                                    .build()

                                for (i in 0 until staticLayout.lineCount) {
                                    titlePaint.textScaleX = 1f
                                    var line = it.substring(
                                        staticLayout.getLineStart(i), staticLayout.getLineEnd(i)
                                    )
                                    line = if (i != 0) line.trim() else line.trimEnd()
                                    val baseLine: Int = staticLayout.getLineBaseline(i)

                                    drawText(line, w/2f, y + baseLine, titlePaint.apply {
                                        textScaleX = if (i < staticLayout.lineCount - 1)
                                            paragraphWidth / measureText(line) else 1f
                                        textAlign = Paint.Align.CENTER
                                    })
                                }

                                y += staticLayout.height + verticalSep * textHeight/2
                            }
                        }
                        rotate(-90f)
                        drawText(activity.resources.getString(R.string.produced)
                            , -height+edge, w/25f , borderTextPaint.apply { textAlign = Paint.Align.LEFT })
                        rotate(90f)
                    }
                }

                while (y < height-(edge + h + verticalSep*1.5f*textHeight) || verses[verseCounter].position == 1){
                    pageExist = false
                    val vWidth = textPaint.apply {textScaleX = 1f}.measureText(verses[verseCounter].text)
                    var pageCanvas = page?.canvas!!
                    when (verses[verseCounter].position){
                            0 -> {
                                y += verticalSep * textHeight
                                if (vWidth > verseWidth) {
                                    pageCanvas.drawText(verses[verseCounter].text!!.trim(), rightVerseEnd, y,
                                        textPaint.apply {
                                            textScaleX = verseWidth / vWidth
                                            textAlign = Paint.Align.LEFT
                                        }
                                    )
                                } else {
                                    pageCanvas.drawText(verseWidthNormalizer(
                                        verses[verseCounter].text!!.trim(),
                                        vWidth,
                                        verseWidth
                                    ), rightVerseEnd, y, textPaint.apply {
                                        textScaleX = 1f
                                        textAlign = Paint.Align.LEFT
                                    })
                                }
                            }
                            1 -> {
                                if (vWidth > verseWidth)
                                    pageCanvas.drawText(verses[verseCounter].text!!.trim(), leftVerseBegin, y, textPaint.apply {
                                        textScaleX = verseWidth / vWidth
                                        textAlign = Paint.Align.RIGHT
                                    })
                                else
                                    pageCanvas.drawText(verseWidthNormalizer(
                                        verses[verseCounter].text!!.trim(),
                                        vWidth,
                                        verseWidth
                                    ), leftVerseBegin, y, textPaint.apply {
                                        textScaleX = 1f
                                        textAlign = Paint.Align.RIGHT
                                    })
                            }
                            2 -> {
                                y += verticalSep * textHeight
                                if (vWidth > verseWidth)
                                    pageCanvas.drawText(verses[verseCounter].text!!.trim(), w / 2f, y, textPaint.apply {
                                        textScaleX = verseWidth / vWidth
                                        textAlign = Paint.Align.CENTER
                                    })
                                else
                                    pageCanvas.drawText(verseWidthNormalizer(
                                        verses[verseCounter].text!!.trim(),
                                        vWidth,
                                        verseWidth
                                    ), w / 2f, y, textPaint.apply {
                                        textScaleX = 1f
                                        textAlign = Paint.Align.CENTER
                                    })
                            }
                            3 -> {
                                y += verticalSep * textHeight
                                if (vWidth > verseWidth)
                                    pageCanvas.drawText(verses[verseCounter].text!!.trim(), w / 2f, y, textPaint.apply {
                                        textScaleX = verseWidth / vWidth
                                        textAlign = Paint.Align.CENTER
                                    })
                                else
                                    pageCanvas.drawText(verseWidthNormalizer(
                                        verses[verseCounter].text!!.trim(),
                                        vWidth,
                                        verseWidth
                                    ), w / 2f, y, textPaint.apply {
                                        textScaleX = 1f
                                        textAlign = Paint.Align.CENTER
                                    })
                            }
                            else -> {
                                val source = activity.getString(R.string.tabbedText, verses[verseCounter].text)
                                staticLayout = StaticLayout.Builder.obtain(
                                    source, 0, source.length, textPaint2, paragraphWidth.toInt())
                                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                    .setLineSpacing(0f, verticalSep)
                                    .build()

                                y += verticalSep * textHeight/2 + paragVerticalSep
                                for (i in 0 until staticLayout.lineCount) {
                                    var line = source.substring(
                                        staticLayout.getLineStart(i), staticLayout.getLineEnd(i)
                                    )
                                    line = if (i != 0) line.trim() else line.trimEnd()
                                    val baseLine: Int = staticLayout.getLineBaseline(i)
                                    textPaint.apply {
                                        textScaleX = 1f
                                        textAlign = Paint.Align.RIGHT
                                    }

                                    if (y + baseLine < height-(edge+h+textHeight/2)){
                                        pageCanvas.drawText(if (i < staticLayout.lineCount - 1)
                                            verseWidthNormalizer(line, textPaint.measureText(line), paragraphWidth) else line
                                            , paragraphBegin, y + baseLine, textPaint)
                                    } else {
                                        document.finishPage(page)
                                        pageNumber++
                                        pageExist = true
                                        y = edge + h + verticalSep * textHeight - baseLine
                                        pageInfo = PdfDocument.PageInfo.Builder(w, height, pageNumber).create()
                                        page = document.startPage(pageInfo)
                                        pageCanvas = page?.canvas!!.apply {
                                            drawColor(Color.WHITE)
                                            drawPageFrame(this, height)
                                            drawText(if (i < staticLayout.lineCount - 1)
                                                verseWidthNormalizer(line, textPaint.measureText(line), paragraphWidth) else line
                                                , paragraphBegin, y + baseLine, textPaint)

                                            rotate(-90f)
                                            drawText(activity.resources.getString(R.string.produced)
                                                , -height+edge, w/25f , borderTextPaint.apply { textAlign = Paint.Align.LEFT })
                                            rotate(90f)
                                        }
                                    }
                                }
                                y += (staticLayout.height - verticalSep * textHeight / 2f + paragVerticalSep)
                            }
                        }

                    verseCounter++
                    if (verseCounter >= verses.size)
                        break

                }
                document.finishPage(page)
            }

            if (saveInSharedStorage) {
                val dir = File(Environment.DIRECTORY_DOCUMENTS)
//                if (!dir.exists()) dir.mkdir()
                val pickerInitialUri = Uri.fromFile(dir)

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_TITLE, "$title.pdf")

                    // Optionally, specify a URI for the directory that should be opened in
                    // the system file picker before your app creates the document.
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
                }
                activity.startActivityForResult(intent, 1)

                val pvm = (activity as MainActivity).poemViewModel
                pvm.savePoem.observe(activity, object : Observer<Boolean?> {
                    override fun onChanged(t: Boolean?) {
                        if (t == true){
                            pvm.poemFileUri?.let { uri ->
                                val pdfOutFile = activity.contentResolver.openOutputStream(uri)
                                try {
                                    document.writeTo(pdfOutFile)
                                    document.close()
                                    if (stvm.openExportFile) activity.openPoemFile(uri)
                                } catch (e: IOException) {
                                    Timber.e("PDF file did not created successfully")
                                }
                            }
                        }
                        if (t != null) {
                            pvm.doneSavePoemToFile()
                            pvm.savePoem.removeObserver(this)
                        }
                    }
                })
                return false
            } else {
                File(activity.filesDir, "poem").let { if (!it.exists()) it.mkdir() }
                val pdfOutFile = FileOutputStream("${activity.filesDir}/poem/$title.pdf")
                return try {
                    document.writeTo(pdfOutFile)
                    document.close()
                    true
                } catch (e: IOException) {
                    Timber.e("PDF file did not created successfully")
                    false
                }
            }
        } else
            return false
    }

    fun exportToText(saveInSharedStorage: Boolean): Boolean {
        val reference =
            itemRoot.reversed().joinToString("، ") { it?.substringBefore('*') ?: "" }

        if (verses.isNotEmpty()) {
            var sher = ""
            verses.forEach { verse ->
                sher += if (verse.verseOrder == 1) verse.text
                else {
                    if (verse.position == 0 || verse.position == 2) "\n \n ${verse.text}"
                    else "\n ${verse.text}"
                }
            }
            sher += "\n \n \n ${activity.getString(R.string.email_subject_2, title, reference)}"

            if (saveInSharedStorage) {
                val dir = File(Environment.DIRECTORY_DOCUMENTS)
                val pickerInitialUri = Uri.fromFile(dir)

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, "$title.txt")

                    // Optionally, specify a URI for the directory that should be opened in
                    // the system file picker before your app creates the document.
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
                }
                activity.startActivityForResult(intent, 1)

                val pvm = (activity as MainActivity).poemViewModel
                pvm.savePoem.observe(activity, object : Observer<Boolean?> {
                    override fun onChanged(t: Boolean?) {
                        if (t == true) {
                            pvm.poemFileUri?.let { uri ->
                                val textOutFile = activity.contentResolver.openOutputStream(uri)
                                try {
                                    val outputStreamWriter = OutputStreamWriter(textOutFile)
                                    outputStreamWriter.write(sher)
                                    outputStreamWriter.close()
                                    if (stvm.openExportFile) activity.openPoemFile(uri)
                                } catch (e: IOException) {
                                    Timber.e("Text file did not created successfully")
                                }
                            }
                        }
                        if (t != null) {
                            pvm.doneSavePoemToFile()
                            pvm.savePoem.removeObserver(this)
                        }
                    }
                })
                return false
            } else {
                File(activity.filesDir, "poem").let { if (!it.exists()) it.mkdir() }
                val textOutFile = FileOutputStream("${activity.filesDir}/poem/$title.txt")
                return try {
                    val outputStreamWriter = OutputStreamWriter(textOutFile)
                    outputStreamWriter.write(sher)
                    outputStreamWriter.close()
                    true
                } catch (e: IOException) {
                    Timber.e("Text file did not created successfully")
                    false
                }
            }
        }
        return false
    }


    private fun verseWidthNormalizer(inString: String, initialWidth: Float, finalWidth: Float): String{

        when (stvm.fontPref){
            0 -> {
                val spaceCount = inString.count { char -> char == ' ' }
                if (spaceCount == 0 || initialWidth >= finalWidth)
                    return  inString
                else {
                    val spaceWidth = textPaint.apply {textScaleX = 1f}.measureText(" ")
                    val numSpaceNeeded = (finalWidth - initialWidth) / spaceWidth
                    if (numSpaceNeeded <= 0)
                        return inString
                    val addingSpaces: Int = (numSpaceNeeded / spaceCount).toInt() + 1
                    val extraSpaces = (addingSpaces * spaceCount - numSpaceNeeded).toInt()
                    var newString = inString.replace(" ", " ".repeat(addingSpaces + 1))
                    var j = 0
                    for (i in 1..extraSpaces) {
                        while (j < newString.length && newString[j] != ' ')
                            j++
                        newString = newString.removeRange(j, j + 1)
                        j += addingSpaces
                    }
                    return newString
                }
            }
            else -> {
                val splittedString = inString.split(' ').toMutableList()
                val dashableCount = splittedString.count { it.charFinder(goalChars) }

                if (dashableCount == 0 || initialWidth >= finalWidth)
                    return  inString
                else {
                    val dashWidth = textPaint.apply {textScaleX = 1f}.measureText("ـ")
                    val numDashNeeded = ((finalWidth - initialWidth) / dashWidth)
                    val addingDashes: Int = (numDashNeeded / dashableCount).toInt() + 1
                    val extraDashes = (addingDashes * dashableCount - numDashNeeded).toInt()
                    val dashStringLong = "ـ".repeat(addingDashes)
                    val dashStringShort = "ـ".repeat(addingDashes - 1)

                    var counter = 0
                    for (j in splittedString.indices){
//                        val stringBiErab = makeTextBiErab(splittedString[j])
//                        val l = stringBiErab.indexOfAny(goalChars.toCharArray())
                        val k = splittedString[j].myIndexOfAny(goalChars.toCharArray())

                        if (k != -1 /*&& k < splittedString[j].length - 1 && splittedString[j][k+1] != '‌' &&
                            l < stringBiErab.length-1*/){
                            if (counter <= extraDashes)
                                splittedString[j] = StringBuilder(splittedString[j]).insert(k+1,
                                    dashStringShort).toString()
                            else
                                splittedString[j] = StringBuilder(splittedString[j]).insert(k+1,
                                    dashStringLong).toString()
                            counter++
                        }
                    }

                    return splittedString.joinToString(" ")
                }
            }
        }
    }

    private fun drawFrame(canvas: Canvas){
        val cornerImage = getBitmapFromPNGFile("${frameName}_1")
        val borderImage = getBitmapFromPNGFile("${frameName}_2")

        if (cornerImage == null || borderImage == null)
            return
        for (i in 1 .. 4){
            val left = when(i){
                1 -> edge
                2 -> edge - (w - 2 * edge)
                3 -> edge - (poemHeight + 2 * h)
                else -> edge
            }
            val top = when(i){
                1 -> edge - (w - 2 * edge)
                2 -> edge - (poemHeight + 2 * h)
                3 -> edge
                else -> edge
            }
            canvas.rotate(90f, edge, edge)
            canvas.drawBitmap(
                cornerImage, Rect(0, 0, cornerImage.width-2, cornerImage.height-2)
                , RectF(left, top, (left+l).toFloat(), (top+l).toFloat()), Paint())

            val end = if (i%2 == 0) m else n
            for (j in 1 .. end){
                canvas.drawBitmap(borderImage, Rect(2, 0, borderImage.width-2, borderImage.height)
                    , RectF((left + l + (j - 1) * d - 1).toFloat(), top, (left + l + j * d + 1).toFloat()
                        , top + h), Paint())
            }
        }
    }

    private fun drawPageFrame(canvas: Canvas, height: Int) {
        val cornerImage = getBitmapFromPNGFile("${frameName}_1")
        val borderImage = getBitmapFromPNGFile("${frameName}_2")

        if (cornerImage == null || borderImage == null)
            return
        for (i in 1 .. 4){
            val left = when(i){
                1 -> edge
                2 -> edge - (w - 2 * edge)
                3 -> 3*edge - height
                else -> edge
            }
            val top = when(i){
                1 -> edge - (w - 2 * edge)
                2 -> 3*edge - height
                3 -> edge
                else -> edge
            }
            canvas.rotate(90f, edge, edge)
            canvas.drawBitmap(
                cornerImage, Rect(0, 0, cornerImage.width-2, cornerImage.height-2)
                , RectF(left, top, (left+l).toFloat(), (top+l).toFloat()), Paint())

            val n = ((height - 2*edge - 2*l)/d).roundToInt()
            val scale = if (i%2 == 0) 1.0 else (height - 2*edge - 2*l)/(n*d)
            val end = if (i%2 == 0) m else n

            for (j in 1 .. end) {
                canvas.drawBitmap(borderImage, Rect(2, 0, borderImage.width-2, borderImage.height)
                    , RectF((left + l + (j - 1) * d * scale).toFloat(), top
                        , (left + l + j * d * scale).toFloat(), (top + h)), Paint())
            }
        }
    }

//    private fun getBitmapFromVectorDrawable(drawable: Drawable?): Bitmap? {
//        drawable?.let {
//            val bitmap = Bitmap.createBitmap(
//                4*it.intrinsicWidth,
//                4*it.intrinsicHeight, Bitmap.Config.ARGB_8888
//            )
//            val canvas = Canvas(bitmap)
//            it.setBounds(0, 0, canvas.width, canvas.height)
//            it.draw(canvas)
//            return bitmap
//        }
//        return null
//    }

    private fun savePNGFromVectorDrawable(drawable: Drawable?, fileName: String) {
        drawable?.let {
            val bitmap = Bitmap.createBitmap(
                2*it.intrinsicWidth,2*it.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)

            val pngOutFile = FileOutputStream("${activity.filesDir}/${fileName}.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngOutFile)
        }
    }

    private fun getBitmapFromPNGFile(name: String): Bitmap? {
        return BitmapFactory.decodeFile("${activity.filesDir}/${name}.png")
    }

}