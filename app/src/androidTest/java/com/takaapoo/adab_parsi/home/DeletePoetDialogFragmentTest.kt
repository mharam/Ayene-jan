package com.takaapoo.adab_parsi.home

import android.view.View
import android.widget.Button
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.launchFragmentInHiltContainer
import com.takaapoo.adab_parsi.util.engNumToFarsiNum
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject


@SmallTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeletePoetDialogFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue lateinit var homeViewModel: HomeViewModel
    @Inject lateinit var dao: Dao
    private val selectedPoetNum = 1

    @Before
    fun init(){
        hiltRule.inject()
//        homeViewModel = HomeViewModel(getApplicationContext(), dao)
    }

    @Test
    fun checkDialogStructure(){
        val resources = getInstrumentation().targetContext.resources
//        homeViewModel.apply {
//            selectedPoetCount = selectedPoetNum
//            deleteDialogTitle = resources.getString(
//                R.string.delete_poet_title,
//                engNumToFarsiNum(selectedPoetNum)
//            )
//        }
        val message = resources.getQuantityString(R.plurals.delete_poet_message, selectedPoetNum)

        launchFragmentInHiltContainer<DeletePoetDialogFragment>(themeResId = R.style.Theme_MyApp) {
            view?.layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        onView(withText(message)).check(matches(isDisplayed()))
        onView(withText(R.string.delete_poet_pos_button)).check(matches(isAssignableFrom(Button::class.java)))
        onView(withText(R.string.delete_poet_pos_button)).check(matches(isAssignableFrom(Button::class.java)))
        onView(withText(R.string.delete_poet_pos_button)).check(matches(hasSibling(withText(R.string.delete_poet_neg_button))))
    }

}