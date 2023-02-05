package com.takaapoo.adab_parsi.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.widget.EdgeEffect
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.play.core.review.ReviewManagerFactory
import com.takaapoo.adab_parsi.AppStore
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.TempCategory
import com.takaapoo.adab_parsi.database.TempVerse
import com.takaapoo.adab_parsi.database.Verse
import com.takaapoo.adab_parsi.setting.SettingViewModel
import timber.log.Timber
import java.text.Collator
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt


val collator: Collator = Collator.getInstance(Locale("fa"))
var allCategory = emptyList<Category>()
    set(value) {
        field = value.sortedWith { one, two ->
            collator.compare(one.text, two.text)
        }
    }
val appStore = AppStore.GooglePlay

var topPadding = 0


fun Int.spTOpx(resources: Resources) = this * resources.displayMetrics.scaledDensity

fun Int.dpTOpx(resources: Resources) = this * resources.displayMetrics.density

@ColorInt
fun Context.getColorFromAttr(@AttrRes attrColor: Int, typedValue: TypedValue = TypedValue(),
                             resolveRefs: Boolean = true): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return ContextCompat.getColor(this, typedValue.resourceId)
}

fun Context.getDimenFromAttr(@AttrRes attrDimension: Int): Int {
    val styledAttributes = theme.obtainStyledAttributes(intArrayOf(attrDimension))
    val dimension = styledAttributes.getDimension(0, 0f).toInt()
    styledAttributes.recycle()
    return dimension
}

fun Fragment.barsPreparation(){
//    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
//    val myActivity = requireActivity()
//
//    myActivity.window.decorView.systemUiVisibility =
//        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//    if (currentNightMode == Configuration.UI_MODE_NIGHT_NO)
//        myActivity.window.decorView.systemUiVisibility = myActivity.window.decorView.systemUiVisibility or
//                (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
}

fun getAllVerseBiErab(tempVerses:  List<TempVerse>): List<Verse> =
    tempVerses.map { Verse(it.poem_id, it.verseOrder, it.position, it.text,
        makeTextBiErab(it.text), null, null, null) }

fun tempCatToCat(tempCats:  List<TempCategory>): List<Category> =
    tempCats.map { Category(it.id, it.ancient, it.poetID, it.text, it.parentID, it.url,
        Calendar.getInstance().timeInMillis, it.version) }



fun makeTextBiErab(text: String?): String {
    val deletChars = "ءًٌٍَُِّْٔ،!؛:؟.\u200C"
    val outText = text?.filterNot { c -> deletChars.contains(c) }?.map { c -> when (c) {
        'إ', 'أ', 'آ' -> 'ا'
        'ة', 'ۀ' -> 'ه'
        'ؤ' -> 'و'
        'ئ' -> 'ی'
        else -> c
    } }
    return if (outText.isNullOrEmpty()) "" else String(outText.toCharArray())
}

fun String.mySubString(firstSpaceNum: Int, secondSpaceNum: Int): String {
    var startIndex = if (firstSpaceNum == 0) 0 else -1
    for (i in 1 .. firstSpaceNum)
        startIndex = this.indexOf(' ', startIndex+1)

    var endIndex = startIndex
    for (i in firstSpaceNum+1 .. secondSpaceNum)
        endIndex = this.indexOf(' ', endIndex+1)

    return if (endIndex == startIndex) {
        if (startIndex != 0) this.substring(startIndex + 1)
        else this.substring(startIndex)
    } else this.substring(if (startIndex == 0) startIndex else startIndex + 1, endIndex)
}

