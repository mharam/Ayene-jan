package com.takaapoo.adab_parsi.setting

import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
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
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.crashlytics
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.poem.PoemEvent
import com.takaapoo.adab_parsi.poem.PoemViewModel
import com.takaapoo.adab_parsi.util.BounceEdgeEffectFactory
import com.takaapoo.adab_parsi.util.Orientation
import com.takaapoo.adab_parsi.util.dpTOpx
import com.takaapoo.adab_parsi.util.topPadding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SettingFragment: PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val settingViewModel: SettingViewModel by activityViewModels()
//    private val poemViewModel: PoemViewModel by activityViewModels()
    private lateinit var navController: NavController

    private var borderPreference: Preference? = null
    private var paperBorderPreference: Preference? = null
    private var paperCornerPreference: Preference? = null
    private var fontPreference: Preference? = null
    private var fontSizePreference: Preference? = null
    private var paperPreference: Preference? = null
    private var hilightPreference: Preference? = null

    private val borderIds = arrayOf(R.drawable.frame0_1, R.drawable.frame1_1, R.drawable.frame2_1)
    private val paperBorderIds = arrayOf(R.drawable.frame5_3, R.drawable.frame6_3, R.drawable.frame7_3)
    private val paperCornerIds = arrayOf(R.drawable.border, R.drawable.border2, R.drawable.border3)
    private val hilightMarkerColorIds = arrayOf(R.color.hilight_1_marker, R.color.hilight_2_marker,
        R.color.hilight_3_marker)
    private lateinit var borderNames: Array<String>
    private lateinit var paperBorderNames: Array<String>
    private lateinit var paperCornerNames: Array<String>
    private lateinit var fontNames: Array<String>
    private lateinit var paperNames: Array<String>
    private lateinit var hilightNames: Array<String>
    private lateinit var fontSizeNames: Array<String>


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
            ClearHistoryDialogFragment().show(parentFragmentManager, "ClearHistory")
            true
        }

        borderPreference = findPreference("border")
        borderPreference?.setOnPreferenceClickListener {
            BorderSelectDialogFragment().show(parentFragmentManager, "Border")
            true
        }
        borderNames = resources.getStringArray(R.array.border_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "border")

        paperBorderPreference = findPreference("paper_border")
        paperBorderPreference?.setOnPreferenceClickListener {
            PaperBorderSelectDialogFragment(settingViewModel::updatePaperBorder)
                .show(parentFragmentManager, "PaperBorder")
            true
        }
        paperBorderNames = resources.getStringArray(R.array.paper_border_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "paper_border")

        paperCornerPreference = findPreference("paper_corner")
        paperCornerPreference?.setOnPreferenceClickListener {
            PaperCornerSelectDialogFragment(settingViewModel::updatePaperCorner)
                .show(parentFragmentManager, "PaperCorner")
            true
        }
        paperCornerNames = resources.getStringArray(R.array.paper_corner_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "paper_corner")

        fontPreference = findPreference("font")
        fontPreference?.setOnPreferenceClickListener {
            FontSelectDialogFragment().show(parentFragmentManager, "Font")
            true
        }
        fontNames = resources.getStringArray(R.array.font_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "font")

        paperPreference = findPreference("paper_color")
        paperPreference?.setOnPreferenceClickListener {
            PaperSelectDialogFragment().show(parentFragmentManager, "Paper")
            true
        }
        paperNames = resources.getStringArray(R.array.paper_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "paper_color")
        paperPreference?.isEnabled = settingViewModel.currentNightMode == Configuration.UI_MODE_NIGHT_NO

        hilightPreference = findPreference("hilight")
        hilightPreference?.setOnPreferenceClickListener {
            HilightSelectDialogFragment().show(parentFragmentManager, "Hilight")
            true
        }
        hilightNames = resources.getStringArray(R.array.hilight_entries)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "hilight")

        fontSizePreference = findPreference("font_size")
        fontSizePreference?.setOnPreferenceClickListener {
            try {
                navController.navigate(
                    SettingFragmentDirections.actionSettingFragmentToSettingFontSizeFragment())
            } catch (_: Exception) { }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settingToolbar = view.findViewById<Toolbar>(R.id.setting_toolbar)
//        settingToolbar.setPadding(0, topPadding, 0, 0)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = insets.top
            }
            view.findViewById<FrameLayout>(android.R.id.list_container)?.getChildAt(0)?.apply {
                (this as RecyclerView).edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)
                this.setPadding(
                    this.paddingLeft,
                    this.paddingTop,
                    this.paddingRight,
                    insets.bottom
                )
                this.clipToPadding = false
                isVerticalScrollBarEnabled = false
            }
            windowInsets
        }


        navController = findNavController()
