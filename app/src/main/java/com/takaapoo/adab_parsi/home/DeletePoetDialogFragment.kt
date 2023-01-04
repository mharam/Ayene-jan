package com.takaapoo.adab_parsi.home

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.selection.Selection
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.util.engNumToFarsiNum
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeletePoetDialogFragment(private val poetList: Selection<Long>)
    : DialogFragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(
                    getString(
                        R.string.delete_poet_title,
                        engNumToFarsiNum(poetList.size())
                    )
                )
                .setMessage(
                    resources.getQuantityString(R.plurals.delete_poet_message, poetList.size()))
                .setPositiveButton(R.string.delete_poet_pos_button) { _, _ ->
                    homeViewModel.reportEvent(HomeEvent.OnDeleteDialogPosClick(poetList))
                }
                .setNegativeButton(R.string.delete_poet_neg_button) { _: DialogInterface, _: Int ->  }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        homeViewModel.reportEvent(HomeEvent.OnDeleteDialogDismiss)
    }

}