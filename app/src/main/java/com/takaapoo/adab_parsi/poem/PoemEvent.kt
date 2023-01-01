package com.takaapoo.adab_parsi.poem

sealed class PoemEvent {


    object OnExportDialogPositiveClick : PoemEvent()
    object OnShareDialogPositiveClick : PoemEvent()
}