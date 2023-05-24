package com.takaapoo.adab_parsi.add

import android.view.View
import com.takaapoo.adab_parsi.network.PoetProperty

sealed class AddEvent {

    data class ShowSnack(val mess: String): AddEvent()
    data class PoetTouched(val poetView: View, val height: Int): AddEvent()
    data class DownloadPoet(val poetItem: PoetProperty): AddEvent()

}
