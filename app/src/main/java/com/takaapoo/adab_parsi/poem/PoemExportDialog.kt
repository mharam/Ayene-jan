package com.takaapoo.adab_parsi.poem

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentPoemExportBinding
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.engNumToFarsiNum

class PoemExportDialog : DialogFragment(), MaterialButtonToggleGroup.OnButtonCheckedListener {

    val poemViewModel: PoemViewModel by activityViewModels()
    val settingViewModel: SettingViewModel by activityViewModels()
    private var _binding: FragmentPoemExportBinding? = null
    private val binding get() = _binding!!
    lateinit var fileTypes: Array<String>

    private val fileTypeTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            poemViewModel.exportOutFile = fileTypes.indexOf(s.toString())
            setVisibility()
        }
        override fun afterTextChanged(s: Editable?) { }
    }

    private val sliderTouchListener = object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
            slider.thumbRadius = 10.dpTOpx(resources).toInt()
        }

        override fun onStopTrackingTouch(slider: Slider) {
            slider.thumbRadius = 8.dpTOpx(resources).toInt()
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mDialog = Dialog(requireContext(), R.style.Export_Dialog)
        _binding = FragmentPoemExportBinding.inflate(layoutInflater)
        mDialog.setContentView(binding.root)
        mDialog.window?.setGravity(Gravity.CENTER)

        setVisibility()

        fileTypes = resources.getStringArray(R.array.poem_output_types)
        val adapter = ArrayAdapter(requireContext(), R.layout.theme_list_item, fileTypes)
        binding.textField.setText(fileTypes[poemViewModel.exportOutFile])
        (binding.fileTypeMenu.editText as? AutoCompleteTextView)?.apply {
            postDelayed(
                {
                    setAdapter(adapter)
                    _binding?.textField?.dismissDropDown()
                },
                300
            )
        }
        binding.fileTypeMenu.editText?.addTextChangedListener(fileTypeTextWatcher)

        binding.frameButton.check(when(settingViewModel.borderPref) {
            0 -> R.id.frame_button1
            1 -> R.id.frame_button2
            else -> R.id.frame_button3
        })
        binding.frameButton.addOnButtonCheckedListener(this)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())

        binding.slider.value = sharedPreferences.getInt("pic_width", 2000).toFloat()
        binding.slider.addOnSliderTouchListener(sliderTouchListener)
        binding.slider.setLabelFormatter { value: Float ->
            resources.getString(R.string.pic_width_summary, engNumToFarsiNum(value.toInt()))
        }
        binding.slider.addOnChangeListener { slider, value, fromUser ->
            sharedPreferences.edit().putInt("pic_width", slider.value.toInt()).apply()
        }

        binding.showResultSwitch.isChecked = settingViewModel.openExportFile
        binding.showResultSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            settingViewModel.openExportFile = isChecked
        }

        binding.cancelButton.setOnClickListener { dismiss() }
        binding.saveButton.setOnClickListener {
            dismiss()
            poemViewModel.reportEvent(PoemEvent.OnExportDialogPositiveClick)
        }

        return mDialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.frameButton.removeOnButtonCheckedListener(this)
        binding.fileTypeMenu.editText?.removeTextChangedListener(fileTypeTextWatcher)
        binding.slider.removeOnSliderTouchListener(sliderTouchListener)
        _binding = null
    }

    override fun onButtonChecked(group: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean) {
        if (isChecked && group?.id == R.id.frame_button){
            settingViewModel.updateBorder(when (checkedId) {
                R.id.frame_button1 -> 0
                R.id.frame_button2 -> 1
                else -> 2
            })
        }
    }

    private fun changeSliderVisibility(visibility: Boolean){
        binding.widthHigh.isVisible = visibility
        binding.widthLow.isVisible = visibility
        binding.slider.isVisible = visibility
    }

    private fun setVisibility(){
        when (poemViewModel.exportOutFile){
            0 -> {
                changeSliderVisibility(false)
                binding.frameButton.isVisible = true
            }
            1 -> {
                changeSliderVisibility(true)
                binding.frameButton.isVisible = true
            }
            else -> {
                changeSliderVisibility(false)
                binding.frameButton.isVisible = false
            }
        }
    }

}