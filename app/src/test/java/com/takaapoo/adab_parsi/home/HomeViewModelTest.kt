package com.takaapoo.adab_parsi.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.takaapoo.adab_parsi.getOrAwaitValueTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import com.takaapoo.adab_parsi.MainCoroutineRule
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.database.FakeTestDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.robolectric.annotation.Config
import javax.inject.Inject


//@HiltAndroidTest
//@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class HomeViewModelTest{

//    @get:Rule(order = 0)
//    var hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1)
    var instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule(order = 2)   // Since I used @RunWith(AndroidJUnit4::class), using this rule is not necessary
    var mainCoroutineRule = MainCoroutineRule()


    lateinit var homeViewModel: HomeViewModel

    private var categories = mutableListOf(
        Category(12, 0, 125, "گلستان",
        8, "", 1002L, 1)
    )
    // Since this is a unit test I have to use fake dao not the real dao
    private lateinit var dao: FakeTestDao



    @Before
    fun setupViewModel() = runBlockingTest{
        dao = FakeTestDao()
        dao.insertDatabase(categories, emptyList(), emptyList(), emptyList())
//        hiltRule.inject()

        homeViewModel = HomeViewModel(getApplicationContext(), dao)
    }

    @Test
    fun showHelp_startShowingHelpDialog(){
        // When Triggering ShowHelp
//        homeViewModel.doShowHelp()
//        homeViewModel.increaseShowHelp()
//
//        // Then ShowHelp has nonNull value
//        val value = homeViewModel.showHelp.getOrAwaitValueTest()
//        assertThat(value).isEqualTo(2)
    }

    @Test
    fun deleteAPoet_PoetDataDeleted() = runBlockingTest {
        // When calling deleteDatabase with a poet id
        homeViewModel.deleteDatabase(listOf(125L))

        // Then dao categories variable should be empty
        assertThat(dao.categories).isEmpty()
    }


}