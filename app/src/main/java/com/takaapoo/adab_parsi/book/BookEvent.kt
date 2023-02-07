package com.takaapoo.adab_parsi.book

import com.takaapoo.adab_parsi.database.Content

sealed class BookEvent {
    data class OpenCloseContent(val newList: MutableList<Content>) : BookEvent()
    data class NavigateToPoem(val itemId: Int) : BookEvent()
    data class NavigateToSearch(val bookItemId: Int) : BookEvent()
    data class OnShowHelp(val state: BookHelpState) : BookEvent()

}

enum class BookHelpState {
    NULL, PAGING, SEARCH
}