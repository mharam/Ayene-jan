package com.takaapoo.adab_parsi.network

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize

//@Keep
@Parcelize
data class PoetProperty (
    @Json(name = "id")              val id: Int,
    @Json(name = "ancient")         val ancient: Int,
    @Json(name = "poet_id")         val poetID: Int,
    @Json(name = "text")            val text: String,
    @Json(name = "parent_id")       val parentID: Int,
    @Json(name = "thumbnail_url")   val thumbnailURL: String?,
    @Json(name = "database_url")    val databaseURL: String?,
    @Json(name = "largeimage_url")  val largeimageURL: String?,
    @Json(name = "version")         val version: Int
): Parcelable