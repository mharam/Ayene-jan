package com.takaapoo.adab_parsi.poet

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.takaapoo.adab_parsi.database.Poet
import com.takaapoo.adab_parsi.databinding.ButtomsheetPoetBinding

class PoetBottomSheetFragment(val poet: Poet): BottomSheetDialogFragment() {

    lateinit var binding: ButtomsheetPoetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        binding = ButtomsheetPoetBinding.inflate(inflater, container, false)
        binding.story.text = poet.description ?: ""

        if (poet.wiki == null)
            binding.moreInfo.visibility = View.GONE
        else
            binding.moreInfo.setOnClickListener { openWiki() }

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


    private fun openWiki() {
        val webPage: Uri = Uri.parse(poet.wiki)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = webPage
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        }
    }

}