package com.takaapoo.adab_parsi.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentAboutBinding
import com.takaapoo.adab_parsi.util.barsPreparation
import com.takaapoo.adab_parsi.util.topPadding

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        _binding = FragmentAboutBinding.inflate(inflater, container, false)

        val navController = findNavController()
        binding.aboutToolbar.setupWithNavController(navController,
            AppBarConfiguration.Builder(navController.graph).build())

        binding.aboutToolbar.navigationContentDescription = resources.getString(R.string.navigation_up)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.aboutToolbar.setPadding(0, topPadding, 0, 0)
        barsPreparation()

        (activity as? MainActivity)?.analyticsLogEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, "About screen")
            }
        )
        Firebase.crashlytics.setCustomKey("Enter Screen", "About screen")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}