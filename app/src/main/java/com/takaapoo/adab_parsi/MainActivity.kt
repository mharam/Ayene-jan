package com.takaapoo.adab_parsi

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.animation.doOnEnd
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.takaapoo.adab_parsi.add.AddViewModel
import com.takaapoo.adab_parsi.add.TempDao
import com.takaapoo.adab_parsi.add.TempDatabase
import com.takaapoo.adab_parsi.book.BookEvent
import com.takaapoo.adab_parsi.book.BookHelpState
import com.takaapoo.adab_parsi.book.BookViewModel
import com.takaapoo.adab_parsi.bookmark.BookmarkDetailFragment
import com.takaapoo.adab_parsi.bookmark.BookmarkViewModel
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.database.PoemDatabase
import com.takaapoo.adab_parsi.databinding.ActivityMainBinding
import com.takaapoo.adab_parsi.favorite.FavoriteViewModel
import com.takaapoo.adab_parsi.home.HelpView
import com.takaapoo.adab_parsi.home.HomeEvent
import com.takaapoo.adab_parsi.home.HomeViewModel
import com.takaapoo.adab_parsi.poem.PoemFragment
import com.takaapoo.adab_parsi.poem.PoemViewModel
import com.takaapoo.adab_parsi.poet.PoetEvent
import com.takaapoo.adab_parsi.poet.PoetHelpState
import com.takaapoo.adab_parsi.poet.PoetViewModel
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

enum class AppStore {GooglePlay, Bazaar, Myket}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var firebaseAnalytics: FirebaseAnalytics? = null
    lateinit var settingViewModel: SettingViewModel
    lateinit var poemViewModel: PoemViewModel
    lateinit var poetViewModel: PoetViewModel
    lateinit var homeViewModel: HomeViewModel
    private lateinit var bookViewModel: BookViewModel
    private lateinit var bookmarkViewModel: BookmarkViewModel
    private lateinit var favoriteViewModel: FavoriteViewModel
    lateinit var addViewModel: AddViewModel

    private val languageRTL = true
    private var _binding: ActivityMainBinding? = null
    val binding get() = _binding!!
//    val broadcastReceiver = NetworkReceiver()
    private var imm : InputMethodManager? = null
    private var connectivityManager: ConnectivityManager? = null
    private val mRunnable = Runnable {
        val keyboardIsVisible =
            ViewCompat.getRootWindowInsets(window.decorView)?.isVisible(WindowInsetsCompat.Type.ime())

        if (hidableNavFragment() && keyboardIsVisible == false)
            hideSystemBars()
    }
    private val mHandler = Handler(Looper.getMainLooper())

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            addViewModel.reloadAllPoet()
        }
    }

    var poemActionMode: ActionMode? = null
    private lateinit var poemDao: Dao
    private lateinit var tempDao: TempDao
    private val fibonacci = listOf(15, 25, 40, 65, 105, 170, 275, 445, 720, 1165, 1885)


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        topPadding = savedInstanceState?.getInt("topPadding") ?: 0

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        settingViewModel = ViewModelProvider(this)[SettingViewModel::class.java]
        poemViewModel = ViewModelProvider(this)[PoemViewModel::class.java]
        poetViewModel = ViewModelProvider(this)[PoetViewModel::class.java]
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
//        val homeViewModelFactory = MyViewModelFactory(application, (application as AyeneJanApplication).dao)
        bookmarkViewModel = ViewModelProvider(this)[BookmarkViewModel::class.java]
        favoriteViewModel = ViewModelProvider(this)[FavoriteViewModel::class.java]
        addViewModel = ViewModelProvider(this)[AddViewModel::class.java]

        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        lifecycleScope.launch {
            if (allCategory.isEmpty())
                homeViewModel.getAllCatSuspend()

            _binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.navView.doOnPreDraw {
                val headerView = LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.drawer_header, null)
                headerView.layoutParams = ViewGroup.LayoutParams(it.width, (it.width * 0.7f).toInt())
                binding.navView.addHeaderView(headerView)
            }

            val navController = findNavController(R.id.nav_host_fragment)
            binding.navView.setupWithNavController(navController)

            bookmarkViewModel.bookmarkCount().observe(this@MainActivity) {
                bookmarkViewModel.bookmarkCount = it
                binding.navView.menu.findItem(R.id.book_marks).title =
                    resources.getString(R.string.book_marks, engNumToFarsiNum(it))
            }

            favoriteViewModel.favoriteCount().observe(this@MainActivity) {
                favoriteViewModel.favoriteCount = it
                binding.navView.menu.findItem(R.id.favorites).title =
                    resources.getString(R.string.favorites, engNumToFarsiNum(it))
            }

            binding.navView.setNavigationItemSelectedListener { menuItem ->
                val currentDesID = navController.currentDestination?.id
                try {
                    when (menuItem.itemId){
                        R.id.home -> {
                            if (currentDesID != R.id.homeFragment)
                                navController.navigate(R.id.action_global_homeFragment)
                        }
                        R.id.book_marks -> {
                            if (currentDesID != R.id.bookmarkFragment) {
                                try {
                                    navController.getBackStackEntry(R.id.bookmarkFragment)
                                    navController.popBackStack(R.id.bookmarkFragment, false)
                                } catch (e: IllegalArgumentException) {
                                    navController.navigate(R.id.action_global_bookmarkFragment)
                                }
                            }
                        }
                        R.id.favorites -> {
                            if (currentDesID != R.id.favoriteFragment) {
                                favoriteViewModel.apply {
                                    scroll = 0
                                    favoriteListDisplace = 0
                                    bottomViewedResultHeight = 0
                                    topViewedResultHeight = 0
                                }
                                try {
                                    navController.getBackStackEntry(R.id.favoriteFragment)
                                    navController.popBackStack(R.id.favoriteFragment, false)
                                } catch (e: IllegalArgumentException) {
                                    navController.navigate(R.id.action_global_favoriteFragment)
                                }
                            }
                        }
                        R.id.settings -> {
                            if (currentDesID != R.id.settingFragment) {
                                try {
                                    navController.getBackStackEntry(R.id.settingFragment)
                                    navController.popBackStack(R.id.settingFragment, false)
                                } catch (e: IllegalArgumentException) {
                                    navController.navigate(R.id.action_global_settingFragment)
                                }
                            }
                        }
                        R.id.contact -> {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:") // only email apps should handle this
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@takaapoo.com"))
//                        putExtra(Intent.EXTRA_SUBJECT, "subject")
                                val appVersion = packageManager.getPackageInfo(packageName, 0).versionName
                                putExtra(
                                    Intent.EXTRA_TEXT, resources.getString(
                                        R.string.app_name_and_version,
                                        appVersion,
                                        Build.VERSION.SDK_INT.toString(),
                                        getDeviceName()
                                    )
                                )
                            }
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                            }
                        }
                        R.id.support -> {
                            if (currentDesID != R.id.supportFragment)
                                navController.navigate(R.id.action_global_supportFragment)
                        }
                        R.id.about -> {
                            if (currentDesID != R.id.aboutFragment)
                                navController.navigate(R.id.action_global_aboutFragment)
                        }
                        R.id.help -> {
                            when (currentDesID) {
                                R.id.homeFragment -> homeViewModel.reportEvent(
                                    event = HomeEvent.OnShowHelp(HelpView.ADD_FAB)
                                )
                                R.id.poetFragment -> poetViewModel.reportEvent(
                                    event = PoetEvent.OnShowHelp(PoetHelpState.PAGING)
                                )
                                R.id.bookFragment -> bookViewModel.reportEvent(
                                    event = BookEvent.OnShowHelp(BookHelpState.PAGING)
                                )
                                R.id.poemFragment -> poemViewModel.doShowHelp()
                            }
                        }
                    }
                } catch (_: Exception) { }

                binding.drawerLayout.closeDrawer(GravityCompat.START, true)
                true
            }

