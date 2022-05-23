package com.takaapoo.adab_parsi.poet

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
//import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.takaapoo.adab_parsi.*
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.home.HomeViewModel
import com.takaapoo.adab_parsi.util.allCategory
import com.takaapoo.adab_parsi.util.collator
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject


@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@MediumTest
class PoetFragmentTest {

    @get:Rule(order = 1)
    var hiltRule = HiltAndroidRule(this)
    // Executes each task synchronously using Architecture Components.
    @get:Rule(order = 2)
    var instantExecutorRule = InstantTaskExecutorRule()


    @BindValue lateinit var homeViewModel: HomeViewModel
    lateinit var navController: TestNavHostController
    @Inject lateinit var dao: Dao

    @Before
    fun init(){
        hiltRule.inject()

        homeViewModel = HomeViewModel(getApplicationContext(), dao)
        val items = homeViewModel.allCat.getOrAwaitValue()
        allCategory = items.sortedWith { one, two -> collator.compare(one.text, two.text) }

        navController = TestNavHostController(getApplicationContext())
    }

    @Test
    fun swipePoetLibraries(){
        homeViewModel.count = 7
        homeViewModel.viewpagePosition = 6
        val fragment = launchFragmentInHiltContainer<PoetFragment>(themeResId = R.style.Theme_MyApp,
        fragmentFactory = object : FragmentFactory(){
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return PoetFragment().also {
                    it.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                        if (viewLifecycleOwner != null) {
                            navController.setGraph(R.navigation.nav_graph)
                            // Make the NavController available via the findNavController() APIs
                            Navigation.setViewNavController(it.requireView(), navController)
                        }
                    }
                }
            }
        })

        fragment?.view?.layoutDirection = View.LAYOUT_DIRECTION_RTL

        onView(allOf(withId(R.id.toolbar), isDisplayed()))
            .check(matches(withToolbarTitle("رشیدالدین وطواط")))

        for (i in 1 .. 6)
            onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft())

        onView(allOf(withId(R.id.toolbar), isDisplayingAtLeast(50)))
            .check(matches(withToolbarTitle("ابوسعید ابوالخیر")))

    }


}

