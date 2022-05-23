package com.takaapoo.adab_parsi.poem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Toast
import com.takaapoo.adab_parsi.R

class PoemTextMenu(private val poemViewModel: PoemViewModel, private val preparer: FragmentPreparer) {

//    lateinit var poemFragment: PoemFragment
    var start = 0
    var end = 0

    fun textMenuClicked(v: View){
        val ppf = preparer.getFragment()
        when (v.id){
            R.id.dictionary -> {
                ppf?.parentFragmentManager?.let { WordMeaningDialog().show(it, "word_meaning") }
            }
            R.id.copy -> {
                val clipboard =
                    ppf?.context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val copiedText =
                    poemViewModel.textMenuText
                        ?.subSequence(poemViewModel.textMenuStart, poemViewModel.textMenuEnd)
                        .toString()
                        .replace("ـ", "")
                        .replace("\\s+".toRegex(), " ")
                        .trim()

                val clip = ClipData.newPlainText("sher", copiedText)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(ppf?.context, R.string.copied, Toast.LENGTH_LONG).show()
            }
            R.id.add_note -> {
                ppf?.addNote(poemViewModel.textNoteVerseOrder)
            }
            R.id.marker1 -> {
                ppf?.poemHilighter?.hilight(poemViewModel.textVerseOrder, start, end, 0)
            }
            R.id.marker2 -> {
                ppf?.poemHilighter?.hilight(poemViewModel.textVerseOrder, start, end, 1)
            }
            R.id.marker3 -> {
                ppf?.poemHilighter?.hilight(poemViewModel.textVerseOrder, start, end, 2)
            }
            R.id.eraser -> {
                ppf?.poemHilighter?.eraseHilight(poemViewModel.textVerseOrder, start, end)
            }
        }
        ppf?.deselectText()
    }

    interface FragmentPreparer {
        fun getFragment(): PoemPagerFragment?
    }

}