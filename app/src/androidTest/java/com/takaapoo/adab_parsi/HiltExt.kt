package com.takaapoo.adab_parsi

import android.app.ProgressDialog.show
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.core.util.Preconditions
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import timber.log.Timber

/**
 * launchFragmentInContainer from the androidx.fragment:fragment-testing library
 * is NOT possible to use right now as it uses a hardcoded Activity under the hood
 * (i.e. [EmptyFragmentActivity]) which is not annotated with @AndroidEntryPoint.
 *
 * As a workaround, use this function that is equivalent. It requires you to add
 * [HiltTestActivity] in the debug folder and include it in the debug AndroidManifest.xml file
 * as can be found in this project.
 */
inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    fragmentFactory: FragmentFactory? = null,
    crossinline action: Fragment.() -> Unit = {}
): Fragment? {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    ).putExtra(
        "androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY",
        themeResId
    )

    var fragment: Fragment? = null

    ActivityScenario.launch<HiltTestActivity>(startActivityIntent).onActivity { activity ->
        if (fragmentFactory != null) {
            activity.supportFragmentManager.fragmentFactory = fragmentFactory
        }
        fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
            Preconditions.checkNotNull(T::class.java.classLoader),
            T::class.java.name
        )
        fragment!!.arguments = fragmentArgs

        if (fragment is DialogFragment){
            (fragment as DialogFragment).show(activity.supportFragmentManager, "")
        } else {
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, fragment!!, "")
                .commitNow()
        }

        fragment!!.action()
    }
    return fragment
}

