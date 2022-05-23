package com.takaapoo.adab_parsi.setting

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.text.TextPaint
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.search.RecentSearchDao
import com.takaapoo.adab_parsi.util.spTOpx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(application: Application, val dao: RecentSearchDao) :
    AndroidViewModel(application) {

//    val dao: RecentSearchDao = RecentSearchDatabase.getDatabase(application).dao()
    fun deleteSearchHistory() = viewModelScope.launch { dao.deleteSearchHistory() }

    val paint = TextPaint()
    var textHeight = 0
    var spaceWidth = 0f
    var dashWidth = 0f
    var mWidth = 0f

//    var navigationBarHeight = 0

    val context = application

    private val fontIds = arrayOf(R.font.iran_nastaliq, R.font.zar, R.font.davat)
    private val fontSizeIds = intArrayOf(R.integer.font_small, R.integer.font_default, R.integer.font_large,
        R.integer.font_huge)
    private val paperColorIds = arrayOf(R.color.paper_1, R.color.paper_2, R.color.paper_3)
    private val paperColors = paperColorIds.map {
        ResourcesCompat.getColor(application.resources, it, application.theme)
    }
    private val hilightColorIds = arrayOf(R.color.hilight_1, R.color.hilight_2, R.color.hilight_3)
    val hilightColors = hilightColorIds.map {
        ResourcesCompat.getColor(application.resources, it, application.theme)
    }

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    var fontSizePref = sharedPreferences.getInt("font_size", 1)
    var fontPref = sharedPreferences.getInt("font", 0)
    var verseVertSep = if (fontPref == 0) 0.9f else 2f

    var fontSize = application.resources.getInteger(fontSizeIds[fontSizePref])
    set(fontSizePref){
        field = context.resources.getInteger(fontSizeIds[fontSizePref])
    }

    var font = ResourcesCompat.getFont(application, fontIds[fontPref])
    private fun setFont(fontPref: Int){
        font = ResourcesCompat.getFont(context, fontIds[fontPref])
        verseVertSep = if (fontPref == 0) 0.9f else 2f
    }

    val paperColorPref = MutableLiveData(sharedPreferences.getInt("paper_color", 0))
    var paperColor = paperColors[paperColorPref.value!!]

    var hilightColorPref = sharedPreferences.getInt("hilight", 0)
    private var hilightColor = hilightColors[hilightColorPref]

    var themePref = sharedPreferences?.getString("theme", "2") ?: "2"
    var currentNightMode = application.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    val brightness = MutableLiveData(sharedPreferences.getFloat("brightness", 100f))

    var borderPref = sharedPreferences.getInt("border", 2)

    var openExportFile = false

    private val _refreshContent = MutableLiveData<Boolean?>()
    val refreshContent: LiveData<Boolean?>
        get() = _refreshContent
    private fun refresh() { _refreshContent.value = true}
    fun doneRefreshing() { _refreshContent.value = null}




    private val rectangle = Rect()
    fun updateConstants(){
        paint.typeface = font
        paint.textSize = fontSize.spTOpx(context.resources)
        paint.getTextBounds(" کیخسرو ول", 0, 9, rectangle)
        textHeight = rectangle.height()
        spaceWidth = paint.measureText(" ")
        dashWidth = paint.measureText("ـ")
        mWidth = if (fontPref == 0) spaceWidth else dashWidth
    }

    fun updateFont(prefValue: Int){
        setFont(prefValue)
        fontPref = prefValue
        updateConstants()
        sharedPreferences.edit().putInt("font", prefValue).apply()
        refresh()
    }

    fun updateFontSize(prefValue: Int){
        fontSize = prefValue
        fontSizePref = prefValue
        updateConstants()
        sharedPreferences.edit().putInt("font_size", prefValue).apply()
        refresh()
    }

    fun updateHilight(prefValue: Int){
        hilightColorPref = prefValue
        hilightColor = hilightColors[prefValue]
        sharedPreferences.edit().putInt("hilight", prefValue).apply()
    }

    fun updatePaper(prefValue: Int){
        paperColor = paperColors[prefValue]
        paperColorPref.value = prefValue
        sharedPreferences.edit().putInt("paper_color", prefValue).apply()
    }

    fun updateTheme(prefValue: String, context: Context){
        themePref = prefValue
        AppCompatDelegate.setDefaultNightMode(when(prefValue){
            "0" -> AppCompatDelegate.MODE_NIGHT_NO
            "1" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> if (Build.VERSION.SDK_INT > 28) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        })
        sharedPreferences.edit().putString("theme", prefValue).apply()
        currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    fun updateBorder(prefValue: Int){
        borderPref = prefValue
        sharedPreferences.edit().putInt("border", prefValue).apply()
    }

    fun updateBrightness(value: Float){
        brightness.value = value
        sharedPreferences.edit().putFloat("brightness", value).apply()
    }

}