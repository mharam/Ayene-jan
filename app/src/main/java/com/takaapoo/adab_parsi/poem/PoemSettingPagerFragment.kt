package com.takaapoo.adab_parsi.poem

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentPoemSettingTextBinding
import com.takaapoo.adab_parsi.databinding.FragmentPoemSettingThemeBinding
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.engNumToFarsiNum
import kotlin.math.roundToInt


class PoemSettingPagerFragment : Fragment(), MaterialButtonToggleGroup.OnButtonCheckedListener {

    val settingViewModel: SettingViewModel by activityViewModels()
    val poemViewModel: PoemViewModel by activityViewModels()

    private var _bindingText: FragmentPoemSettingTextBinding? = null
    private val bindingText get() = _bindingText!!
    private var _bindingTheme: FragmentPoemSettingThemeBinding? = null
    private val bindingTheme get() = _bindingTheme!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        _bindingText = FragmentPoemSettingTextBinding.inflate(inflater, container, false)
        _bindingTheme = FragmentPoemSettingThemeBinding.inflate(inflater, container, false)

        arguments?.takeIf { it.containsKey(ARG_POEM_SETTING) }?.apply {
            when (getInt(ARG_POEM_SETTING)){
                0 -> {
                    val fontSizeNames = resources.getStringArray(R.array.font_size_entries)

                    (bindingText.fontPicker.getChildAt(settingViewModel.fontPref) as Chip).isChecked = true
                    updateFontChipText()
                    bindingText.fontPicker.setOnCheckedChangeListener { group, checkedId ->
                        when (checkedId) {
                            R.id.nastaliq -> settingViewModel.updateFont(0)
                            R.id.zar -> settingViewModel.updateFont(1)
                            R.id.davat -> settingViewModel.updateFont(2)
                        }
                        updateFontChipText()
                        poemViewModel.refresh()
                    }

                    bindingText.fontSize.text = fontSizeNames[settingViewModel.fontSizePref]
                    bindingText.fontSizeSmall.setOnClickListener {
                        val prefValue = (settingViewModel.fontSizePref-1).coerceAtLeast(0)
                        settingViewModel.updateFontSize(prefValue)
                        bindingText.fontSize.text = fontSizeNames[settingViewModel.fontSizePref]
                        poemViewModel.refresh()
                    }
                    bindingText.fontSizeLarge.setOnClickListener {
                        val prefValue = (settingViewModel.fontSizePref+1).coerceAtMost(fontSizeNames.size - 1)
                        settingViewModel.updateFontSize(prefValue)
                        bindingText.fontSize.text = fontSizeNames[settingViewModel.fontSizePref]
                        poemViewModel.refresh()
                    }

                    bindingText.hilightButton.check(when(settingViewModel.hilightColorPref) {
                        0 -> R.id.hilight_button1
                        1 -> R.id.hilight_button2
                        else -> R.id.hilight_button3
                    })
                    bindingText.hilightButton.addOnButtonCheckedListener(this@PoemSettingPagerFragment)


                    return bindingText.root
                }
                else -> {
                    bindingTheme.slider.value = settingViewModel.brightness.value ?: 100f
                    bindingTheme.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                        override fun onStartTrackingTouch(slider: Slider) {
                            slider.thumbRadius = 10.dpTOpx(resources).toInt()
                        }

                        override fun onStopTrackingTouch(slider: Slider) {
                            slider.thumbRadius = 8.dpTOpx(resources).toInt()
                            settingViewModel.updateBrightness(slider.value)
                        }
                    })
                    bindingTheme.slider.setLabelFormatter { value: Float ->
                        resources.getString(R.string.percent, engNumToFarsiNum(value.roundToInt()))
                    }
                    bindingTheme.slider.addOnChangeListener { slider, value, fromUser ->
                        settingViewModel.brightness.value = value
//                        poemViewModel.refreshBrightness()
                    }

                    bindingTheme.paperButton.check(when(settingViewModel.paperColorPref.value) {
                        0 -> R.id.paper_button1
                        1 -> R.id.paper_button2
                        else -> R.id.paper_button3
                    })
                    bindingTheme.paperButton.addOnButtonCheckedListener(this@PoemSettingPagerFragment)

                    val themeNames = if (Build.VERSION.SDK_INT > 28) resources.getStringArray(R.array.theme_entries)
                        else resources.getStringArray(R.array.theme_entries_old)
                    val adapter = ArrayAdapter(requireContext(), R.layout.theme_list_item, themeNames)
                    bindingTheme.textField.setText(themeNames[settingViewModel.themePref.toInt()])
                    (bindingTheme.themeMenu.editText as? AutoCompleteTextView)?.apply {
                        postDelayed({ setAdapter(adapter)
                                bindingTheme.textField.dismissDropDown() }, 500)
                    }
                    bindingTheme.themeMenu.editText?.doOnTextChanged { text, start, before, count ->
                        settingViewModel.updateTheme(themeNames.indexOf(text.toString()).toString(), requireContext())
//                        barsPreparation()
                    }


                    return bindingTheme.root
                }
            }
        }

        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingText.hilightButton.removeOnButtonCheckedListener(this)
        bindingTheme.paperButton.removeOnButtonCheckedListener(this)

        _bindingText = null
        _bindingTheme = null
    }

    private fun updateFontChipText() {
        bindingText.nastaliqText.isSelected = settingViewModel.fontPref == 0
        bindingText.lotusText.isSelected = settingViewModel.fontPref == 1
        bindingText.davatText.isSelected = settingViewModel.fontPref == 2
    }

    override fun onButtonChecked(group: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean) {
        if (isChecked) {
            when (group?.id) {
                R.id.hilight_button -> settingViewModel.updateHilight(when (checkedId) {
                    R.id.hilight_button1 -> 0
                    R.id.hilight_button2 -> 1
                    else -> 2
                })
                R.id.paper_button -> {
                    settingViewModel.updatePaper(when (checkedId) {
                        R.id.paper_button1 -> 0
                        R.id.paper_button2 -> 1
                        else -> 2
                    })
//                    poemViewModel.refreshPaper()
                }
            }
        }
    }
}