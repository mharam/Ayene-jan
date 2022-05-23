package com.takaapoo.adab_parsi.setting

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.SettingBorderItemBinding

class FontListAdapter(val context: Activity) : ArrayAdapter<Int>(context, -1, arrayOf()) {

    private val fontNames = context.resources.getStringArray(R.array.font_entries)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val layoutInflater = LayoutInflater.from(context)
        val binding = SettingBorderItemBinding.inflate(layoutInflater, parent, false)

        binding.apply {
            borderImage.visibility = View.GONE
            borderName.text = fontNames[position]
            borderRadioButton.isChecked = (position == sharedPreferences.getInt("font", 0))
            borderName.typeface = ResourcesCompat.getFont(context, when (position){
                0 -> R.font.iran_nastaliq
                1 -> R.font.zar
                else -> R.font.davat
            })
            borderName.textSize = 20f
        }
        return binding.root
    }

    override fun getCount() = 3

}