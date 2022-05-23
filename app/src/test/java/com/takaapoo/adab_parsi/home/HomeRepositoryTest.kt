package com.takaapoo.adab_parsi.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.FakeTestDao
import com.takaapoo.adab_parsi.getOrAwaitValueTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@ExperimentalCoroutinesApi
class HomeRepositoryTest{
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private var categories = mutableListOf(Category(12, 0, 125, "گلستان",
        8, "", 1002L, 1))

    private lateinit var dao: FakeTestDao
    private lateinit var homeRepository: HomeRepository


    @Before
    fun setupRepository() = runBlockingTest {
        dao = FakeTestDao()
        dao.insertDatabase(categories, emptyList(), emptyList(), emptyList())
        homeRepository = HomeRepository(dao)
    }

    @Test
    fun allCat_Check() {
        val value = homeRepository.allCat.getOrAwaitValueTest()
        assertThat(value).isEqualTo(categories)
    }

    @Test
    fun deletePoet_RemoveSpecifiedPoetFromDatabase() = runBlockingTest {
        homeRepository.deletePoet(listOf(125))
        assertThat(dao.categories).doesNotContain(
            Category(12, 0, 125, "گلستان",
                8, "", 1002L, 1)
        )
    }

}