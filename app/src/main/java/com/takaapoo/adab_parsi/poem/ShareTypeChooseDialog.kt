package com.takaapoo.adab_parsi.poem

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.takaapoo.adab_parsi.R

class ShareTypeChooseDialog : DialogFragment(){

    val poemViewModel: PoemViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return parentFragment?.let {
            MaterialAlertDialogBuilder(it.requireContext())
                .setTitle(R.string.share_poem_message)
                .setPositiveButton(R.string.share_poem_pos_button){ _, _ ->
                    poemViewModel.reportEvent(PoemEvent.OnShareDialogPositiveClick)
                }
                .setMultiChoiceItems(R.array.poem_output_types, poemViewModel.shareOutFiles){ _, which, isChecked ->
                    poemViewModel.shareOutFiles[which] = isChecked
                }
                .create()
        } ?: throw IllegalStateException("Fragment cannot be null")
    }
}
