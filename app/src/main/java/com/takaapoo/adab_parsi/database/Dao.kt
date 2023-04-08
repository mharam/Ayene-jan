package com.takaapoo.adab_parsi.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Dao
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface Dao {

    @Query("SELECT * from cat WHERE parent_id == 0")
    suspend fun getCatPoet(): List<Category>

    @Query("SELECT * from cat ORDER BY text")
    fun getAllCat(): LiveData<List<Category>>

    @Query("SELECT * from cat ORDER BY text")
    suspend fun getAllCatSuspend(): List<Category>

    @Query("UPDATE cat SET last_open_date = :newDate WHERE id == :id")
    suspend fun updatePoetDate(newDate: Long, id: Int)

    @Query("SELECT * from poet")
    fun getAllPoet(): LiveData<List<Poet>>

    @Query("SELECT id from poet")
    fun getAllPoetId(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatabase(category: List<Category>,
                               poem: List<Poem>,
                               poet: List<Poet>,
                               verse: List<Verse>)

    @Query("SELECT id from cat WHERE poet_id IN (:poetID)")
    suspend fun getAllCatWithPoetId(poetID: List<Int>): List<Int>

    @Query("DELETE from cat WHERE poet_id IN (:poetID)")
    suspend fun deleteCat(poetID: List<Int>)

    @Query("DELETE from cat")
    suspend fun deleteAllCat()

    @Query("DELETE from poem WHERE cat_id IN (:catID)")
    suspend fun deletePoem(catID: List<Int>)

    @Query("DELETE from poem")
    suspend fun deleteAllPoem()

    @Query("DELETE from poet WHERE id IN (:poetID)")
    suspend fun deletePoet(poetID: List<Int>)

    @Query("DELETE from poet")
    suspend fun deleteAllPoet()

    @Query("""DELETE from verse WHERE poem_id IN (SELECT id from poem WHERE cat_id IN (:catID))""")
    suspend fun deleteVerse(catID: List<Int>)

    @Query("DELETE from verse")
    suspend fun deleteAllVerse()

    @RawQuery
    suspend fun vacuum(supportSQLiteQuery: SupportSQLiteQuery): Int?


    @Query("""SELECT id, cat_id AS parent_id, title AS text, 1 AS rowOrder, 0 AS rank from poem 
        WHERE cat_id IN (:catId) ORDER BY cat_id, id""")
    suspend fun getPoemWithCatID(catId: List<Int>): List<Content>

    @Query("""SELECT id, cat_id AS parent_id, title AS text, 1 AS rowOrder, 0 AS rank from poem 
        WHERE cat_id IN (:catId) ORDER BY cat_id, id""")
    fun getPoemWithCatID2(catId: List<Int>): LiveData<List<Content>>

    @Query("""SELECT id, parent_id, text, 2 AS rowOrder, 0 AS rank from cat WHERE parent_id == (:catId) 
            UNION SELECT id, cat_id, title, 1 AS rowOrder, 0 AS rank from poem WHERE cat_id == (:catId) 
            ORDER BY id""")
    fun getAllPoemWithCatID(catId: Int): LiveData<List<Content>>

    @Query("""SELECT id, parent_id, text, 2 AS rowOrder, 0 AS rank from cat WHERE parent_id IN (:catId) 
            UNION SELECT id, cat_id, title, 1 AS rowOrder, 0 AS rank from poem WHERE cat_id IN (:catId) 
            ORDER BY id""")
    suspend fun getAllPoemWithCatID(catId: List<Int>): List<Content>

    @Query("SELECT * from poem WHERE id == :poem_id")
    fun getPoemWithID(poem_id: Int): LiveData<Poem>

    @Query("SELECT * from poet WHERE id == :id")
    fun getPoet(id: Int): LiveData<Poet>

//    @Query("""WITH poemCAT AS (SELECT cat_id from poem WHERE id == :poem_id)
//        SELECT title from poem WHERE id == :poem_id UNION ALL
//        SELECT text from cat WHERE id IN poemCAT UNION ALL
//        SELECT text from cat WHERE id IN (SELECT parent_id from cat WHERE id IN poemCAT) UNION ALL
//        SELECT text from cat WHERE id IN (SELECT parent_id from cat WHERE id IN (SELECT parent_id from cat WHERE id IN poemCAT))
//    """)
//    fun getPoemRootWithID(poem_id: Int): LiveData<List<String?>>

//    @Query("""
//        SELECT title, c1.text AS cat1, c2.text AS cat2, c3.text AS cat3 from poem
//        INNER JOIN cat AS c1 ON poem.cat_id = c1.id
//        LEFT JOIN cat AS c2 ON c1.parent_id = c2.id
//        LEFT JOIN cat AS c3 ON c2.parent_id = c3.id WHERE poem.id == :poem_id """)
//    fun getPoemRootWithID(poem_id: Int): LiveData<PoemRoot>

    @Query("SELECT book_mark from poem WHERE id == :poem_id")
    fun getPoemBookmark(poem_id: Int): LiveData<Long?>

    @Query("UPDATE poem SET book_mark = :state WHERE id == :poem_id")
    suspend fun updateBookmark(state: Long?, poem_id: Int)

    @Query("""UPDATE verse SET favorite = :state WHERE poem_id == :poem_id and vorder IN (:vOrder)""")
    suspend fun updateFavorite(state: Long?, poem_id: Int, vOrder: List<String>)

    @Query("""UPDATE verse SET hilight = :hilight WHERE poem_id == :poem_id and vorder == :vOrder""")
    suspend fun updateHilight(hilight: String?, poem_id: Int, vOrder: Int)

    @Query("UPDATE verse SET note = :text WHERE poem_id == :poemId and vorder == :vOrder")
    suspend fun updateNote(text: String?, poemId: Int, vOrder: Int)

    @Query("SELECT COUNT(book_mark) from poem WHERE book_mark IS NOT NULL")
    fun bookmarkCount(): LiveData<Int>

    @Query("SELECT COUNT(favorite) from verse WHERE favorite IS NOT NULL")
    fun favoriteCount(): LiveData<Int>

    @Query("SELECT * from verse WHERE poem_id == :poem_id")
    fun getVerseWithPoemID(poem_id: Int): LiveData<List<Verse>>


//    @SkipQueryVerification
    @Query("""
        SELECT verse.text, 0 AS isRecentSearch from verse INNER JOIN versefts ON versefts.docid = verse.rowid
        WHERE versefts MATCH :query AND (verse.poem_id == :poemId OR verse.poem_id IN
        (SELECT id from poem WHERE cat_id IN (:catId))) LIMIT 500""")
    fun getSearchSuggest(query: String, poemId: Int, catId: List<Int>): LiveData<List<SearchSuggest>>

    @Query("""SELECT verse.text, 0 AS isRecentSearch from verse INNER JOIN
        versefts ON versefts.docid = verse.rowid WHERE versefts MATCH :query LIMIT 500""")
    fun getAllSearchSuggest(query: String): LiveData<List<SearchSuggest>>

//    @Query("""
//        SELECT DISTINCT TRIM(snippet(versefts, '', '', '', -1, :tokenCount)) AS text, 0 AS isRecentSearch
//        from versefts INNER JOIN verse ON versefts.docid = verse.rowid
//        WHERE versefts MATCH :query AND (verse.poem_id == :poemId OR verse.poem_id IN
//        (SELECT id from poem WHERE cat_id IN (:catId))) LIMIT 100""")
//    fun getSearchSuggest(query: String, poemId: Int, catId: List<Int>, tokenCount: Int): LiveData<List<SearchSuggest>>
//
//    @Query("""SELECT DISTINCT TRIM(snippet(versefts, '', '', '', -1, :tokenCount)) AS text, 0 AS isRecentSearch from
//        versefts INNER JOIN verse ON versefts.docid = verse.rowid WHERE versefts MATCH :query
//        LIMIT 100""")
//    fun getAllSearchSuggest(query: String, tokenCount: Int): LiveData<List<SearchSuggest>>


    @Query("""
        SELECT v1.rowid AS row_id1, v2.rowid AS row_id2, v1.vorder, v1.position, v1.text AS text1, 
        v2.text AS text2, poem.* from verse AS v1
        INNER JOIN versefts ON versefts.docid = v1.rowid
        INNER JOIN verse AS v2 ON v2.rowid = v1.rowid + (-2)*(v1.position%2) + 1
        INNER JOIN poem ON poem.id = v1.poem_id
        WHERE (v1.poem_id == :poemId OR v1.poem_id IN (SELECT id from poem WHERE cat_id IN (:catId)))
        AND versefts MATCH :query LIMIT 12000""")
    suspend fun search(query: String, poemId: Int, catId: List<Int>): List<SearchContent>

    @Query("""
        SELECT v1.rowid AS row_id1, v2.rowid AS row_id2, v1.vorder, v1.position, v1.text AS text1, 
        v2.text AS text2, poem.* from verse AS v1
        INNER JOIN versefts ON versefts.docid = v1.rowid
        INNER JOIN verse AS v2 ON v2.rowid = v1.rowid + (-2)*(v1.position%2) + 1
        INNER JOIN poem ON poem.id = v1.poem_id
        WHERE versefts MATCH :query LIMIT 12000""")
    suspend fun searchAll(query: String): List<SearchContent>

    @Query(""" 
        SELECT v1.text AS verse1_text, v1.position AS verse1_pos, v2.text AS verse2_text, poem.* from poem
        INNER JOIN verse AS v1 ON v1.poem_id = poem.id AND v1.vorder = 1
        LEFT JOIN verse AS v2 ON v2.poem_id = poem.id AND v2.vorder = 2
        WHERE poem.book_mark IS NOT NULL ORDER BY poem.book_mark DESC""")
    fun getAllBookmark(): LiveData<List<BookmarkContent>>

    @Query(""" 
        SELECT v1.text AS verse1_text, v1.position AS verse1_pos, v1.vorder AS verse1_order, 
        v2.text AS verse2_text, poem.* from verse AS v1
        LEFT JOIN verse AS v2 ON v2.poem_id = v1.poem_id AND v2.rowid = v1.rowid + 1
        INNER JOIN poem ON poem.id = v1.poem_id
        WHERE v1.favorite IS NOT NULL ORDER BY v1.favorite DESC""")
    fun getAllFavorite(): LiveData<List<FavoriteContent>>

    @Query(""" 
        SELECT v1.text AS verse1_text, v1.position AS verse1_pos, v1.vorder AS verse1_order, 
        v2.text AS verse2_text, poem.* from verse AS v1
        LEFT JOIN verse AS v2 ON v2.poem_id = v1.poem_id AND v2.rowid = v1.rowid + 1
        INNER JOIN poem ON poem.id = v1.poem_id
        WHERE v1.favorite IS NOT NULL ORDER BY v1.favorite DESC""")
    suspend fun getAllFavoriteSuspend(): List<FavoriteContent>



}