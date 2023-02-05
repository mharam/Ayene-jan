package com.takaapoo.adab_parsi.poet

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.NavGraphDirections
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.book.BookViewModel
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.databinding.PagerPoetBinding
import com.takaapoo.adab_parsi.home.HomeViewModel
import com.takaapoo.adab_parsi.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.ceil
import kotlin.math.roundToInt

@AndroidEntryPoint
class PoetPagerFragment : Fragment() {

    val homeViewModel: HomeViewModel by activityViewModels()
    val poetViewModel: PoetViewModel by activityViewModels()
    private val bookViewModel: BookViewModel by activityViewModels()

    private var _binding: PagerPoetBinding? = null
    val binding get() = _binding!!
    private var adapter: BookListAdapter? = null
    private var bookLayoutManager: GridLayoutManager? = null
    private lateinit var poetItem: Category
    lateinit var navController: NavController

    private val shelfScrollListener = object : RecyclerView.OnScrollListener(){
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            binding.shelfList.scrollBy(0, dy)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        _binding = PagerPoetBinding.inflate(inflater, container, false)
        bookLayoutManager = binding.poetBookList.layoutManager as GridLayoutManager

        adapter = BookListAdapter(poetViewModel, bookViewModel)
        binding.poetBookList.adapter = adapter
        binding.poetBookList.edgeEffectFactory = ShelfEdgeEffectFactory(binding.shelfList)

        binding.poetBookList.addOnScrollListener(shelfScrollListener)

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId){
                R.id.search -> {
                    poetViewModel.reportEvent(PoetEvent.Navigate(
                        destination = Destinations.SEARCH,
                        directions = NavGraphDirections.actionGlobalSearchFragment(-1, poetItem.id)
                    ))
                }
                R.id.biography -> {
                    poetItem.poetID?.let {
                        poetViewModel.reportEvent(PoetEvent.OnBiographyClicked(it))
                    }
                }
            }
            true
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val actionBarSize = requireContext().getDimenFromAttr(R.attr.actionBarSize)
        binding.toolbar.updatePadding(top = topPadding)
        (binding.toolbar.layoutParams as ViewGroup.MarginLayoutParams).height = topPadding + actionBarSize

