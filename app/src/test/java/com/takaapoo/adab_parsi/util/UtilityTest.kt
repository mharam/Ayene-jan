package com.takaapoo.adab_parsi.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import com.takaapoo.adab_parsi.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class UtilityTest {

    @Test
    fun makeTextBiErab_emptyString_returnEmptyString(){
        val input = ""
        val result = makeTextBiErab(input)

        assertThat(result).isEqualTo("")
    }

    @Test
    fun mySubString_spaceNo_returnStringBetweenSpaces() {

        val context = ApplicationProvider.getApplicationContext<Context>()
        val stringUnderTest = context.getString(R.string.rate_message)
        val result = stringUnderTest.mySubString(0, 1)

        assertThat(result).isEqualTo("آینه")
    }

    @Test
    fun charFinder_stringWithGoalChars_returnTrue(){
        val stringUnderTest = "الّا"
        val result = stringUnderTest.charFinder(goalChars)
        assertThat(result).isFalse()
    }

    @Test
    fun myIndexOfAny_stringWithGoalChars_returnSuitableIndex(){
        val stringUnderTest = "اله"
        val result = stringUnderTest.myIndexOfAny(goalChars.toCharArray())
        assertThat(result).isEqualTo(1)
    }

}