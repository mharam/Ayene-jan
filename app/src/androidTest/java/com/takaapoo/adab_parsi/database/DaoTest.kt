package com.takaapoo.adab_parsi.database

import org.junit.Rule
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.takaapoo.adab_parsi.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class DaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: PoemDatabase
    private lateinit var dao: Dao
    private val categories = mutableListOf(Category(12, 0, 125, "سعدی",
        0, "", 1002L, 1))

    @Before
    fun initDb(){
        database = PoemDatabase.getTestDatabase(ApplicationProvider.getApplicationContext())
        dao = database.dao()
        database.clearAllTables()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertDatabase_getCatPoet() = runBlockingTest {
        // GIVEN - inserting a list of categories
        dao.insertDatabase(categories, emptyList(), emptyList(), emptyList())

        // WHEN - get all categories
        val loadedAllCatPoet = dao.getCatPoet()

        // THEN - loadedAllCat equals categories
        assertThat(loadedAllCatPoet).isEqualTo(categories)
    }

    @Test
    fun insertDatabase_getAllCat() = runBlockingTest {
        // GIVEN - inserting a list of categories
        dao.insertDatabase(categories, emptyList(), emptyList(), emptyList())

        // WHEN - get all categories
        val loadedAllCat = dao.getAllCat().getOrAwaitValue()

        // THEN - loadedAllCat equals categories
        assertThat(loadedAllCat).isEqualTo(categories)
    }


}