package com.takaapoo.adab_parsi.home

import android.graphics.Rect
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.add.ARG_ADD_PAGE
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.database.FakeAndroidTestDao
import com.takaapoo.adab_parsi.di.DatabaseModule
import com.takaapoo.adab_parsi.launchFragmentInHiltContainer
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers.startsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@UninstallModules(DatabaseModule::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@MediumTest
class HomePagerFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue val dao: Dao = FakeAndroidTestDao()
    lateinit var categories: MutableList<Category>

    @Before
    fun setUp() = runBlockingTest {
        categories = mutableListOf(
            Category(100012, 0, 50125, "حسن آرام*محمدحسن بن مجتبی آرام (متولد 1366 ه.ش.)",
                0, "", 1002L, 1) ,
            Category(200010, 0, 55000, "زینب آرام*محمدحسن بنت مجتبی آرام (متولد 1369 ه.ش.)",
                0, "", 8502L, 1) ,
            Category(300010, 0, 60000, "علی آرام*محمدحسن بن مجتبی آرام (متولد 1362 ه.ش.)",
                0, "", 6702L, 1)
        )
        dao.insertDatabase(categories, emptyList(), emptyList(), emptyList())
    }

    @Test
    fun checkRecyclerViewItems(){
        val fragmentBundle = bundleOf(ARG_ADD_PAGE to 0)
        launchFragmentInHiltContainer<HomePagerFragment>(themeResId = R.style.Theme_MyApp,
            fragmentArgs = fragmentBundle)
        onView(withId(R.id.loaded_poet_list)).check(matches(hasDescendant(withText(startsWith("زینب")))))
    }

    @Test
    fun scrollPoetList_getFirstRectangle(){
        val fragmentBundle = bundleOf(ARG_ADD_PAGE to 0)
        val frag = launchFragmentInHiltContainer<HomePagerFragment>(themeResId = R.style.Theme_MyApp,
            fragmentArgs = fragmentBundle)

        onView(withId(R.id.loaded_poet_list)).perform(scrollToPosition<RecyclerView.ViewHolder>(2))
        val rect = (frag as HomePagerFragment).firstItemRectangle()

        val itemRect = Rect()
        frag.view?.findViewById<RecyclerView>(R.id.loaded_poet_list)?.getChildAt(1)?.apply {
            itemRect.set(left, top, right, bottom)
        }
        assertThat(rect).isEqualTo(itemRect)
    }

    @Test
    fun longClickAnItem_selectionTrackerHoldItem(){
        val fragmentBundle = bundleOf(ARG_ADD_PAGE to 0)
        val frag = launchFragmentInHiltContainer<HomePagerFragment>(themeResId = R.style.Theme_MyApp,
            fragmentArgs = fragmentBundle)

        onView(withId(R.id.loaded_poet_list))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))

        assertThat((frag as HomePagerFragment).getSelectedPoetList()).contains(categories[0].poetID)
    }


}