        val moldingDrawable = ResourcesCompat.getDrawable(resources, R.drawable.molding2, context?.theme)
        val moldingBottomDrawable = ResourcesCompat.getDrawable(resources, R.drawable.molding_bottom, context?.theme)
        if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES) {
            ResourcesCompat.getColor(resources, R.color.black_overlay, context?.theme).apply {
                moldingDrawable?.setTint(this)
                moldingBottomDrawable?.setTint(this)
            }
        }
        GlideApp.with(this).load(moldingDrawable).into(binding.molding)
        GlideApp.with(this).load(moldingBottomDrawable).into(binding.moldingBottom)
        GlideApp.with(this).load(R.drawable.wall1).into(binding.wall)

        navController = findNavController()
        binding.toolbar.setupWithNavController(navController, AppBarConfiguration
            .Builder(navController.graph).build())
        binding.toolbar.setNavigationOnClickListener { prepForGoingBack() }

        view.doOnPreDraw {
            val moldingHeight =
                ((topPadding + actionBarSize) / 0.89f + view.height * (DEFAULT_SCALE - 1) / 2).toInt()
            (binding.molding.layoutParams as ViewGroup.MarginLayoutParams).height = moldingHeight
            binding.poetCoordinate.updatePadding(
                top = (0.89f * moldingHeight - view.height * (DEFAULT_SCALE - 1) / 2).toInt()
            )
            poetViewModel.poetViewAspectRatio = view.width.toFloat() / view.height

            val pivotX = view.width.toFloat()
            val pivotY = view.height / 2f

            binding.apply {
                wall.pivotX = pivotX
                wall.pivotY = pivotY
                widgets.pivotX = pivotX
                widgets.pivotY = pivotY
                poetCoordinate.pivotX = pivotX
                poetCoordinate.pivotY = pivotY
                toolbar.pivotX = pivotX
                toolbar.pivotY = pivotY
                molding.pivotX = pivotX
                molding.pivotY = pivotY
                moldingBottom.pivotX = pivotX
                moldingBottom.pivotY = -pivotY + moldingBottom.height
            }

            if (homeViewModel.poetFirstOpening) {
                homeViewModel.poetFirstOpening = false
                binding.apply {
                    wall.scaleX = scaleXWall
                    wall.scaleY = DEFAULT_SCALE
                    molding.scaleX = DEFAULT_SCALE
                    molding.scaleY = DEFAULT_SCALE
                    moldingBottom.scaleX = DEFAULT_SCALE
                    moldingBottom.scaleY = DEFAULT_SCALE
                    poetCoordinate.postDelayed({ finalScale() }, 1000)
                }
            } else {
                finalScale()
            }

            bookLayoutManager!!.spanCount = (view.width / 178.dpTOpx(resources)).toInt()
            poetViewModel.bookShelfSpanCount = bookLayoutManager!!.spanCount


            arguments?.takeIf { it.containsKey(ARG_POET_POSITION) }?.apply {
                allCategory.filter { it.ancient == getInt(ARG_POET_ANCIENT) && it.parentID == 0 }.let { list ->
                    poetItem = list[getInt(ARG_POET_POSITION)]
                    binding.item = poetItem
                    binding.poetLayout.transitionName = poetItem.text

                    poetViewModel.getPoemWithCatID(poetItem.id).observe(viewLifecycleOwner) { items ->
                        val rowOrders = items.map { it.rowOrder }
                        val prependedItems = List(poetViewModel.bookShelfSpanCount) {
                            Content(-1, null, null, -1)
                        }.plus(items)

                        when {
                            rowOrders.let { it.contains(1) && it.contains(2) } -> {
                                val newItems =
                                    prependedItems.filterNot { it.rowOrder == 1 }.toMutableList()
                                newItems.add(Content(poetItem.id, 0, "سایر آثار", 1))
                                adapter!!.submitList(newItems)
                                binding.shelfList.adapter = ShelfAdapter(
                                    ceil(newItems.size.toFloat() / poetViewModel.bookShelfSpanCount).roundToInt(),
                                    poetItem
                                )
                            }
                            rowOrders.contains(1) -> {
                                adapter!!.submitList(
                                    List(poetViewModel.bookShelfSpanCount) {
                                        Content(-1, null, null, -1)
                                    }.plus(
                                        Content(poetItem.id, 0, "مجموعه آثار", 1)
                                    )
                                )
                                binding.shelfList.adapter = ShelfAdapter(2, poetItem)
                            }
                            else -> {
                                adapter!!.submitList(prependedItems)
                                binding.shelfList.adapter = ShelfAdapter(
                                    ceil(prependedItems.size.toFloat() / poetViewModel.bookShelfSpanCount).roundToInt(),
                                    poetItem
                                )
                            }
                        }
                    }
                }
            }

            scrollToPosition()
        }
        binding.toolbar.navigationContentDescription = resources.getString(R.string.navigation_up)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.poetBookList.removeOnScrollListener(shelfScrollListener)
        binding.toolbar.setOnMenuItemClickListener(null)
        binding.toolbar.setNavigationOnClickListener(null)

        bookLayoutManager = null
        adapter = null
        binding.poetBookList.adapter = null
        binding.shelfList.adapter = null
        _binding = null
    }

    fun firebaseLog(){
        if (::poetItem.isInitialized){
            (activity as? MainActivity)?.analyticsLogEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                Bundle().apply {
                    putString(
                        FirebaseAnalytics.Param.SCREEN_NAME,
                        "Poet: ${poetItem.text?.substringBefore('*')}"
                    )
                }
            )
            Firebase.crashlytics.setCustomKey(
                "Enter Screen",
                "Poet: ${poetItem.text?.substringBefore('*')}"
            )
        }
    }


    private fun scrollToPosition() {
        binding.poetBookList.doOnLayout {

            val shelfLayoutManager = binding.shelfList.layoutManager as LinearLayoutManager
//            val prevPosition = max(bookLayoutManager.findFirstCompletelyVisibleItemPosition(), 0)
//            val displacement = floor((poetViewModel.bookPosition - prevPosition)/2f)

            shelfLayoutManager.stackFromEnd = false
            bookLayoutManager?.stackFromEnd = false

            binding.poetBookList.postDelayed( {
//                binding.appBar.setExpanded(false)
//                bookLayoutManager.scrollToPosition(poetViewModel.bookPosition)
//                shelfLayoutManager.scrollToPosition(poetViewModel.bookPosition/2)



                bookLayoutManager?.scrollToPositionWithOffset(poetViewModel.bookPosition, 0)
                shelfLayoutManager.scrollToPositionWithOffset(
                    poetViewModel.bookPosition/poetViewModel.bookShelfSpanCount, 0.dpTOpx(resources).toInt())
//                binding.poetBookList.scrollBy(0, (displacement*240.dpTOpx(resources)).toInt())


                parentFragment?.startPostponedEnterTransition()
            }, 50)
        }
    }

//    fun setTransitionType(excView: View){
//        requireParentFragment().apply {
//            exitTransition =
//                Hold().setDuration(500)/*.addTarget(R.id.poet_coordinate)*/.excludeTarget(excView, true)
//            reenterTransition = null
//        }
//    }

    fun backCallback(){
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            prepForGoingBack()
        }
    }

    private fun prepForGoingBack() {
        binding.apply {
            wall.scaleX = scaleXWall
            wall.scaleY = DEFAULT_SCALE
            molding.scaleX = DEFAULT_SCALE
            molding.scaleY = DEFAULT_SCALE
            moldingBottom.scaleX = DEFAULT_SCALE
            moldingBottom.scaleY = DEFAULT_SCALE
            widgets.scaleX = 1f
            widgets.scaleY = 1f
            poetCoordinate.scaleX = 1f
            poetCoordinate.scaleY = 1f
            toolbar.scaleX = 1f
            toolbar.scaleY = 1f
        }
        Handler(Looper.getMainLooper()).postDelayed({ navController.popBackStack() }, 50)
    }

    private fun finalScale(){
        val scale1 = 1 / DEFAULT_SCALE
        binding.apply {
            wall.scaleX = scaleXWall
            wall.scaleY = DEFAULT_SCALE
            molding.scaleX = 1f
            molding.scaleY = 1f
            moldingBottom.scaleX = 1f
            moldingBottom.scaleY = 1f
            widgets.scaleX = DEFAULT_SCALE
            widgets.scaleY = DEFAULT_SCALE
            poetCoordinate.scaleX = 1f
            poetCoordinate.scaleY = 1f
            toolbar.scaleX = scale1
            toolbar.scaleY = scale1
        }
    }

    fun searchIconCenter(): IntArray{
        val location = IntArray(2){0}
        val fragLocation = IntArray(2){0}
        val searchView = binding.toolbar.findViewById<View>(R.id.search)

        searchView.getLocationInWindow(location)
        binding.root.getLocationInWindow(fragLocation)

        location[0] = location[0] - fragLocation[0] + searchView.width/2
        location[1] = location[1] - fragLocation[1] + searchView.height/2

        return location
    }

}