//            binding.drawerLayout.doOnPreDraw {
//                val navHostFrag = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
//                val fragView = navHostFrag?.childFragmentManager?.fragments?.firstOrNull()?.view
//                val fragWidth = fragView?.width ?: (2 * it.width)
//                val fragHeight = fragView?.height ?: it.height
//                val bookShelfWidth = (resources.getDimension(R.dimen.book_height_on_shelf) *
//                        fragWidth / fragHeight).coerceAtLeast(resources.getDimension(R.dimen.book_min_width)) * 0.915f
//                bookViewModel.bookWidthMultiplier = fragWidth / bookShelfWidth
//            }

        }

        firebaseAnalytics = Firebase.analytics
        connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        if(Build.VERSION.SDK_INT in 23..25){
            window.navigationBarColor = ResourcesCompat.getColor(
                resources,
                R.color.black_overlay_light,
                theme
            )
        }

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        var appEntranceCount = preferenceManager.getInt("AyeneJanEntranceCount", 0)
        if (savedInstanceState == null){
            appEntranceCount++
            preferenceManager.edit().putInt("AyeneJanEntranceCount", appEntranceCount).apply()
        }
        poemDao = PoemDatabase.getDatabase(this).dao()
        tempDao = TempDatabase.getDatabase(this, "", true).dao()
        if (appEntranceCount == 1)
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.IO) {
                    addHafezToDatabase()
                }
            }
        if (fibonacci.contains(appEntranceCount) &&
            !preferenceManager.getBoolean("RatedAyeneJan", false) && savedInstanceState == null)
                Handler(mainLooper).postDelayed({
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                        RateDialogFragment().show(supportFragmentManager, "Rate_AyeneJan")
                }, 6000)

        if (preferenceManager.getBoolean("screen_on", false))
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        settingViewModel.updateConstants()

        resources.configuration.setLayoutDirection(
            if (languageRTL) Locale("fa") else Locale("en")
        )

        File(filesDir, "poem").let {
            if (it.exists())
                 it.listFiles()?.forEach { file -> file.delete() }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
        addViewModel.reloadAllPoet()

//        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).apply {
//            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
//        }
//        registerReceiver(broadcastReceiver, filter)

        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        barsPreparation()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("topPadding", topPadding)
    }

    override fun onDestroy() {
        super.onDestroy()
//        unregisterReceiver(broadcastReceiver)
        connectivityManager?.unregisterNetworkCallback(networkCallback)
        connectivityManager = null

        _binding = null
    }

    private fun barsPreparation(){
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN){
            val orientationPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val keyboardIsVisible =
                ViewCompat.getRootWindowInsets(window.decorView)?.isVisible(WindowInsetsCompat.Type.ime())

            if (orientationPortrait && hidableNavFragment() && keyboardIsVisible == true){
                mHandler.removeCallbacks(mRunnable)
                mHandler.postDelayed(mRunnable, 3500)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hidableNavFragment(): Boolean{
        val navHostFrag = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val presentFrag = navHostFrag?.childFragmentManager?.fragments?.firstOrNull()

        return (presentFrag is PoemFragment || presentFrag is BookmarkDetailFragment)
    }

    fun moveDrawer(){
        binding.drawerLayout.openDrawer(GravityCompat.START, true)
    }

    fun openPoemFile(uri: Uri){
        val exportIntent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            data = uri
        }
        if (exportIntent.resolveActivity(packageManager) != null)
            startActivity(exportIntent)
        else
            Snackbar.make(binding.root, R.string.no_such_app_installed, Snackbar.LENGTH_LONG).show()
    }

    private suspend fun addHafezToDatabase(){
        poemDao.insertDatabase(
            category = tempCatToCat(tempDao.getAllCat()),
            poem = tempDao.getAllPoem(),
            poet = tempDao.getAllPoet(),
            verse = getAllVerseBiErab(tempDao.getAllVerse())
        )
        addViewModel.modifyAllPoet(2)
    }


//    fun setLocale() {
//        val locale = Locale("fa")
//        val config = Configuration(resources.configuration)
//        Locale.setDefault(locale)
//        config.setLocale(locale)
//        resources.updateConfiguration(config, resources.displayMetrics)
//    }

    fun addFragmentToContainer(frag: Fragment){
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(false)
            .replace(R.id.poem_container_view, frag)
            .commit()
    }

    fun removeFragmentFromContainer(){
        val presentFrag = getContainerFrag()
        if (presentFrag != null){
            supportFragmentManager.beginTransaction().remove(presentFrag).commit()
        }
    }

    fun getContainerFrag() = supportFragmentManager.findFragmentById(R.id.poem_container_view)

    fun statusBarColoring(){
        ValueAnimator.ofArgb(getColorFromAttr(R.attr.colorBeitSelect),
            if (settingViewModel.currentNightMode == Configuration.UI_MODE_NIGHT_NO)
                settingViewModel.paperColor
            else getColorFromAttr(R.attr.colorSurface)).apply {
            addUpdateListener { updatedAnimation ->
                window?.statusBarColor = updatedAnimation.animatedValue as Int
            }
            doOnEnd { window?.statusBarColor = Color.TRANSPARENT }
        }.start()
    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        when (newConfig.orientation){
//            Configuration.ORIENTATION_LANDSCAPE -> {
//
//            }
//        }
//    }

    @Deprecated("to be modified")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1){
            if (resultCode == Activity.RESULT_OK){
                poemViewModel.poemFileUri = data?.data
                poemViewModel.savePoemToFile()
            } else
                poemViewModel.notSavePoemToFile()
        }
    }

    fun hideSystemBars(){
        if (Build.VERSION.SDK_INT < 30)
            window?.decorView?.systemUiVisibility?.let {
                window?.decorView?.systemUiVisibility = it or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
            }
        else {
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    fun showSystemBars(){
        if (Build.VERSION.SDK_INT < 30)
            window?.decorView?.systemUiVisibility?.let {
                window?.decorView?.systemUiVisibility = it and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
            }
        else {
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    fun hideKeyBoard(){
        if (Build.VERSION.SDK_INT < 30)
            imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        else {
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.hide(WindowInsetsCompat.Type.ime())
        }
    }

    fun showKeyBoard(view: View?){
        if (Build.VERSION.SDK_INT < 30) {
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }else{
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.show(WindowInsetsCompat.Type.ime())
        }
    }

    fun analyticsLogEvent(name: String, params: Bundle){
        firebaseAnalytics?.logEvent(name, params)
    }

}





class RateDialogFragment : DialogFragment() {

    lateinit var preferenceManager: SharedPreferences

    override fun onAttach(context: Context) {
        super.onAttach(context)
        preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(resources.getString(R.string.rate_title))
                .setMessage(resources.getString(R.string.rate_message))
                .setPositiveButton(R.string.rate_now){ _: DialogInterface, _: Int ->
                    rateApp(it, false)
                    preferenceManager.edit().putBoolean("RatedAyeneJan", true).apply()
                }
                .setNegativeButton(R.string.rate_later){ _: DialogInterface, _: Int ->
                    preferenceManager.edit().putBoolean("RatedAyeneJan", true).apply()
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}











