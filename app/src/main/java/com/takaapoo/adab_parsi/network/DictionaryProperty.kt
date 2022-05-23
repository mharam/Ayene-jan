package com.takaapoo.adab_parsi.network

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize


@Parcelize
data class DictionaryProperty (
    @Json(name = "word")            val word: String,
    @Json(name = "wordBiErab")      val wordBiErab: String,
    @Json(name = "mean")            val mean: String,
    @Json(name = "dictionary")      val dictionary: Int
): Parcelable