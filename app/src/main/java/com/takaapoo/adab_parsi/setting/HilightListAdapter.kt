package com.takaapoo.adab_parsi.setting

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.SettingBorderItemBinding



class HilightListAdapter(context: Activity) : ArrayAdapter<Int>(context, -1, arrayOf()) {

    private val highlightColodIds = arrayOf(R.color.hilight_1, R.color.hilight_2, R.color.hilight_3)
    private val highlightNames = context.resources.getStringArray(R.array.hilight_entries)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SettingBorderItemBinding.inflate(layoutInflater, parent, false)

        val shape = ResourcesCompat.getDrawable(context.resources, R.drawable.setting_paper_color,
            context.theme) as GradientDrawable
        shape.setColor(ResourcesCompat.getColor(context.resources, highlightColodIds[position], context.theme))

        binding.borderImage.setImageDrawable(shape)
        binding.borderImage.scaleType = ImageView.ScaleType.CENTER
        binding.borderName.text = highlightNames[position]
        binding.borderRadioButton.isChecked = (position == sharedPreferences.getInt("hilight", 0))

        return binding.root
    }

    override fun getCount() = 3

}