package com.takaapoo.adab_parsi.poem

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentPoemMeaningBinding


class WordMeaningDialog : BottomSheetDialogFragment() {

    val poemViewModel: PoemViewModel by activityViewModels()
    private var _binding: FragmentPoemMeaningBinding? = null
    private val binding get() = _binding!!

    private var currentNightMode = 0
    private var tabMediator: TabLayoutMediator? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
    : View {

        currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        _binding = FragmentPoemMeaningBinding.inflate(inflater, container, false)
        binding.word.text = poemViewModel.meanWord
        tabMediator = TabLayoutMediator(binding.tab, binding.viewPager) { tab, position ->
            when (position){
                0 -> tab.text = "معین"
                1 -> tab.text = "دهخدا"
            }
        }

        poemViewModel.meaning.observe(viewLifecycleOwner){
            binding.viewPager.doOnPreDraw {
                binding.viewPager.adapter = PoemMeaningAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
                if (tabMediator?.isAttached == false) tabMediator?.attach()
            }
        }

        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 1


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//            val bottomSheet = (binding.root.parent.parent as View).findViewById<FrameLayout>(R.id.design_bottom_sheet)
//        (binding.root.parent.parent.parent as FrameLayout).y = settingViewModel.navigationBarHeight.toFloat()

        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog

            val bottomSheetInternal = d.findViewById<View>(R.id.design_bottom_sheet)
//            val behavior = BottomSheetBehavior.from(bottomSheetInternal!!)
//            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                val params = bottomSheetInternal?.layoutParams
                params?.height = requireActivity().window.decorView.height
                bottomSheetInternal?.layoutParams = params
            }
        }

        view.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0 &&
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                view.postDelayed({
//                    dialog?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
//                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
                    view?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
                    if (currentNightMode == Configuration.UI_MODE_NIGHT_NO)
                        view.systemUiVisibility = view.systemUiVisibility or
                                (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                }, 3500)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        view?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO)
            view?.systemUiVisibility = binding.root.systemUiVisibility or
                    (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)

//        Handler(Looper.getMainLooper()).postDelayed({
//            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
//                dialog?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
//                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
//                if (currentNightMode == Configuration.UI_MODE_NIGHT_NO)
//                    view?.systemUiVisibility = binding.root.systemUiVisibility or
//                            (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
//
//                (activity as MainActivity).hideSystemBars()
//            }
//        }, 50)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        tabMediator?.detach()
        tabMediator = null
        binding.viewPager.adapter = null

        _binding = null
    }
}


const val ARG_POEM_MEANING = "poem_meaning"

class PoemMeaningAdapter(fragManager: FragmentManager, lifeCycle: Lifecycle) :
    FragmentStateAdapter(fragManager, lifeCycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = WordMeaningPagerFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_POEM_MEANING, position)
        }

        return fragment
    }
}