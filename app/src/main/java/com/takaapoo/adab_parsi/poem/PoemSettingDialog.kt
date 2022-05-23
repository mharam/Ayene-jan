package com.takaapoo.adab_parsi.poem

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentPoemSettingBinding
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.dpTOpx
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PoemSettingDialog : DialogFragment() {

    val settingViewModel: SettingViewModel by activityViewModels()
    val poemViewModel: PoemViewModel by activityViewModels()
    val windowWidth = 220

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mDialog = Dialog(requireContext(), R.style.Theme_Dialog)
        val binding = FragmentPoemSettingBinding.inflate(layoutInflater)
        mDialog.setContentView(binding.root)
        mDialog.window?.attributes?.width = (windowWidth + 16).dpTOpx(resources).toInt()

        mDialog.window?.setGravity(Gravity.TOP or Gravity.LEFT)

        val poemSettingAdapter = PoemSettingAdapter(this)
        val viewPager = binding.viewPager
        viewPager.adapter = poemSettingAdapter
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = 1

        TabLayoutMediator(binding.tab, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "متن"
                1 -> tab.text = "زمینه"
            }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val params = viewPager.layoutParams
                params.width = windowWidth.dpTOpx(resources).toInt()
                params.height = when (position) {
                    0 -> 232
                    else -> if (Build.VERSION.SDK_INT > 28) 212 else 242
                }.dpTOpx(resources).toInt()
                viewPager.layoutParams = params
            }
        })


//        val params = mDialog.window?.attributes
//        params?.x = 1000
//        params?.y = context?.getDimenFromAttr(R.attr.actionBarSize)
//        mDialog.window?.attributes = params

        return mDialog
    }

}

const val ARG_POEM_SETTING = "poem_setting"

class PoemSettingAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = PoemSettingPagerFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_POEM_SETTING, position)
        }

        return fragment
    }
}