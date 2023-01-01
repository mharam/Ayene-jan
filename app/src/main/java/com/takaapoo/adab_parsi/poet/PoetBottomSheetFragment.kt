package com.takaapoo.adab_parsi.poet

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.takaapoo.adab_parsi.databinding.ButtomsheetPoetBinding

class PoetBottomSheetFragment : BottomSheetDialogFragment() {

    val poetViewModel: PoetViewModel by activityViewModels()
    lateinit var binding: ButtomsheetPoetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        binding = ButtomsheetPoetBinding.inflate(inflater, container, false)
        arguments?.getInt("poet_id")?.let { poetId ->
            poetViewModel.getPoet(poetId).observe(viewLifecycleOwner){ poet ->
                binding.story.text = poet.description ?: ""

                if (poet.wiki == null)
                    binding.moreInfo.visibility = View.GONE
                else
                    binding.moreInfo.setOnClickListener { openWiki(poet.wiki) }
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.root.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO)
                    (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
            else
                0
        }
    }


    private fun openWiki(wiki:String) {
        val webPage: Uri = Uri.parse(wiki)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = webPage
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        }
    }

}