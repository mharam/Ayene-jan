package com.takaapoo.adab_parsi.poem

import android.view.GestureDetector
import android.view.MotionEvent


class PoemGestureListener(private val pvm: PoemViewModel, private val poemPagerFragment: PoemPagerFragment) :
    GestureDetector.SimpleOnGestureListener() {

    override fun onDown(event: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        val tempList = pvm.selectedVerses[poemPagerFragment.poemItem.id]?.value ?: mutableListOf()
        if (!tempList.remove(pvm.touchedViewID))
            tempList.add(pvm.touchedViewID)

        pvm.selectedVerses[poemPagerFragment.poemItem.id]?.value = tempList
        poemPagerFragment.deselectText()
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        val tempList = pvm.selectedVerses[poemPagerFragment.poemItem.id]?.value ?: mutableListOf()
        if (tempList.isNotEmpty()){
            if (!tempList.remove(pvm.touchedViewID))
                tempList.add(pvm.touchedViewID)

            pvm.selectedVerses[poemPagerFragment.poemItem.id]?.value = tempList
        } else {
            if (pvm.noteOpenedVerses[poemPagerFragment.poemItem.id]?.contains(pvm.touchedViewID) == true){
                poemPagerFragment.closeNote(pvm.touchedViewID)
            } else if (!poemPagerFragment.verses.find { it.verseOrder == pvm.touchedViewID }?.note.isNullOrEmpty()){
                pvm.commentTextFocused = false
                poemPagerFragment.openNote(pvm.touchedViewID)
            }
        }
        poemPagerFragment.deselectText()
        return true
    }


}