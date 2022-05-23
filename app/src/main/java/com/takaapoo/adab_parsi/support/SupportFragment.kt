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
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentSupportBinding
import com.takaapoo.adab_parsi.util.GlideApp
import com.takaapoo.adab_parsi.util.barsPreparation
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
        barsPreparation()

//        val outBorder = ResourcesCompat.getDrawable(resources, R.drawable.salavat_out, null)!!
//        val inBorder = ResourcesCompat.getDrawable(resources, R.drawable.salavat_in, null)!!
//        view.doOnPreDraw {
//            File(context?.filesDir, "salavatOut.png").let {
//                if (!it.exists()){
//                    val bitmap = Bitmap.createBitmap(view.width, view.width, Bitmap.Config.ARGB_8888)
//                    val canvas = Canvas(bitmap)
//                    outBorder.setBounds(0, 0, canvas.width, canvas.height)
//                    for (i in 0 until 360 step 36){
//                        outBorder.draw(canvas)
//                        canvas.rotate(36f, canvas.width / 2f, canvas.height / 2f)
//                    }
//                    val pngOutFile = FileOutputStream("${context?.filesDir}/salavatOut.png")
//                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngOutFile)
//                }
//            }
//            binding.salavatOut.setImageBitmap(BitmapFactory.decodeFile("${context?.filesDir}/salavatOut.png"))
//
//            File(context?.filesDir, "salavatIn.png").let {
//                if (!it.exists()){
//                    val bitmap = Bitmap.createBitmap(view.width, view.width, Bitmap.Config.ARGB_8888)
//                    val canvas = Canvas(bitmap)
//                    inBorder.setBounds(0, 0, canvas.width, canvas.height)
//                    for (i in 0 until 360 step 36){
//                        inBorder.draw(canvas)
//                        canvas.rotate(36f, canvas.width / 2f, canvas.height / 2f)
//                    }
//                    val pngOutFile = FileOutputStream("${context?.filesDir}/salavatIn.png")
//                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngOutFile)
//                }
//            }
//            binding.salavatIn.setImageBitmap(BitmapFactory.decodeFile("${context?.filesDir}/salavatIn.png"))
//        }

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