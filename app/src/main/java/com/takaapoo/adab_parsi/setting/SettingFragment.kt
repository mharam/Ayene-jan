package com.takaapoo.adab_parsi.setting

import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.poem.PoemViewModel
import com.takaapoo.adab_parsi.util.BounceEdgeEffectFactory
import com.takaapoo.adab_parsi.util.Orientation
import com.takaapoo.adab_parsi.util.barsPreparation
import com.takaapoo.adab_parsi.util.topPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_setting.view.*
import java.lang.Exception


@AndroidEntryPoint
class SettingFragment: PreferenceFragmentCompat(), ClearHistoryDialogFragment.ClearDialogListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val settingViewModel: SettingViewModel by activityViewModels()
    private val poemViewModel: PoemViewModel by activityViewModels()
    private lateinit var navController: NavController

    private var borderPreference: Preference? = null
    private var fontPreference: Preference? = null
    private var fontSizePreference: Preference? = null
    private var paperPreference: Preference? = null
    private var hilightPreference: Preference? = null

    private val borderIds = arrayOf(R.drawable.frame0_1, R.drawable.frame1_1, R.drawable.frame2_1)
//    private val paperColorIds = arrayOf(R.color.paper_1, R.color.paper_2, R.color.paper_3)
    private val hilightMarkerColorIds = arrayOf(R.color.hilight_1_marker, R.color.hilight_2_marker,
        R.color.hilight_3_marker)
    private lateinit var borderNames: Array<String>
    private lateinit var fontNames: Array<String>
    private lateinit var paperNames: Array<String>
    private lateinit var hilightNames: Array<String>
    private lateinit var fontSizeNames: Array<String>

//    var currentNightMode = 0


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setting, rootKey)
        settingViewModel.currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
//        currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

//        preferenceManager.sharedPreferences.edit().clear().apply()

//        val sound = findPreference<Preference>("sound")
//        sound?.setOnPreferenceClickListener {
//            val switchState = preferenceManager.sharedPreferences.getBoolean("sound", false)
//            Timber.i("switchState = $switchState")
//            val amanager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
//            if (Build.VERSION.SDK_INT > 22)
//                amanager!!.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
//            else
//                amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, switchState)
//
//            true
//        }


        if (Build.VERSION.SDK_INT <= 28)
            findPreference<ListPreference>("theme")?.entries = resources.getStringArray(R.array.theme_entries_old)

        findPreference<Preference>("delete_history")?.setOnPreferenceClickListener {
            ClearHistoryDialogFragment(this).show(parentFragmentManager, "ClearHistory")
            true
        }

        borderPreference = findPreference("border")
        borderPreference?.setOnPreferenceClickListener {
            BorderSelectDialogFragment(settingViewModel).show(parentFragmentManager, "Border")
            true
        }
        borderNames = resources.getStringArray(R.array.border_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "border")

        fontPreference = findPreference("font")
        fontPreference?.setOnPreferenceClickListener {
            FontSelectDialogFragment(settingViewModel, poemViewModel).show(parentFragmentManager, "Font")
            true
        }
        fontNames = resources.getStringArray(R.array.font_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "font")

        paperPreference = findPreference("paper_color")
        paperPreference?.setOnPreferenceClickListener {
            PaperSelectDialogFragment(settingViewModel).show(parentFragmentManager, "Paper")
            true
        }
        paperNames = resources.getStringArray(R.array.paper_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "paper_color")
        paperPreference?.isEnabled = settingViewModel.currentNightMode == Configuration.UI_MODE_NIGHT_NO

        hilightPreference = findPreference("hilight")
        hilightPreference?.setOnPreferenceClickListener {
            HilightSelectDialogFragment(settingViewModel).show(parentFragmentManager, "Hilight")
            true
        }
        hilightNames = resources.getStringArray(R.array.hilight_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "hilight")

        fontSizePreference = findPreference("font_size")
        fontSizePreference?.setOnPreferenceClickListener {
            try {
                navController.navigate(
                    SettingFragmentDirections.actionSettingFragmentToSettingFontSizeFragment())
            } catch (e: Exception) { }

            true
        }
        fontSizeNames = resources.getStringArray(R.array.font_size_entries)

    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        barsPreparation()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setting_toolbar.setPadding(0, topPadding, 0, 0)
        view.findViewById<FrameLayout>(android.R.id.list_container)?.getChildAt(0)?.apply {
            (this as RecyclerView).edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)
            isVerticalScrollBarEnabled = false
        }

        navController = findNavController()
