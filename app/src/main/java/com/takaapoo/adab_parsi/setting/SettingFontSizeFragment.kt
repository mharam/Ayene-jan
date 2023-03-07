package com.takaapoo.adab_parsi.setting

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentSettingFontSizeBinding
import com.takaapoo.adab_parsi.poem.PoemEvent
import com.takaapoo.adab_parsi.poem.PoemViewModel
import com.takaapoo.adab_parsi.util.dpTOpx
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SettingFontSizeFragment: Fragment() {

    private val settingViewModel: SettingViewModel by activityViewModels()
    private val poemViewModel: PoemViewModel by activityViewModels()

    private var _binding: FragmentSettingFontSizeBinding? = null
    private val binding get() = _binding!!

    private val paperColorIds = arrayOf(R.color.paper_1, R.color.paper_2, R.color.paper_3)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        _binding = FragmentSettingFontSizeBinding.inflate(inflater, container, false)
        val navController = findNavController()
        binding.fontSizeToolbar.setupWithNavController(navController,
            AppBarConfiguration.Builder(navController.graph).build())


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        var fontSize = sharedPreferences.getInt("font_size", 1)
        val fontSizeNames = resources.getStringArray(R.array.font_size_entries)
        val fontSizeIds = intArrayOf(R.integer.font_small, R.integer.font_default, R.integer.font_large,
            R.integer.font_huge)
        val fontType = sharedPreferences.getInt("font", 0)

        binding.previewText.typeface = when (fontType){
            0 -> ResourcesCompat.getFont(requireContext(), R.font.iran_nastaliq)
            1 -> ResourcesCompat.getFont(requireContext(), R.font.zar)
            else -> ResourcesCompat.getFont(requireContext(), R.font.davat)
        }
        binding.previewText.textSize = resources.getInteger(fontSizeIds[fontSize]).toFloat()
        binding.previewText.setLineSpacing(when (fontType){
            1, 2 -> 12.dpTOpx(resources)
            else -> 0f}, 1f)

        binding.slider.value = fontSize.toFloat()
        binding.value.text = fontSizeNames[fontSize]


        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO)
            binding.scrollView.setBackgroundResource(
                paperColorIds[sharedPreferences.getInt("paper_color", 0)])


        binding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                slider.thumbRadius = 10.dpTOpx(requireContext().resources).toInt()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                slider.thumbRadius = 8.dpTOpx(requireContext().resources).toInt()
            }
        })

        binding.slider.addOnChangeListener { slider, value, fromUser ->
            fontSize = value.toInt()
//            sharedPreferences.edit().putInt("font_size", fontSize).apply()
            settingViewModel.updateFontSize(fontSize)
            poemViewModel.reportEvent(PoemEvent.OnRefreshContent)
            binding.previewText.textSize = resources.getInteger(fontSizeIds[fontSize]).toFloat()
            binding.value.text = fontSizeNames[fontSize]
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}