//        val appBarConfiguration = (requireActivity() as MainActivity).appBarConfiguration
//        (view.setting_toolbar).setupWithNavController(navController, appBarConfiguration)
        settingToolbar.setupWithNavController(navController,
            AppBarConfiguration.Builder(navController.graph).build())

        onSharedPreferenceChanged(preferenceManager.sharedPreferences, "font_size")
        settingToolbar.navigationContentDescription = resources.getString(R.string.navigation_up)

        (activity as? MainActivity)?.analyticsLogEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, "Setting screen")
            }
        )
        Firebase.crashlytics.setCustomKey("Enter Screen", "Setting screen")
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
            }
            "font" -> {
                val prefValue = sharedPreferences?.getInt(key, 0) ?: 0
                fontPreference?.summary = fontNames[prefValue]
            }
            "font_size" -> {
                val prefValue = sharedPreferences?.getInt(key, 1) ?: 1
                fontSizePreference?.summary = fontSizeNames[prefValue]
            }
            "paper_color" -> {
                val prefValue = sharedPreferences?.getInt(key, 0) ?: 0
                paperPreference?.summary = paperNames[prefValue]
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
            "paper_border" -> {
                val prefValue = sharedPreferences?.getInt(key, 1) ?: 1
                val shapeDrawable = if (prefValue > 0)
                    ResourcesCompat.getDrawable(
                        resources,
                        paperBorderIds[prefValue - 1],
                        context?.theme
                    ) else null
                paperBorderPreference?.summary = paperBorderNames[prefValue]
                paperBorderPreference?.icon = shapeDrawable
            }
            "paper_corner" -> {
                val prefValue = sharedPreferences?.getInt(key, 1) ?: 1
                val shapeDrawable = if (prefValue > 0)
                    ResourcesCompat.getDrawable(
                        resources,
                        paperCornerIds[prefValue - 1],
                        context?.theme
                    ) else null
                paperCornerPreference?.summary = paperCornerNames[prefValue]
                paperCornerPreference?.icon = shapeDrawable
            }
        }
    }

}




class ClearHistoryDialogFragment : DialogFragment() {
    val settingViewModel: SettingViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(it)
//                .setIcon(R.drawable.ic_baseline_delete_24)
                .setTitle(R.string.delete_search_history_title)
                .setMessage(resources.getString(R.string.delete_search_history_mess))
                .setPositiveButton(R.string.delete_search_pos_button){ _, _ ->
                    settingViewModel.deleteSearchHistory()
                    Toast.makeText(this.context, "حافظه جستجو پاک شد", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class BorderSelectDialogFragment : DialogFragment() {
    val settingViewModel: SettingViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val borderListAdapter = BorderListAdapter(requireActivity())
        val listView = layoutInflater.inflate(R.layout.setting_border_list, null) as ListView
        listView.adapter = borderListAdapter

        val builder = activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.border)
                .setView(listView)
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")

        listView.setOnItemClickListener { _, _, position, _ ->
            settingViewModel.updateBorder(position)
            builder.dismiss()
        }

        return builder
    }
}

class PaperBorderSelectDialogFragment(
    private val onUpdatePaperBorder: (Int) -> Unit
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val borderListAdapter = PaperBorderListAdapter(requireActivity())
        val listView = layoutInflater.inflate(R.layout.setting_border_list, null) as ListView
        listView.adapter = borderListAdapter

        val builder = activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.border)
                .setView(listView)
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")

        listView.setOnItemClickListener { _, _, position, _ ->
            onUpdatePaperBorder(position)
            builder.dismiss()
        }

        return builder
    }
}

class PaperCornerSelectDialogFragment(
    private val onUpdatePaperCorner: (Int) -> Unit
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val borderListAdapter = PaperCornerListAdapter(requireActivity())
        val listView = layoutInflater.inflate(R.layout.setting_border_list, null) as ListView
        listView.adapter = borderListAdapter

        val builder = activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.corner)
                .setView(listView)
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")

        listView.setOnItemClickListener { _, _, position, _ ->
            onUpdatePaperCorner(position)
            builder.dismiss()
        }

        return builder
    }
}

class FontSelectDialogFragment : DialogFragment() {
    val settingViewModel: SettingViewModel by activityViewModels()
    val poemViewModel: PoemViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.font)
                .setSingleChoiceItems(FontListAdapter(it), 0){ _, which ->
                    settingViewModel.updateFont(which)
                    poemViewModel.reportEvent(PoemEvent.OnRefreshContent)
                    dismiss()
                }
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class PaperSelectDialogFragment : DialogFragment() {
    val settingViewModel: SettingViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.paper)
                .setSingleChoiceItems(PaperListAdapter(it), 0){ _, which ->
                    settingViewModel.updatePaper(which)
                    dismiss()
                }
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class HilightSelectDialogFragment : DialogFragment() {
    val settingViewModel: SettingViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.hilight_color)
                .setSingleChoiceItems(HilightListAdapter(it), 0){ _, which ->
                    settingViewModel.updateHilight(which)
                    dismiss()
                }
                .setNegativeButton(R.string.cancel){ _: DialogInterface, _: Int -> dismiss() }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
