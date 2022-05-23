package com.takaapoo.adab_parsi.setting

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.Slider
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.engNumToFarsiNum

class PicWidthSeekbarPreference : Preference {

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int = 0, defStyleRes: Int) :
            super(context!!, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?) : super(context!!)

    init {
        layoutResource = R.layout.setting_width_item
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val mySlider = holder.findViewById(R.id.slider) as Slider
        val summary = holder.findViewById(R.id.summary) as TextView

        mySlider.value = preferenceManager.sharedPreferences?.getInt("pic_width", 2000)?.toFloat() ?: 2000f
        summary.text = context.resources.getString(R.string.pic_width_summary,
            engNumToFarsiNum(mySlider.value.toInt()))


        mySlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                slider.thumbRadius = 10.dpTOpx(context.resources).toInt()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                slider.thumbRadius = 8.dpTOpx(context.resources).toInt()
            }
        })

        mySlider.addOnChangeListener { slider, value, fromUser ->
            preferenceManager.sharedPreferences?.edit()?.putInt("pic_width", mySlider.value.toInt())?.apply()
            summary.text = context.resources.getString(R.string.pic_width_summary,
                engNumToFarsiNum(mySlider.value.toInt()))
        }

        mySlider.setLabelFormatter { value: Float ->
            context.resources.getString(R.string.pic_width_summary, engNumToFarsiNum(value.toInt()))
        }

    }
}