package com.takaapoo.adab_parsi.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

class FakeTestDao : Dao {

    var categories: MutableList<Category> = mutableListOf()
    var poets: MutableList<Poet> = mutableListOf()
    var poems: MutableList<Poem> = mutableListOf()
    var verses: MutableList<Verse> = mutableListOf()


    override suspend fun getCatPoet(): List<Category> {
        TODO("Not yet implemented")
    }

    override fun getAllCat(): LiveData<List<Category>> {
        return MutableLiveData(categories)
    }

    override suspend fun getAllCatSuspend(): List<Category> {
        TODO("Not yet implemented")
    }

    override suspend fun updatePoetDate(newDate: Long, id: Int) {
        TODO("Not yet implemented")
    }

    override fun getAllPoet(): LiveData<List<Poet>> {
        return MutableLiveData(poets)
    }

    override fun getAllPoetId(): Flow<List<Int>> {
        TODO("Not yet implemented")
    }

    override suspend fun insertDatabase(
        category: List<Category>,
        poem: List<Poem>,
        poet: List<Poet>,
        verse: List<Verse>
    ) {
        categories.addAll(category)
        poems.addAll(poem)
        poets.addAll(poet)
        verses.addAll(verse)
    }

    override suspend fun getAllCatWithPoetId(poetID: List<Int>): List<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCat(poetID: List<Int>) {
        categories.removeIf { it.poetID in poetID }
    }

    override suspend fun deleteAllCat() {
        categories.clear()
    }

    override suspend fun deletePoem(poetID: List<Int>) {
        poems.removeIf { poem -> poem.catID in categories.filter { poetID.contains(it.poetID) }.map { it.id } }
    }

    override suspend fun deleteAllPoem() {
        poems.clear()
    }

    override suspend fun deletePoet(poetID: List<Int>) {
        poets.removeIf { it.id in poetID }
    }

    override suspend fun deleteAllPoet() {
        poets.clear()
    }

    override suspend fun deleteVerse(poetID: List<Int>) {
        verses.removeIf { verse -> verse.poemId in (poems.filter { poem ->
            poem.catID in categories.filter {
            it.poetID in poetID }.map { it.id } }.map { it.id })
        }
    }

    override suspend fun deleteAllVerse() {
        verses.clear()
    }

    override suspend fun vacuum(supportSQLiteQuery: SupportSQLiteQuery): Int = 0

    override suspend fun getPoemWithCatID(catId: List<Int>): List<Content> {
        TODO("Not yet implemented")
    }

    override fun getPoemWithCatID2(catId: List<Int>): LiveData<List<Content>> {
        TODO("Not yet implemented")
    }

    override fun getAllPoemWithCatID(catId: Int): LiveData<List<Content>> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllPoemWithCatID(catId: List<Int>): List<Content> {
        TODO("Not yet implemented")
    }

    override fun getPoemWithID(poem_id: Int): LiveData<Poem> {
        TODO("Not yet implemented")
    }

    override fun getPoet(id: Int): LiveData<Poet> {
        TODO("Not yet implemented")
    }

    override fun getPoemBookmark(poem_id: Int): LiveData<Long?> {
        TODO("Not yet implemented")
    }

    override suspend fun updateBookmark(state: Long?, poem_id: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun updateFavorite(state: Long?, poem_id: Int, vOrder: List<String>) {
        TODO("Not yet implemented")
    }

    override suspend fun updateHilight(hilight: String?, poem_id: Int, vOrder: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun updateNote(text: String?, poemId: Int, vOrder: Int) {
        TODO("Not yet implemented")
    }

    override fun bookmarkCount(): LiveData<Int> {
        TODO("Not yet implemented")
    }

    override fun favoriteCount(): LiveData<Int> {
        TODO("Not yet implemented")
    }

    override fun getVerseWithPoemID(poem_id: Int): LiveData<List<Verse>> {
        TODO("Not yet implemented")
    }

    override fun getSearchSuggest(
        query: String,
        poemId: Int,
        catId: List<Int>
    ): LiveData<List<SearchSuggest>> {
        TODO("Not yet implemented")
    }

    override fun getAllSearchSuggest(query: String): LiveData<List<SearchSuggest>> {
        TODO("Not yet implemented")
    }

    override suspend fun search(query: String, poemId: Int, catId: List<Int>): List<SearchContent> {
        TODO("Not yet implemented")
    }

    override suspend fun searchAll(query: String): List<SearchContent> {
        TODO("Not yet implemented")
    }


    override fun getAllBookmark(): LiveData<List<BookmarkContent>> {
        TODO("Not yet implemented")
    }

    override fun getAllFavorite(): LiveData<List<FavoriteContent>> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllFavoriteSuspend(): List<FavoriteContent> {
        TODO("Not yet implemented")
    }


}