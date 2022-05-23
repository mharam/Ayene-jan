package com.takaapoo.adab_parsi

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import com.takaapoo.adab_parsi.database.Dao
import org.junit.Before
import org.junit.Test
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.takaapoo.adab_parsi.util.DataBindingIdlingResource
import com.takaapoo.adab_parsi.util.EspressoIdlingResource
import com.takaapoo.adab_parsi.util.monitorActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@LargeTest
class MainActivityTest{

    @get:Rule
    var hiltRule = HiltAndroidRule(this)


    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init(){

    }

//    @After
//    fun reset(){
//    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }


    @Test
    fun longClickACard_CardSelected(){ // longClick works the same as click!!!
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // THEN - Long Click a poet cause its checkBox to be checked
        onView(withId(R.id.loaded_poet_list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick())
        )

        onView(withId(R.id.checkBox)).check(matches(isChecked()))

        activityScenario.close()
    }

    @Test
    fun clickAPoetCard_PoetLibraryOpened(){
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // THEN - Click a poet cause the poet library opens
        onView(withId(R.id.loaded_poet_list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )

//        onView(withText("ابوسعید ابوالخیر")).check(matches(isDisplayed()))
        onView(withId(R.id.toolbar)).check(matches(withToolbarTitle("حافظ")))

        onView(withId(R.id.biography)).perform(click())
        onView(withId(R.id.bottomsheet)).check(matches(isDisplayed()))

        activityScenario.close()
    }

}

fun withToolbarTitle(title: CharSequence?): Matcher<View?> {
    return withToolbarTitle(`is`(title))
}

fun withToolbarTitle(textMatcher: Matcher<CharSequence?>): Matcher<View?> {
    return object : BoundedMatcher<View?, Toolbar>(Toolbar::class.java) {
        override fun matchesSafely(toolbar: Toolbar): Boolean {
            return textMatcher.matches(toolbar.title)
        }

        override fun describeTo(description: Description) {
            description.appendText("with toolbar title: ")
            textMatcher.describeTo(description)
        }
    }
}