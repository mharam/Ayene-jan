package com.takaapoo.adab_parsi.home

import androidx.recyclerview.selection.Selection
import com.takaapoo.adab_parsi.util.Destinations

sealed class HomeEvent {
    object OpenDrawer: HomeEvent()
    object OnCreateActionMode: HomeEvent()
    object OnDeleteDialogDismiss: HomeEvent()
    object OnDestroyActionMode: HomeEvent()

    data class Navigate(val destination: Destinations): HomeEvent()
    data class OnDeleteClick(val poetList: Selection<Long>): HomeEvent()
    data class OnDeleteDialogPosClick(val poetList: Selection<Long>): HomeEvent()
    data class OnShowHelp(val helpView: HelpView): HomeEvent()
}

enum class ActionModeState {
    VISIBLE, GONE
}

enum class HelpView {
    NULL, ADD_FAB, SEARCH_BAR, POET_CARD
}
