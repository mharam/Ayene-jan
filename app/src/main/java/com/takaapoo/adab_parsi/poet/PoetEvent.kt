package com.takaapoo.adab_parsi.poet

import androidx.navigation.NavDirections
import androidx.navigation.Navigator
import com.takaapoo.adab_parsi.util.Destinations

sealed class PoetEvent {

    data class OnBiographyClicked(val poetID: Int) : PoetEvent()
    data class Navigate(val destination: Destinations,
                        val directions: NavDirections,
                        val extras: Navigator.Extras? = null) : PoetEvent()
    data class OnShowHelp(val state: PoetHelpState): PoetEvent()
}

enum class PoetHelpState{
    NULL, PAGING, SEARCH
}