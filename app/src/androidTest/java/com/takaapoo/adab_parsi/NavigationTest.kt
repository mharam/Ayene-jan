package com.takaapoo.adab_parsi

import android.app.Activity
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import androidx.appcompat.widget.Toolbar
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.takaapoo.adab_parsi.util.DataBindingIdlingResource
import com.takaapoo.adab_parsi.util.EspressoIdlingResource
import com.takaapoo.adab_parsi.util.monitorActivity
import kotlinx.android.synthetic.main.activity_main.*
import com.google.common.truth.Truth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@LargeTest
class NavigationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private lateinit var activityScenario: ActivityScenario<MainActivity>

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        activityScenario.close()
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun openCloseDrawerView(){
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Check if drawer is closed first
        onView(withId(R.id.drawer_layout)).check(matches(isClosed(Gravity.START)))

        // Click on drawer icon or swipe from right edge
//        onView(withId(R.id.hamburger)).perform(click())
        onView(withId(R.id.drawer_layout)).perform(swipeFromRightEdgeToCenter())

        // Check if drawer is open now
        onView(withId(R.id.drawer_layout)).check(matches(isOpen(Gravity.START)))

        // click on somewhere outside the drawer to close it
        onView(withId(R.id.drawer_layout)).perform(clickXY(0, 200))

        // Check if drawer is close now
        onView(withId(R.id.drawer_layout)).check(matches(isClosed(Gravity.START)))
    }

    @Test
    fun homeScreen_ToPoetScreen_ThenUpButton(){
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withText("جامی")).perform(click())

//        onView(withContentDescription(activityScenario.getToolbarNavigationContentDescription())).
//            perform(click())

        pressBack()

        onView(withId(R.id.hamburger)).check(matches(isDisplayed()))
    }

//    @Test
//    fun tapSearchBar_ReturnBackToHome(){
//        dataBindingIdlingResource.monitorActivity(activityScenario)
//
//        val navController = TestNavHostController(getApplicationContext())
//        activityScenario.onActivity {
//            it.runOnUiThread {
//                navController.setGraph(R.navigation.nav_graph)
//                Navigation.setViewNavController(
//                    it.supportFragmentManager.findFragmentById(R.id.homeFragment)?.view!!, navController)
//            }
//        }
//
//        Truth.assertThat(navController.currentDestination?.id).isEqualTo(R.id.homeFragment)
//        onView(withId(R.id.toolbar)).perform(click())
//        Truth.assertThat(navController.currentDestination?.id).isEqualTo(R.id.homeFragment)
//    }


}


fun <T : Activity> ActivityScenario<T>.getToolbarNavigationContentDescription(): String {
    var description = ""
    onActivity {
        description = it.findViewById<Toolbar>(R.id.toolbar).navigationContentDescription as String
    }
    return description
}

fun swipeFromRightEdgeToCenter(): ViewAction {
    return GeneralSwipeAction(
        Swipe.FAST, GeneralLocation.CENTER_RIGHT,
        GeneralLocation.CENTER, Press.FINGER
    )
}

fun clickXY(x: Int, y: Int): ViewAction {
    return GeneralClickAction(
        Tap.SINGLE,
        { view ->
            val screenPos = IntArray(2)
            view.getLocationOnScreen(screenPos)
            val screenX = (screenPos[0] + x).toFloat()
            val screenY = (screenPos[1] + y).toFloat()
            floatArrayOf(screenX, screenY)
        },
        Press.FINGER, InputDevice.SOURCE_UNKNOWN, MotionEvent.EDGE_LEFT
    )
}