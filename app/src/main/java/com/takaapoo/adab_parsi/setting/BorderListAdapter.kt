package com.takaapoo.adab_parsi.setting

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.preference.PreferenceManager
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.SettingBorderItemBinding

class BorderListAdapter(context: Activity) : ArrayAdapter<Int>(context, -1, arrayOf()) {

    private val borderIds = arrayOf(R.drawable.frame0_1, R.drawable.frame1_1, R.drawable.frame2_1)
    private val borderNames = context.resources.getStringArray(R.array.border_entries)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SettingBorderItemBinding.inflate(layoutInflater, parent, false)

        binding.borderImage.setImageResource(borderIds[position])
        binding.borderName.text = borderNames[position]
        binding.borderRadioButton.isChecked = (position == sharedPreferences.getInt("border", 2))

        return binding.root
    }

    override fun getCount() = 3

}