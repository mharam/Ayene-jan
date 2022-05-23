package com.takaapoo.adab_parsi.util.fastScroll

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import com.takaapoo.adab_parsi.poem.PoemPagerFragment
import com.takaapoo.adab_parsi.poem.PoemViewModel
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.fastscroll.Predicate


class PoemFastScrollViewHelper(view: RecyclerView, popupTextProvider: PopupTextProvider?
                               , private val poemViewModel: PoemViewModel, private val ppf : PoemPagerFragment)
    : FastScroller.ViewHelper {


    private val mView: RecyclerView = view
    private var mPopupTextProvider: PopupTextProvider? = popupTextProvider
    private val mTempRect: Rect = Rect()

    var initialOffset = 0
    var scroll = 0


    fun modifyScroll(){
        initialOffset = (scrollRange-mView.height).coerceAtLeast(0)
        scroll = initialOffset
    }

    override fun addOnPreDrawListener(onPreDraw: Runnable) {
        mView.addItemDecoration(object : ItemDecoration() {
            override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                onPreDraw.run()
            }
        })
    }

    override fun addOnScrollChangedListener(onScrollChanged: Runnable) {
        mView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onScrollChanged.run()
                scroll += dy
                ppf.largePoemHeight(ppf.poemItem.id, scroll)
            }
        })
    }

    override fun addOnTouchEventListener(onTouchEvent: Predicate<MotionEvent?>) {
        mView.addOnItemTouchListener(object : SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, event: MotionEvent)
                    : Boolean {
                return onTouchEvent.test(event)
            }

            override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
                onTouchEvent.test(event)
            }
        })
    }

    override fun getScrollRange(): Int {
        val itemCount = getItemCount()
        if (itemCount == 0)
            return 0

        return /*mView.paddingTop +*/ mView.paddingBottom +
                (poemViewModel.poemContentHeight[ppf.poemItem.id] ?: 0)
    }

    override fun getScrollOffset(): Int {

//        val firstItemPosition = getFirstItemAdapterPosition()
//        if (firstItemPosition == RecyclerView.NO_POSITION) return 0
//        when{
//            firstItemPosition > firstItem -> {
//                val itemsAdded = ppf.itemViewHeights.sliceArray(firstItem until firstItemPosition)
//                itemsHeightAdded +=
//                    itemsAdded.sum()
//                firstItem = firstItemPosition
//            }
//            firstItemPosition < firstItem -> {
//                val itemsAdded = ppf.itemViewHeights.sliceArray(firstItemPosition until firstItem)
//                itemsHeightAdded -=
//                    itemsAdded.sum()
//                firstItem = firstItemPosition
//            }
//        }
//
//        initialOffset = itemsHeightAdded - getFirstItemOffset()

        initialOffset = scroll
        return initialOffset
    }

    override fun scrollTo(offset: Int) {
        // Stop any scroll in progress for RecyclerView.
        mView.stopScroll()

        val myOffset = offset - initialOffset
        mView.scrollBy(0, myOffset)
    }

    override fun getPopupText(): String? {
        var popupTextProvider = mPopupTextProvider
        if (popupTextProvider == null) {
            val adapter = mView.adapter
            if (adapter is PopupTextProvider) {
                popupTextProvider = adapter
            }
        }
        if (popupTextProvider == null) {
            return null
        }
        val position = getFirstItemAdapterPosition()
        return if (position == RecyclerView.NO_POSITION) null
        else popupTextProvider.getPopupText(position)
    }

    private fun getItemCount(): Int {
        val linearLayoutManager = getVerticalLinearLayoutManager() ?: return 0
        var itemCount = linearLayoutManager.itemCount
        if (itemCount == 0) {
            return 0
        }
        if (linearLayoutManager is GridLayoutManager) {
            itemCount = (itemCount - 1) / linearLayoutManager.spanCount + 1
        }
        return itemCount
    }

    private fun getItemHeight(): Int {
//        if (mView.childCount == 0) {
//            return 0
//        }
        val itemView: View = mView.getChildAt(0)
        mView.getDecoratedBoundsWithMargins(itemView, mTempRect)
        return mTempRect.height()
    }

    private fun getFirstItemPosition(): Int {
        var position = getFirstItemAdapterPosition()
        val linearLayoutManager =
            getVerticalLinearLayoutManager() ?: return RecyclerView.NO_POSITION
        if (linearLayoutManager is GridLayoutManager) {
            position /= linearLayoutManager.spanCount
        }
        return position
    }

    private fun getFirstItemAdapterPosition(): Int {
        if (mView.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val itemView: View = mView.getChildAt(0)
        val linearLayoutManager =
            getVerticalLinearLayoutManager() ?: return RecyclerView.NO_POSITION
        return linearLayoutManager.getPosition(itemView) ?: 0
    }

    private fun getFirstItemOffset(): Int {
        if (mView.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val itemView: View = mView.getChildAt(0)
        mView.getDecoratedBoundsWithMargins(itemView, mTempRect)
        return mTempRect.top
    }

    private fun scrollToPositionWithOffset(position: Int, offset: Int) {
        var position = position
        var offset = offset
        val linearLayoutManager = getVerticalLinearLayoutManager() ?: return
        if (linearLayoutManager is GridLayoutManager) {
            position *= linearLayoutManager.spanCount
        }
        // LinearLayoutManager actually takes offset from paddingTop instead of top of RecyclerView.
        offset -= mView.paddingTop
        linearLayoutManager.scrollToPositionWithOffset(position, offset)
    }

    private fun getVerticalLinearLayoutManager(): LinearLayoutManager? {
        val layoutManager = mView.layoutManager as? LinearLayoutManager ?: return null
        val linearLayoutManager = layoutManager
        return if (linearLayoutManager.orientation != RecyclerView.VERTICAL) {
            null
        } else linearLayoutManager
    }


}