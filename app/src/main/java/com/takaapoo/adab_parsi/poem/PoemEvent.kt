package com.takaapoo.adab_parsi.poem

import android.view.MenuItem
import android.view.MotionEvent


sealed class PoemEvent {

    object OnSingleTap : PoemEvent()
    object OnDoubleTap : PoemEvent()

    open class TextMenu : PoemEvent() {
        object OpenDictionary : TextMenu()
        object Copy : TextMenu()
        object AddNote : TextMenu()
        object Marker1 : TextMenu()
        object Marker2 : TextMenu()
        object Marker3 : TextMenu()
        object Eraser : TextMenu()
    }

    data class OnMenuItemClicked(val menuItem: MenuItem) : PoemEvent()
    data class OnToggleButtonClicked(val isChecked: Boolean) : PoemEvent()
    data class OnContextMenuItemClicked(val menuItem: MenuItem?) : PoemEvent()
    data class OnShowHelp(val state: PoemHelpState) : PoemEvent()

    object OnRefreshContent : PoemEvent()
    object OnRefreshTextMenu : PoemEvent()

    data class OnRightHandleMove(val motionEvent: MotionEvent) : PoemEvent()
    data class OnLeftHandleMove(val motionEvent: MotionEvent) : PoemEvent()

    object OnExportDialogPositiveClick : PoemEvent()
    object OnShareDialogPositiveClick : PoemEvent()
}

enum class PoemHelpState {
    NULL, PAGING, TOOLBAR, VERSE
}