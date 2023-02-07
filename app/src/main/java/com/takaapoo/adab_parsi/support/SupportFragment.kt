package com.takaapoo.adab_parsi.support

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentSupportBinding
import com.takaapoo.adab_parsi.util.GlideApp
import com.takaapoo.adab_parsi.util.rateApp
import com.takaapoo.adab_parsi.util.topPadding

class SupportFragment: Fragment() {

    private var _binding: FragmentSupportBinding? = null
    private val binding get() = _binding!!
    private var mediaPlayer: MediaPlayer? = null

    private var salavatInAnimator : Animator? = null
    private var salavatOutAnimator : Animator? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        _binding = FragmentSupportBinding.inflate(inflater, container, false)

        val navController = findNavController()
        binding.supportToolbar.setupWithNavController(navController,
            AppBarConfiguration.Builder(navController.graph).build())

        GlideApp.with(this).load(R.drawable.salavat_in_2).into(binding.salavatIn)
        GlideApp.with(this).load(R.drawable.salavat_out_2).into(binding.salavatOut)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.supportToolbar.setPadding(0, topPadding, 0, 0)
        binding.supportToolbar.navigationContentDescription = resources.getString(R.string.navigation_up)
//        barsPreparation()

        salavatInAnimator =
            AnimatorInflater.loadAnimator(requireContext(), R.animator.salavat_rotate_in).apply {
                setTarget(binding.salavatIn)
                start()
            }
        salavatOutAnimator =
            AnimatorInflater.loadAnimator(requireContext(), R.animator.salavat_rotate_out).apply {
                setTarget(binding.salavatOut)
                start()
            }

        binding.buttonShare.setOnClickListener { shareApp() }
        binding.buttonRate.setOnClickListener {
            rateApp(requireActivity(), true)
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit().putBoolean("RatedAyeneJan", true).apply()
        }

        (activity as? MainActivity)?.analyticsLogEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, "Support screen")
            }
        )
        Firebase.crashlytics.setCustomKey("Enter Screen", "Support screen")
    }

    override fun onStart() {
        super.onStart()
        val sharedPreference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (!sharedPreference.getBoolean("sound", false)) {
            if (mediaPlayer == null){
                mediaPlayer = MediaPlayer.create(requireContext(), R.raw.salavat)?.apply {
                    isLooping = true
                    start()
                }
            } else
                mediaPlayer?.start()
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        salavatInAnimator?.end()
        salavatOutAnimator?.end()
        salavatInAnimator = null
        salavatOutAnimator = null

        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun shareApp(){
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK

            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.introduce_app_subj))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.introduce_app_text))
        }
        startActivity(Intent.createChooser(intent, "share via"))
    }

}