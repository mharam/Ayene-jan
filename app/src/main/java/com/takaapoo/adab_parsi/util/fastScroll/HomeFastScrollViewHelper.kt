package com.takaapoo.adab_parsi.util.fastScroll

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.fastscroll.Predicate


internal class HomeFastScrollViewHelper(private val mView: RecyclerView,
    private val mPopupTextProvider: PopupTextProvider?) : FastScroller.ViewHelper {

    private val mTempRect = Rect()


    override fun addOnPreDrawListener(onPreDraw: Runnable) {
        mView.addItemDecoration(object : ItemDecoration() {
            override fun onDraw(
                canvas: Canvas, parent: RecyclerView,
                state: RecyclerView.State
            ) {
                onPreDraw.run()
            }
        })
    }

    override fun addOnScrollChangedListener(onScrollChanged: Runnable) {
        mView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onScrollChanged.run()
            }
        })
    }

    override fun addOnTouchEventListener(onTouchEvent: Predicate<MotionEvent?>) {
        mView.addOnItemTouchListener(object : SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, event: MotionEvent): Boolean {
                return onTouchEvent.test(event)
            }

            override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
                onTouchEvent.test(event)
            }
        })
    }

    override fun getScrollRange(): Int {
        val itemCount = itemCount
        if (itemCount == 0) {
            return 0
        }
        val itemHeight = itemHeight
        return if (itemHeight == 0) {
            0
        } else mView.paddingTop + itemCount * itemHeight + mView.paddingBottom
    }

    override fun getScrollOffset(): Int {
        val firstItemPosition = firstItemPosition
        if (firstItemPosition == RecyclerView.NO_POSITION) {
            return 0
        }
        val itemHeight = itemHeight
        val firstItemTop = firstItemOffset
        return mView.paddingTop + firstItemPosition * itemHeight - firstItemTop
    }

    override fun scrollTo(offset: Int) {
        // Stop any scroll in progress for RecyclerView.
        var scrollOffset = offset
        mView.stopScroll()
        scrollOffset -= mView.paddingTop
        val itemHeight = itemHeight
        // firstItemPosition should be non-negative even if paddingTop is greater than item height.
        val firstItemPosition = 0.coerceAtLeast(scrollOffset / itemHeight)
        val firstItemTop = firstItemPosition * itemHeight - scrollOffset
        scrollToPositionWithOffset(firstItemPosition, firstItemTop)
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
        val position = firstItemAdapterPosition
        return if (position == RecyclerView.NO_POSITION) {
            null
        } else popupTextProvider.getPopupText(position)
    }

    private val itemCount: Int
        get() {
            val linearLayoutManager = verticalLinearLayoutManager ?: return 0
            var itemCount = linearLayoutManager.itemCount
            if (itemCount == 0) {
                return 0
            }
            if (linearLayoutManager is GridLayoutManager) {
                itemCount = (itemCount - 1) / linearLayoutManager.spanCount + 1
            }
            return itemCount
        }

    private val itemHeight: Int
        get() {
            if (mView.childCount == 0) {
                return 0
            }
            val itemView = mView.getChildAt(0)
            mView.getDecoratedBoundsWithMargins(itemView, mTempRect)
            return mTempRect.height()
        }

    private val firstItemPosition: Int
        get() {
            var position = firstItemAdapterPosition
            val linearLayoutManager = verticalLinearLayoutManager ?: return RecyclerView.NO_POSITION
            if (linearLayoutManager is GridLayoutManager) {
                position /= linearLayoutManager.spanCount
            }
            return position
        }

    private val firstItemAdapterPosition: Int
        get() {
            if (mView.childCount == 0) {
                return RecyclerView.NO_POSITION
            }
            val itemView = mView.getChildAt(0)
            val linearLayoutManager = verticalLinearLayoutManager ?: return RecyclerView.NO_POSITION
            return linearLayoutManager.getPosition(itemView)
        }

    private val firstItemOffset: Int
        get() {
            if (mView.childCount == 0) {
                return RecyclerView.NO_POSITION
            }
            val itemView = mView.getChildAt(0)
            mView.getDecoratedBoundsWithMargins(itemView, mTempRect)
            return mTempRect.top
        }

    private fun scrollToPositionWithOffset(position: Int, offset: Int) {
        var scrollPosition = position
        var scrollOffset = offset
        val linearLayoutManager = verticalLinearLayoutManager ?: return
        if (linearLayoutManager is GridLayoutManager) {
            scrollPosition *= linearLayoutManager.spanCount
        }
        // LinearLayoutManager actually takes offset from paddingTop instead of top of RecyclerView.
        scrollOffset -= mView.paddingTop
        linearLayoutManager.scrollToPositionWithOffset(scrollPosition, scrollOffset)
    }

    private val verticalLinearLayoutManager: LinearLayoutManager?
        get() {
            val layoutManager = mView.layoutManager as? LinearLayoutManager ?: return null
            val linearLayoutManager = layoutManager
            return if (linearLayoutManager.orientation != RecyclerView.VERTICAL) {
                null
            } else linearLayoutManager
        }

}