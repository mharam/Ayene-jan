package com.takaapoo.adab_parsi.poet

import android.view.View
import androidx.fragment.app.Fragment

abstract class FragmentWithTransformPage: Fragment() {

    abstract fun transformPage(view: View, position: Float)

}