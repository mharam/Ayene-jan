package com.takaapoo.adab_parsi.poem

import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.database.Verse
import com.takaapoo.adab_parsi.setting.SettingViewModel

class PoemHilighter(private val verses: List<Verse>, private val poemItem: Content,
                    private val pvm: PoemViewModel, private val stvm: SettingViewModel) {


    fun fullHilight(selectedVerseOrder: List<Int>){
        selectedVerseOrder.forEach { verseOrder ->
            val firstIndex1 = verses[verseOrder - 1].text!!.indexOfFirst { c -> !c.isWhitespace() }
            var lastIndex1 = verses[verseOrder - 1].text!!.indexOfLast { c -> !c.isWhitespace() } + 1
            val hilight1 = verses[verseOrder - 1].hilight?.split(',')?.map { it.toInt() }

            if ((verses[verseOrder - 1].position == 0 && verses.size > verseOrder && verses[verseOrder].position == 1)
                /*|| */){

                val firstIndex2 = verses[verseOrder].text!!.indexOfFirst { c -> !c.isWhitespace() }
                val lastIndex2 = verses[verseOrder].text!!.indexOfLast { c -> !c.isWhitespace() } + 1
                val hilight2 = verses[verseOrder].hilight?.split(',')?.map { it.toInt() }

                if (hilight1 == listOf(firstIndex1, lastIndex1, stvm.hilightColorPref)
                    && hilight2 == listOf(firstIndex2, lastIndex2, stvm.hilightColorPref)){
                    pvm.updateHilight(null, poemItem.id, verseOrder)
                    pvm.updateHilight(null, poemItem.id, verseOrder+1)
                } else{
                    pvm.updateHilight("${firstIndex1},${lastIndex1},${stvm.hilightColorPref}",
                        poemItem.id, verseOrder)
                    pvm.updateHilight("${firstIndex2},${lastIndex2},${stvm.hilightColorPref}",
                        poemItem.id, verseOrder+1)
                }
            } else if ((verses[verseOrder - 1].position == 2 && verses.size > verseOrder
                        && verses[verseOrder].position == 3)){

                lastIndex1 = verses[verseOrder - 1].text!!.length +
                        verses[verseOrder].text!!.indexOfLast { c -> !c.isWhitespace() } + 2
                if (hilight1 == listOf(firstIndex1, lastIndex1, stvm.hilightColorPref))
                    pvm.updateHilight(null, poemItem.id, verseOrder)
                else
                    pvm.updateHilight("${firstIndex1},${lastIndex1},${stvm.hilightColorPref}",
                        poemItem.id, verseOrder)
            } else {
                if (hilight1 == listOf(firstIndex1, lastIndex1, stvm.hilightColorPref))
                    pvm.updateHilight(null, poemItem.id, verseOrder)
                else
                    pvm.updateHilight("${firstIndex1},${lastIndex1},${stvm.hilightColorPref}",
                        poemItem.id, verseOrder)
            }
        }
    }

    fun hilight(verseOrder: Int, start: Int, end: Int, hilightColorPref: Int){
        val hilight = verses[verseOrder - 1].hilight?.split(',')?.map { it.toInt() }
        val hilightSegments = hilight?.chunked(3)

        if (hilight != null){
            val editedList =
                hilightSegments?.plus(listOf(listOf(start, end, hilightColorPref)))
                    ?.flatten()
                    ?.joinToString(",")
            pvm.updateHilight(editedList, poemItem.id, verseOrder)
        } else
            pvm.updateHilight("${start},${end},${hilightColorPref}", poemItem.id, verseOrder)
    }

    fun eraseHilight(verseOrder: Int, start: Int, end: Int){
        val hilight = verses[verseOrder - 1].hilight?.split(',')?.map { it.toInt() }
        val hilightSegments = hilight?.chunked(3)

        if (hilightSegments?.size == 1)
            pvm.updateHilight(null, poemItem.id, verseOrder)
        else{
            val elem = hilightSegments?.findLast { it[0] <= start && it[1] >= end }
            elem?.let {
                val editedList =
                    hilightSegments.minus(listOf(it))
                        .flatten()
                        .joinToString(",")
                pvm.updateHilight(editedList, poemItem.id, verseOrder)
            }
        }
    }


}

