package com.takaapoo.adab_parsi.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.FakeTestDao
import com.takaapoo.adab_parsi.getOrAwaitValueTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config


@RunWith(AndroidJUnit4::class)
@Config(manifest= Config.NONE)
@ExperimentalCoroutinesApi
class HomeRepositoryTest{
    // This rule runs all Architecture Components-related background jobs in the same thread
    // so that the test results happen synchronously, and in a repeatable order.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val cat1 = Category(12, 0, 125, "گلستان", 8, "",
        1002L, 1)
    private var categories = mutableListOf(cat1)

    private lateinit var dao: FakeTestDao
    private lateinit var homeRepository: HomeRepository


    @Before
    fun setupRepository() = runBlockingTest {
        dao = FakeTestDao()
        dao.insertDatabase(categories, emptyList(), emptyList(), emptyList())
        homeRepository = HomeRepository(getApplicationContext(), dao)
    }

    @Test
    fun allCat_Check() {
        val value = homeRepository.allCat.getOrAwaitValueTest()
        assertThat(value).isEqualTo(categories)
    }

    @Test
    fun deletePoet_RemoveSpecifiedPoetFromDatabase() = runBlockingTest {
        homeRepository.deletePoet(listOf(125))
        assertThat(dao.categories).doesNotContain(cat1)
    }

}