//        val appBarConfiguration = (requireActivity() as MainActivity).appBarConfiguration
//        (view.setting_toolbar).setupWithNavController(navController, appBarConfiguration)
        (view.setting_toolbar).setupWithNavController(navController,
            AppBarConfiguration.Builder(navController.graph).build())

        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "font_size")
        view.setting_toolbar.navigationContentDescription = resources.getString(R.string.navigation_up)
    }

    override fun onClearDialogPositiveClick() {
        settingViewModel.deleteSearchHistory()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key){
            "screen_on" -> {
                val prefValue = sharedPreferences?.getBoolean(key, false) ?: false
                if (prefValue)
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            "theme" -> {
                val prefValue = sharedPreferences?.getString(key, "2") ?: "2"
                settingViewModel.themePref = prefValue
                AppCompatDelegate.setDefaultNightMode(when(prefValue){
                    "0" -> AppCompatDelegate.MODE_NIGHT_NO
                    "1" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> if (Build.VERSION.SDK_INT > 28) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                })
                barsPreparation()
//                settingViewModel.currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
//                paperPreference?.isEnabled = settingViewModel.currentNightMode == Configuration.UI_MODE_NIGHT_NO

            }
            "font" -> {
                val prefValue = sharedPreferences?.getInt(key, 0) ?: 0
                fontPreference?.summary = fontNames[prefValue]
//                settingViewModel.updateFont(prefValue)
            }
            "font_size" -> {
                val prefValue = sharedPreferences?.getInt(key, 1) ?: 1
                fontSizePreference?.summary = fontSizeNames[prefValue]
//                settingViewModel.apply {
//                    fontSize = prefValue
//                    fontSizePref = prefValue
//                    updateConstants()
//                }
            }
            "paper_color" -> {
                val prefValue = sharedPreferences?.getInt(key, 0) ?: 0
                paperPreference?.summary = paperNames[prefValue]
//                settingViewModel.apply {
//                    paperColorPref = prefValue
//                    paperColor = ResourcesCompat.getColor(resources, paperColorIds[prefValue],
//                        context.theme
//                    )
//                }

                val shape = ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_paper_icon, context?.theme
                )!!
                shape.setTintMode(PorterDuff.Mode.MULTIPLY)
                shape.setTint(settingViewModel.paperColor)
                paperPreference?.icon = shape
            }
            "hilight" -> {
                val prefValue = sharedPreferences?.getInt(key, 0) ?: 0
                hilightPreference?.summary = hilightNames[prefValue]

                val shape = ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_highlighter, context?.theme
                )!!
                shape.setTint(ResourcesCompat.getColor(resources,
                    hilightMarkerColorIds[settingViewModel.hilightColorPref], context?.theme))
                hilightPreference?.icon = shape
            }
            "border" -> {
                val prefValue = sharedPreferences?.getInt(key, 2) ?: 2
                borderPreference?.summary = borderNames[prefValue]
                borderPreference?.icon = ResourcesCompat.getDrawable(
                    resources,
                    borderIds[prefValue],
                    context?.theme
                )
            }
        }
    }

}




class ClearHistoryDialogFragment(val listener: ClearDialogListener) : DialogFragment() {

    interface ClearDialogListener {
        fun onClearDialogPositiveClick()
//        fun onClearDialogNegativeClick()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(it)
//                .setIcon(R.drawable.ic_baseline_delete_24)
                .setTitle(R.string.delete_search_history_title)
                .setMessage(resources.getString(R.string.delete_search_history_mess))
                .setPositiveButton(R.string.delete_search_pos_button){ _, _ ->
                    listener.onClearDialogPositiveClick()
                    Toast.makeText(this.context, "?????????? ?????????? ?????? ????", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class BorderSelectDialogFragment(private val stvm: SettingViewModel) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val borderListAdapter = BorderListAdapter(requireActivity())
        val listView = LayoutInflater.from(this.context)
            .inflate(R.layout.setting_border_list, null) as ListView
        listView.adapter = borderListAdapter

        val builder = activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.border)
                .setView(listView)
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")

//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())

        listView.setOnItemClickListener { parent, view, position, id ->
//            sharedPreferences.edit().putInt("border", position).apply()
            stvm.updateBorder(position)
            builder.dismiss()
        }

        return builder
    }
}

class FontSelectDialogFragment(private val stvm: SettingViewModel, private val pvm: PoemViewModel)
    : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fontListAdapter = FontListAdapter(requireActivity())
//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())

        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.font)
                .setSingleChoiceItems(fontListAdapter, 0){ dialog, which ->
                    stvm.updateFont(which)
                    pvm.refresh()
//                    sharedPreferences.edit().putInt("font", which).apply()
                    dismiss()
                }
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class PaperSelectDialogFragment(private val stvm: SettingViewModel) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val paperListAdapter = PaperListAdapter(requireActivity())
//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())

        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.paper)
                .setSingleChoiceItems(paperListAdapter, 0){ dialog, which ->
                    stvm.updatePaper(which)
//                    sharedPreferences.edit().putInt("paper_color", which).apply()
                    dismiss()
                }
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class HilightSelectDialogFragment(private val stvm: SettingViewModel) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val hilightListAdapter = HilightListAdapter(requireActivity())
//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())

        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.hilight_color)
                .setSingleChoiceItems(hilightListAdapter, 0){ dialog, which ->
                    stvm.updateHilight(which)
//                    sharedPreferences.edit().putInt("hilight", which).apply()
                    dismiss()
                }
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
