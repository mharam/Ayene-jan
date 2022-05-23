package com.takaapoo.adab_parsi.database

import android.os.Parcelable
import androidx.room.*
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "cat")
data class Category(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "ancient") val ancient: Int?,
    @ColumnInfo(name = "poet_id") val poetID: Int?,
    @ColumnInfo(name = "text") val text: String?,
    @ColumnInfo(name = "parent_id") val parentID: Int?,
    @ColumnInfo(name = "url") val url: String?,
    @ColumnInfo(name = "last_open_date") val lastOpenDate: Long?,
    @ColumnInfo(name = "version") val version: Int
)

@Entity(tableName = "cat")
data class TempCategory(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "ancient") val ancient: Int?,
    @ColumnInfo(name = "poet_id") val poetID: Int?,
    @ColumnInfo(name = "text") val text: String?,
    @ColumnInfo(name = "parent_id") val parentID: Int?,
    @ColumnInfo(name = "url") val url: String?,
    @ColumnInfo(name = "database_url") val databaseURL: String?,
    @ColumnInfo(name = "largeimage_url") val largeimageURL: String?,
    @ColumnInfo(name = "version") val version: Int
)

@Entity(tableName = "poem")
data class Poem(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int? = 0,
    @ColumnInfo(name = "cat_id") val catID: Int? = 0,
    @ColumnInfo(name = "title") val title: String? = "",
    @ColumnInfo(name = "url") val url: String? = "",
    @ColumnInfo(name = "book_mark") val bookMark: Long? = 0
)

@Entity(tableName = "poet")
data class Poet(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "cat_id") val catID: Int?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "wiki") val wiki: String?,
)

//@Fts4(notIndexed = ["poem_id", "vorder", "position"])
@Entity(tableName = "verse", primaryKeys = ["poem_id", "vorder"])
data class Verse(
    @ColumnInfo(name = "poem_id") val poemId: Int,
    @ColumnInfo(name = "vorder") val verseOrder: Int,
    @ColumnInfo(name = "position") val position: Int?,
    @ColumnInfo(name = "text") var text: String?,
    @ColumnInfo(name = "text_bi_erab") val textBiErab: String?,
    @ColumnInfo(name = "favorite") val favorite: Long?,
    @ColumnInfo(name = "hilight") var hilight: String?,
    @ColumnInfo(name = "note") var note: String?
)

@Entity(tableName = "verse", primaryKeys = ["poem_id", "vorder"])
data class TempVerse(
    @ColumnInfo(name = "poem_id") val poem_id: Int,
    @ColumnInfo(name = "vorder") val verseOrder: Int,
    @ColumnInfo(name = "position") val position: Int?,
    @ColumnInfo(name = "text") var text: String?
)

@Fts4(contentEntity = Verse::class)
@Entity(tableName = "versefts")
data class VerseFts(
    @ColumnInfo(name = "text_bi_erab") val textBiErab: String?
)






@Parcelize
data class Content(
    val id: Int = 0,
    @ColumnInfo(name = "parent_id") val parentID: Int? = 0,
    val text: String? = "",
    val rowOrder: Int = 0,
    var rank: Int = 0
): Parcelable

@Entity(tableName = "recent_search", indices = [Index(value = ["text"], unique = true)])
data class RecentSearch(
    val text: String,
    val date: Long
) {@PrimaryKey(autoGenerate = true) var id: Int = 0}

data class SearchSuggest(
    val text: String,
    val isRecentSearch: Boolean     // true -> RecentSearch, false -> DatabaseSearch
)

data class SearchContent(
    @ColumnInfo(name = "row_id1") val rowId1: Int,
    @ColumnInfo(name = "row_id2") val rowId2: Int,
    @ColumnInfo(name = "vorder") val verseOrder: Int,
    @ColumnInfo(name = "position") val position: Int?,
    @ColumnInfo(name = "text1") var majorText: String?,
    @ColumnInfo(name = "text2") var minorText: String?,
    @Embedded  val poemm: Poem
)

//data class CatRoot(
//    val cat1: String?,              // Chapter
//    val cat2: String?,              // Book
//    val cat3: String?,              // Poet
//    @ColumnInfo(name = "book_count") val bookCount: Int?
//)

data class BookmarkContent(
    @ColumnInfo(name = "verse1_text") val verse1Text: String?,
    @ColumnInfo(name = "verse1_pos")  val verse1Position: Int,
    @ColumnInfo(name = "verse2_text") val verse2Text: String?,
    @Embedded  val poemm: Poem
)

data class FavoriteContent(
    @ColumnInfo(name = "verse1_text") val verse1Text: String?,
    @ColumnInfo(name = "verse1_pos")  val verse1Position: Int?,
    @ColumnInfo(name = "verse1_order")  val verse1Order: Int,
    @ColumnInfo(name = "verse2_text") val verse2Text: String?,
    @Embedded  val poemm: Poem
)

data class RankedFavoriteContent(
    val verse1Text: String?,
    val verse1Position: Int?,
    val verse1Order: Int,
    val verse2Text: String?,
    val poemm: Poem,
    val rank: Int
)

fun FavoriteContent.toRankedFavoriteContent(rank: Int): RankedFavoriteContent{
    return RankedFavoriteContent(verse1Text, verse1Position, verse1Order, verse2Text, poemm, rank)
}