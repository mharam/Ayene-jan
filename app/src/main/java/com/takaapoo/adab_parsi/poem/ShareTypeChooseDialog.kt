package com.takaapoo.adab_parsi.poem

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.takaapoo.adab_parsi.R

class ShareTypeChooseDialog(val listener: NoticeDialogListener) : DialogFragment(){

    private val checkedItems = booleanArrayOf(true, false, false)
    val poemViewModel: PoemViewModel by activityViewModels()

//    lateinit var listener: NoticeDialogListener
    interface NoticeDialogListener {
        fun onShareDialogPositiveClick()
        fun onShareDialogNegativeClick()
        fun onDismissClick()
        fun onItemSelected(which: Int, isChecked: Boolean)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            poemViewModel.shareOutFiles = checkedItems
//            listener = parentFragmentManager.findFragmentByTag("f${poemViewModel.poemPosition}")
//                    as NoticeDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(("$context must implement NoticeDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return parentFragment?.let {
            MaterialAlertDialogBuilder(it.requireContext())
                .setTitle(R.string.share_poem_message)
                .setPositiveButton(R.string.share_poem_pos_button){ _, _ ->
                    listener.onShareDialogPositiveClick()}
                .setNegativeButton(R.string.share_poem_neg_button){ _: DialogInterface, _: Int ->
                    listener.onShareDialogNegativeClick()}
                .setMultiChoiceItems(R.array.poem_output_types, checkedItems){ dialog, which, isChecked ->
                    listener.onItemSelected(which, isChecked)
                }
                .create()
        } ?: throw IllegalStateException("Fragment cannot be null")
    }
}