fun widthNormalizer(inString: String, initialWidth: Int, finalWidth: Int, stvm: SettingViewModel): String{
    when (stvm.fontPref){
        0 -> {
            val spaceCount = inString.count { char -> char == ' ' }
            if (spaceCount == 0 || initialWidth >= finalWidth)
                return  inString
            else {
                val numSpaceNeeded = ((finalWidth - initialWidth) / stvm.spaceWidth)
                val addingSpaces: Int = (numSpaceNeeded / spaceCount).toInt() + 1
                val extraSpaces = (addingSpaces * spaceCount - numSpaceNeeded).roundToInt()
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
                val numDashNeeded = ((finalWidth - initialWidth) / stvm.dashWidth)
                val addingDashes: Int = (numDashNeeded / dashableCount).toInt() + 1
                val extraDashes = (addingDashes * dashableCount - numDashNeeded).roundToInt()
                val dashStringLong = "ـ".repeat(addingDashes)
                val dashStringShort = "ـ".repeat(addingDashes - 1)

                var counter = 0
                for (j in splittedString.indices){
//                    val stringBiErab = makeTextBiErab(splittedString[j])
//                    val l = stringBiErab.indexOfAny(goalChars.toCharArray())
                    val k = splittedString[j].myIndexOfAny(goalChars.toCharArray())

                    if (k != -1 /*&& k < splittedString[j].length - 1 && splittedString[j][k+1] != '‌' &&
                            l < stringBiErab.length-1*/){
                        if (counter < extraDashes)
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

fun String.charFinder(charContainer: String): Boolean {
//    val deletChars = "ءًٌٍَُِّْٔ،!؛:؟.\u200C"
    val myString = makeTextBiErab(this)

    for (i in 0 until myString.length-1){
        if (charContainer.contains(myString[i]) && myString[i+1] != '‌'){
            if (myString[i] != 'ل' || myString[i+1] != 'ا')
                return true
        }
    }
    return false
}

fun String.myIndexOfAny(chars: CharArray): Int{
    val aa = "إأآا"
    var index: Int
    var startIndex = 0
    while (startIndex < this.length - 1){
        index = this.indexOfAny(chars, startIndex)
        when (index){
            -1 -> return -1
            else -> {
                when {
                    index == this.length - 1 -> return  -1
                    this[index + 1] == '‌' -> startIndex = index + 1
                    makeTextBiErab(this.substring(index + 1)).isEmpty() -> return -1
                    (this[index] == 'ل' && aa.contains(this[index + 1]) ||
                            index < this.length - 2 && makeTextBiErab(this.substring(index, index+3)) == "لا" )
                        -> startIndex = index + 1
                    else -> return index
                }
            }
        }
    }
    return -1
}

const val goalChars = "بپتثجچحخسشصضطظعغفقکگلمنهی"



fun engNumToFarsiNum(num: Int): String =
    NumberFormat.getInstance(Locale("ar", "EG")).format(num)

fun List<String>.allIndexOf(element: String): List<Int>{
    var index = -1
    val allIndex = mutableListOf<Int>()

    var i = 0
    while (i != -1){
        i = this.filterIndexed { ind, _ -> ind > index }.myIndexOf(element)
        index += i + 1
        if (i != -1)
            allIndex.add(index)
    }
    return allIndex
}

fun List<String>.myIndexOf(element: String): Int{
    for (i in 0 until this.count()){
        if (this.elementAt(i).startsWith(element))
            return i
    }
    return -1
}


fun String.findSpanIndex2(elements: List<String>): List<Int>?{
    if (this.trim().isEmpty() || elements.isEmpty()) return null
    val input = String(this.map { c -> when (c) {
        'إ', 'أ', 'آ' -> 'ا'
        'ة', 'ۀ' -> 'ه'
        'ؤ' -> 'و'
        'ئ' -> 'ی'
        else -> c }
    }.toCharArray())

    val notConsidered = "ءًٌٍَُِّْٔ\u200Cـ"
    val notConsidered2 = " ،!؛:؟."

    val outList = mutableListOf(0)

    var elemIndices = (elements.indices).toMutableList()
    var i = input.indexOfFirst { c -> !c.isWhitespace() }
    var k = 0
    var startIndex = i
    var endIndex = 0
    while (i < input.length){
        var j = 0

        while (j < elemIndices.size){
            if (k == elements[elemIndices[j]].length)
                endIndex = i

            if (k < elements[elemIndices[j]].length && input[i] != elements[elemIndices[j]][k])
                elemIndices.removeAt(j)
            else
                j++
        }
        i++
        k++
        while (i < input.length && notConsidered.contains(input[i]))
            i++

        if (i == input.length || notConsidered2.contains(input[i])){
            for (l in elemIndices)
                if (k == elements[l].length)
                    endIndex = i

            if (endIndex != 0){
                outList.add(startIndex)
                outList.add(endIndex)
            }

            val increment = input.substring(i).indexOfFirst { c -> !notConsidered2.contains(c) }
            if (increment == -1)
                i = input.length
            else
                i += increment

            startIndex = i
            endIndex = 0
            k = 0
            elemIndices = (elements.indices).toMutableList()
        }
    }

    outList.add(this.length)
    return outList.sorted()
}

fun String.findSpanIndex3(element: String?): List<Int>{
    if (element.isNullOrEmpty()) return listOf()
    val input = String(this.map { c -> when (c) {
        'إ', 'أ', 'آ' -> 'ا'
        'ة', 'ۀ' -> 'ه'
        'ؤ' -> 'و'
        'ئ' -> 'ی'
        else -> c }
    }.toCharArray())
    val elemBiErab = makeTextBiErab(element)

    val notConsidered = "ءًٌٍَُِّْٔ\u200Cـ"
    val outList = mutableListOf<Int>()

    var i = 0
    var nexti = 1
    var k = 0
    while (i < input.length){
        while (i < input.length && notConsidered.contains(input[i]))
            i++

        if (k == 0)
            nexti = i+1

        while (k < elemBiErab.length && notConsidered.contains(elemBiErab[k]))
            k++

        if (i < input.length && k < elemBiErab.length){
            if (elemBiErab[k] == input[i]){
                if (input[i] == ' ')
                    i += input.substring(i).indexOfFirst { c -> c != ' ' }
                        .let { if (it == -1) 1 else it }
                else
                    i++
                k++
            } else {
                i = nexti
                nexti++
                k = 0
            }
        }

        if (k == elemBiErab.length){
            outList.add(nexti-1)
            outList.add(i)
            i = nexti
            nexti++
            k = 0
        }
    }

    return outList.sorted()
}

fun String.shorten(keyWords: List<String>, maxChars: Int): String?{ //Shorten a String based on keywords inside it
    if (this.trim().isEmpty() || keyWords.isEmpty()) return null

    val spanIndex = this.findSpanIndex2(keyWords)?.toMutableList()
    spanIndex?.removeFirstOrNull()
    spanIndex?.removeLastOrNull()
    val wordCount = (spanIndex?.size ?: 0) / 2
    if (wordCount == 0) return this
    val window = maxChars / (2 * wordCount)

    val extendedSpanIndex = spanIndex?.mapIndexed { index, num ->
        if (index % 2 == 0) {
            val ind = this.substring(0, (num - window).coerceAtLeast(0))
                .indexOfLast { c -> c.isWhitespace() }
            arrayOf(index, if (ind == -1) 0 else ind)
        } else {
            val ind = this.indexOf(' ', num + window)
            arrayOf(index, if (ind == -1) this.length else ind)
        }
    }?.sortedBy { it[1] }

    val modifiedSpanIndex = extendedSpanIndex?.filterIndexed { index, ints -> ints[0] == index }
        ?.map { it[1] }?.toMutableList() ?: return null
    var i = 1
    while (i < modifiedSpanIndex.size){
        if (modifiedSpanIndex[i] == modifiedSpanIndex[i-1]){
            modifiedSpanIndex.removeAt(i)
            modifiedSpanIndex.removeAt(i-1)
        }
        i++
    }

    if (modifiedSpanIndex.size == 0) return null
    val outString = if (modifiedSpanIndex[0] == 0) StringBuilder("") else StringBuilder("... ")
    for (j in modifiedSpanIndex.indices step 2){
        outString.append(this.substring(modifiedSpanIndex[j], modifiedSpanIndex[j+1]))
        if (modifiedSpanIndex[j+1] < this.length)
            outString.append(" ... ")
    }

    return outString.toString()
}

fun indexInNormalizedFinder(text: String, normalizedText: String, index: Int): Int {
    if (index >= text.length - 1 || index >= normalizedText.length - 1)
        return normalizedText.length

    var j = 0
    var i = 0
    while (j <= index){
        while (j+i < normalizedText.length && text[j] != normalizedText[j+i])
            i++

        j++
    }

    return index+i
}

fun rateApp(activity: Activity, comeFromRateButton: Boolean){

//    val packageName = "com.takaapoo.adab_parsi"
    val uri: Uri = Uri.parse("market://details?id=${activity.packageName}")
    val globalIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
    }

    when (appStore){
        AppStore.GooglePlay -> {
            if (comeFromRateButton){
                if (globalIntent.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(globalIntent)
                } else {
                    activity.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=${activity.packageName}")))
                }
            } else {
                val manager = ReviewManagerFactory.create(activity)
                val request = manager.requestReviewFlow()
                Timber.i("request: ${request.isSuccessful}")
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.i("task is successful")

                        // We got the ReviewInfo object
                        val flow = task.result?.let { manager.launchReviewFlow(activity, it) }
                        flow?.addOnCompleteListener {
                            Timber.i("flow is complete")
                            // The flow has finished. The API does not indicate whether the user
                            // reviewed or not, or even whether the review dialog was shown. Thus, no
                            // matter the result, we continue our app flow.
                        }
                    } else {
                        // There was some problem, log or handle the error code.
//                        @ReviewErrorCode val reviewErrorCode = (task.exception as RuntimeExecutionException).errorCode
//                        Timber.i("reviewErrorCode = $reviewErrorCode")
                    }
                }
            }
        }
        AppStore.Bazaar -> {
            val bazaarIntent = Intent(Intent.ACTION_EDIT).apply {
                data = Uri.parse("bazaar://details?id=${activity.packageName}")
                setPackage("com.farsitel.bazaar")
            }
            when {
                bazaarIntent.resolveActivity(activity.packageManager) != null -> {
                    activity.startActivity(bazaarIntent)
                }
                globalIntent.resolveActivity(activity.packageManager) != null -> {
                    activity.startActivity(globalIntent)
                }
                else -> {
                    activity.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=${activity.packageName}")))
//                    Toast.makeText(context, context.getString(R.string.rate_app_not_found),
//                        Toast.LENGTH_LONG).show()
                }
            }
        }
        AppStore.Myket -> {

        }
    }
}

fun allSubCategories(id: Int): List<Int>{
    val outList = mutableListOf(id)
    allCategory.filter { it.parentID == id }.map { it.id }.forEach{ elem ->
        outList.addAll(allSubCategories(elem))
    }
    return outList.sortedBy { it }
}

fun allUpCategories(id: Int?): List<Int>{
    val outList = mutableListOf<Int>()
    var parentId = id

//    parentId = id
    while (parentId != 0){
        val cat = allCategory.find { it.id == parentId } ?: break
        outList.add(cat.id)
        parentId = cat.parentID
    }
    return  outList
}


fun getDeviceName(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (model.lowercase(Locale.getDefault()).startsWith(manufacturer.lowercase(Locale.getDefault()))) {
        capitalize(model)
    } else {
        capitalize(manufacturer) + " " + model
    }
}


private fun capitalize(s: String?): String {
    if (s.isNullOrEmpty()) return ""

    val first = s[0]
    return if (Character.isUpperCase(first)) {
        s
    } else {
        Character.toUpperCase(first).toString() + s.substring(1)
    }
}

fun RectF.scale(scaleX: Float, scaleY: Float): RectF{
    return RectF(centerX() - scaleX * (centerX() - left), centerY() - scaleY * (centerY() - top),
        centerX() + scaleX * (right - centerX()), centerY() + scaleY * (bottom - centerY())
    )
}

fun RectF.enlarge(r: Float) = RectF(left - r, top - r, right + r, bottom + r)



/** The magnitude of translation distance while the list is over-scrolled. */
const val OVERSCROLL_TRANSLATION_MAGNITUDE = 0.2f

/** The magnitude of translation distance when the list reaches the edge on fling. */
const val FLING_TRANSLATION_MAGNITUDE = 0.5f

enum class Orientation{ VERTICAL , HORIZONTAL}

class BounceEdgeEffectFactory(val orientation: Orientation) : RecyclerView.EdgeEffectFactory() {

    override fun createEdgeEffect(recyclerView: RecyclerView, direction: Int): EdgeEffect {

        return object : EdgeEffect(recyclerView.context) {

            val anim =
                SpringAnimation(recyclerView, when(orientation) {
                    Orientation.VERTICAL -> SpringAnimation.TRANSLATION_Y
                    else -> SpringAnimation.TRANSLATION_X
                })
                    .setSpring(SpringForce()
                        .setFinalPosition(0f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                        .setStiffness(SpringForce.STIFFNESS_LOW)
                )

            override fun onPull(deltaDistance: Float) {
                super.onPull(deltaDistance)
                handlePull(deltaDistance)
            }

            override fun onPull(deltaDistance: Float, displacement: Float) {
                super.onPull(deltaDistance, displacement)
                handlePull(deltaDistance)
            }

            private fun handlePull(deltaDistance: Float) {
                // Translate the recyclerView with the distance
                val sign = if (direction == DIRECTION_BOTTOM || direction == DIRECTION_RIGHT) -1 else 1
                val translationDelta = sign *
                        when(orientation) {
                            Orientation.VERTICAL -> recyclerView.height
                            else -> recyclerView.width
                        } * deltaDistance * OVERSCROLL_TRANSLATION_MAGNITUDE

                when(orientation) {
                    Orientation.VERTICAL -> recyclerView.translationY += translationDelta
                    else -> recyclerView.translationX += translationDelta
                }
                anim?.cancel()
            }

            override fun onRelease() {
                super.onRelease()
                // The finger is lifted. Start the animation to bring translation back to the resting state.
                if (recyclerView.translationY != 0f || recyclerView.translationX != 0f)
                    anim.start()
            }

            override fun onAbsorb(velocity: Int) {
                super.onAbsorb(velocity)
                // The list has reached the edge on fling.
                val sign = if (direction == DIRECTION_BOTTOM || direction == DIRECTION_RIGHT) -1 else 1
                val translationVelocity = sign * velocity * FLING_TRANSLATION_MAGNITUDE
                anim?.cancel()
                anim.setStartVelocity(translationVelocity)?.also { it.start() }
            }

            override fun draw(canvas: Canvas?): Boolean {
                // don't paint the usual edge effect
                return false
            }

            override fun isFinished(): Boolean {
                // Without this, will skip future calls to onAbsorb()
                return anim?.isRunning?.not() ?: true
            }
        }
    }
}


