package com.takaapoo.adab_parsi.home

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.database.FakeAndroidTestDao
import com.takaapoo.adab_parsi.launchFragmentInHiltContainer
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@MediumTest
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class HomeFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var dao: Dao
    private lateinit var categories: MutableList<Category>
    @BindValue
    lateinit var homeViewModel: HomeViewModel

    @Before
    fun initDao() = runBlockingTest{
        dao = FakeAndroidTestDao()
        categories = mutableListOf(
            Category(100012, 0, 50125, "حسن آرام*محمدحسن بن مجتبی آرام (متولد 1366 ه.ش.)",
                0, "", 1002L, 1)
        )
        dao.insertDatabase(categories, emptyList(), emptyList(), emptyList())
//        homeViewModel = HomeViewModel(getApplicationContext(), dao)
    }

    @After
    fun cleanDb() = runBlockingTest{
//        ServiceLocator.resetDao()
    }


    @Test
    fun launch_HomeFragment() = runBlockingTest{
        // WHEN - HomeFragment is launched to show list of poets
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_MyApp)

        // THEN - Long Click a poet cause its checkBox to be checked
        onView(withId(R.id.loaded_poet_list)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))

//        Thread.sleep(5000)
        onView(withId(R.id.checkBox)).check(matches(isChecked()))
    }

    @Test
    fun clickCard_NavigateToPoetFragment(){
/*
        // Testing using Mockito
        // GIVEN - HomeFragment is launched to show list of poets
        val homeScenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_MyApp)
        val navController = mock(NavController::class.java)
        homeScenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - click on the first poet
        onView(withId(R.id.loaded_poet_list)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // THEN - Verify we navigated to poet screen
        val action = HomeFragmentDirections.actionHomeFragmentToPoetFragment()
        verify(navController).navigate(action)
*/


        // Testing using TestNavHostController
        // GIVEN - HomeFragment is launched to show list of poets
        // Create a TestNavHostController
        val navController = TestNavHostController(getApplicationContext())
        launchFragmentInHiltContainer<HomeFragment>(themeResId = R.style.Theme_MyApp) {
            // Set the graph on the TestNavHostController
            navController.setGraph(R.navigation.nav_graph)

            // Make the NavController available via the findNavController() APIs
            Navigation.setViewNavController(requireView(), navController)
        }

        // WHEN - click on the first poet
        onView(withId(R.id.loaded_poet_list)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // THEN - Verify we navigated to poet fragment
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.poetFragment)
    }

    @Test
    fun clickTab_viewpagerPageChange(){
        launchFragmentInHiltContainer<HomeFragment>(themeResId = R.style.Theme_MyApp) {
            view?.layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        onView(withContentDescription(R.string.content_desc_ancient_poet_list)).check(matches(
            isDisplayed()))

//        onView(withText("معاصر")).perform(click())
        onView(withId(R.id.view_pager)).perform(swipeRight())

        onView(withContentDescription(R.string.content_desc_recent_poet_list)).check(matches(
            isDisplayed()))
    }

    @Test
    fun tapSearchBar_NavigateToSearchFragment(){
        val navController = TestNavHostController(getApplicationContext())
        launchFragmentInHiltContainer<HomeFragment>(themeResId = R.style.Theme_MyApp) {
            view?.layoutDirection = View.LAYOUT_DIRECTION_RTL
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(requireView(), navController)
        }

        assertThat(navController.currentDestination?.id).isEqualTo(R.id.homeFragment)
        onView(withId(R.id.toolbar)).perform(click())
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.searchFragment)
    }

    @Test
    fun clickFABButton_NavigateToAddFragment(){
        val navController = TestNavHostController(getApplicationContext())
        launchFragmentInHiltContainer<HomeFragment>(themeResId = R.style.Theme_MyApp) {
            view?.layoutDirection = View.LAYOUT_DIRECTION_RTL
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(requireView(), navController)
        }

        assertThat(navController.currentDestination?.id).isEqualTo(R.id.homeFragment)
        onView(withId(R.id.add_poet_fab)).perform(click())
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.addFragment)
    }

}