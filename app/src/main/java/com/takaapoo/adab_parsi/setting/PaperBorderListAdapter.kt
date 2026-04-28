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

class PaperBorderListAdapter(context: Activity) : ArrayAdapter<Int>(context, -1, arrayOf()) {

    private val borderIds = arrayOf(0, R.drawable.frame5_3, R.drawable.frame6_3, R.drawable.frame7_3)
    private val borderNames = context.resources.getStringArray(R.array.paper_border_entries)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SettingBorderItemBinding.inflate(layoutInflater, parent, false)

        if (position != 0) binding.borderImage.setImageResource(borderIds[position])
        binding.borderName.text = borderNames[position]
        binding.borderRadioButton.isChecked = (position == sharedPreferences.getInt("paper_border", 1))

        return binding.root
    }

    override fun getCount() = 4

}