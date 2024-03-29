package com.takaapoo.adab_parsi.book

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.app.SharedElementCallback
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.NavGraphDirections
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.FragmentBookBinding
import com.takaapoo.adab_parsi.poem.PoemFragment
import com.takaapoo.adab_parsi.poet.PoetBounceEdgeEffectFactory
import com.takaapoo.adab_parsi.poet.PoetViewModel
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.GlideApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow


@AndroidEntryPoint
class BookFragment : Fragment() {

    private val poetViewModel: PoetViewModel by activityViewModels()
    val bookViewModel: BookViewModel by activityViewModels()
    private val settingViewModel: SettingViewModel by activityViewModels()

    private var _binding: FragmentBookBinding? = null
    val binding get() = _binding!!
    var currentChildFragment: BookPagerFragment? = null

    private val pageCallBack = object : ViewPager2.OnPageChangeCallback(){
        override fun onPageSelected(position: Int) {
            poetViewModel.bookPosition = position
            bookViewModel.bookCurrentItem = poetViewModel.bookListItems[position]!!

            currentChildFragment = childFragmentManager.findFragmentByTag(
                "f${poetViewModel.bookPosition}"
            ) as? BookPagerFragment
            currentChildFragment?.run {
                backCallback()
                firebaseLog()
            }
        }
    }

    private val scaleXOutValue = TypedValue()
    private val scaleYOutValue = TypedValue()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        motionInitialization()
        _binding = FragmentBookBinding.inflate(inflater, container, false)

        if (bookViewModel.bookFirstOpening)
            binding.bookViewPager.background = poetViewModel.poetLibContentShot?.toDrawable(resources)
        else
            changeBackgroundImage()

        poetViewModel.enterBookFragment = true
        binding.root.apply {
            post {
                pivotX = measuredWidth.toFloat()
                pivotY = measuredHeight/2f
                cameraDistance = 30f * measuredWidth
            }
        }

        settingViewModel.currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        resources.getValue(R.integer.open_book_init_scale_x, scaleXOutValue, true)
        resources.getValue(R.integer.open_book_init_scale_y, scaleYOutValue, true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bookAdapter = BookAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,
            poetViewModel.count)

        val help = Help(this)
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val firstFragEntrance = preferenceManager.getBoolean("bookFragFirstEnter", true)
        if (firstFragEntrance) {
            bookViewModel.reportEvent(BookEvent.OnShowHelp(BookHelpState.PAGING))
            preferenceManager.edit().putBoolean("bookFragFirstEnter", false).apply()
        }

        currentChildFragment = childFragmentManager.findFragmentByTag(
            "f${poetViewModel.bookPosition}"
        ) as? BookPagerFragment

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                bookViewModel.uiEvent.collect{ event ->
                    when(event){
                        is BookEvent.OpenCloseContent -> {
                            currentChildFragment?.endAction(event.newList)
                        }
                        is BookEvent.NavigateToPoem -> {
                            currentChildFragment?.prepareForNavigationToPoem(event.itemId)
                            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                                (activity as? MainActivity)?.addFragmentToContainer(PoemFragment())
                            else {
                                try {
                                    exitTransition = null
                                    reenterTransition = null
                                    findNavController().navigate(
                                        directions = BookFragmentDirections.actionBookFragmentToPoemFragment()
                                    )
                                } catch (_: Exception) { }
                            }
                        }
                        is BookEvent.OnShowHelp -> {
                            help.showHelp(event.state, if (firstFragEntrance) 3000 else 500)
                        }
                        is BookEvent.NavigateToSearch -> {
                            try {
                                exitTransition = MaterialFadeThrough()
                                findNavController().navigate(
                                    directions = NavGraphDirections.actionGlobalSearchFragment(
                                        -2,
                                        event.bookItemId
                                    )
                                )
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }

        binding.bookViewPager.apply {
            adapter = bookAdapter
            (getChildAt(0) as RecyclerView).edgeEffectFactory =
                PoetBounceEdgeEffectFactory(::transformPage)
            setCurrentItem(poetViewModel.bookPosition, false)
            setPageTransformer(ZoomOutPageTransformer(::transformPage))
            registerOnPageChangeCallback(pageCallBack)
            post { pageCallBack.onPageSelected(binding.bookViewPager.currentItem) }
        }

        // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: List<String>, sharedElements: MutableMap<String, View> ) {
                currentChildFragment?.view ?: return
                try {
                    sharedElements[names[0]] = currentChildFragment?.binding?.bookLayout!!
                } catch (_: Exception) { }
            }
        })

        settingViewModel.refreshContent.observe(viewLifecycleOwner){
            if (it == true){
                childFragmentManager.fragments.forEach { fragment ->
                    if (fragment is BookPagerFragment)
                        fragment.refreshContent()
                }
                settingViewModel.doneRefreshing()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (activity?.isChangingConfigurations == false)
            bookViewModel.reportEvent(BookEvent.OnShowHelp(BookHelpState.NULL))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.bookViewPager.unregisterOnPageChangeCallback(pageCallBack)
        binding.bookViewPager.adapter = null

        currentChildFragment = null

        _binding = null
        poetViewModel.poemListItems.clear()
        poetViewModel.poemListSubItems.clear()

        GlideApp.get(requireContext()).trimMemory(Glide.TRIM_MEMORY_COMPLETE)
    }

    private fun motionInitialization(){
        postponeEnterTransition()
        val enterMatConTrans = MaterialContainerTransform()
        val returnMatConTrans = MaterialContainerTransform()

        enterMatConTrans.apply {
            duration = 500
            scrimColor = Color.TRANSPARENT
            fitMode = MaterialContainerTransform.FIT_MODE_HEIGHT
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 0.55f)
        }

        returnMatConTrans.apply {
            duration = 500
            scrimColor = Color.TRANSPARENT
            fitMode = MaterialContainerTransform.FIT_MODE_HEIGHT
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.05f, 1f)
        }

        sharedElementEnterTransition = enterMatConTrans
        sharedElementReturnTransition = returnMatConTrans
    }

    fun changeBackgroundImage(){
        GlideApp.with(this).load(R.drawable.background_wood).into(
            object : CustomTarget<Drawable?>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable?>?) {
                    binding.bookViewPager.background = resource
                }
                override fun onLoadCleared(placeholder: Drawable?) { }
            }
        )
        poetViewModel.poetLibContentShot = null

        //        binding.bookViewPager.background =
//            ResourcesCompat.getDrawable(resources, R.drawable.background_wood, context?.theme)
    }

    private fun transformPage(view: View, position: Float) {
        val bookLayout = view.findViewById<FrameLayout>(R.id.book_layout) ?: return
        val bookCover = view.findViewById<FrameLayout>(R.id.book_cover) ?: return

        view.apply {
            when {
                position == 0f && bookViewModel.bookFirstOpening -> {
                    bookCover.rotationY = 0f
                }
                position <= -1 -> { // [-Infinity,-1]
                    translationX = 0f
                }
                position < 1 -> { // (-1,1)
                    val scaleFactor = MIN_SCALE.coerceAtLeast(1 - abs(position))
                    val vertMargin = height * (1 - scaleFactor) / 2
                    val horizMargin = width * (1 - scaleFactor) / 2
                    val transX = if (position < 0) horizMargin + vertMargin
                    else horizMargin - vertMargin / 2

                    when {
                        position < 0 && position > MIN_SCALE - 1 -> x = 0f
                        position <= MIN_SCALE - 1 -> x =
                            (-0.1f / MIN_SCALE) * (1 + position) * width - position * width
                        position > MIN_SCALE -> x = -1f * width
                        position <= MIN_SCALE -> x = (-0.1f / MIN_SCALE - 1) * position * width
                    }

                    bookLayout.run {
                        translationX = transX
                        scaleX = scaleFactor / scaleXOutValue.float
                        scaleY = scaleFactor / scaleYOutValue.float
                    }
                    bookCover.rotationY = (1 - abs(position)).pow(4) * 90f
                }
                else -> { // [1,+Infinity]
                    translationX = 0f
                    bookLayout.run {
                        scaleX = 1 / scaleXOutValue.float
                        scaleY = 1 / scaleYOutValue.float
                    }
                }
            }
        }
    }
}


const val ARG_BOOK_POSITION = "book_position"
class BookAdapter(fragManager: FragmentManager,
                  lifeCycle: Lifecycle,
                  private val itemNumber: Int
                  ) : FragmentStateAdapter(fragManager, lifeCycle) {

    override fun getItemCount(): Int = itemNumber

    override fun createFragment(position: Int): Fragment {
        val fragment = BookPagerFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_BOOK_POSITION, position)
        }
        return fragment
    }
}


private const val MIN_SCALE = 0.90f
class ZoomOutPageTransformer(val transform: (view: View, position: Float) -> Unit) : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) = transform(view, position)
}