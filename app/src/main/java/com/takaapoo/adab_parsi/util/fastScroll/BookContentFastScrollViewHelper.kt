package com.takaapoo.adab_parsi.util.fastScroll

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import com.takaapoo.adab_parsi.book.BookPagerFragment
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.fastscroll.Predicate


class BookContentFastScrollViewHelper(
    view: RecyclerView, popupTextProvider: PopupTextProvider?, private val bpf: BookPagerFragment
)
    : FastScroller.ViewHelper {


    private val mView: RecyclerView = view
    private var mPopupTextProvider: PopupTextProvider? = popupTextProvider
//    private val mTempRect: Rect = Rect()
    private val layoutManager = view.layoutManager as LinearLayoutManager


    var initialOffset = 0
//    var scroll = 0


    fun modifyScroll(){
        initialOffset = (scrollRange-mView.height).coerceAtLeast(0)
//        scroll = initialOffset
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
//                scroll += dy
//                bpf.bookContentHeight(false)
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
        return bpf.exactBookContentHeight()
    }

    override fun getScrollOffset(): Int {
        initialOffset = bpf.getScrollOffset()
        return initialOffset
    }

    override fun scrollTo(offset: Int) {
        // Stop any scroll in progress for RecyclerView.
        mView.stopScroll()
        when {
            offset < 1000 -> {
                layoutManager.scrollToPositionWithOffset(0, -offset)
//                scroll = offset
            }
            offset > scrollRange - mView.height - 1000 -> {
                layoutManager.scrollToPositionWithOffset(layoutManager.itemCount - 1,
                    scrollRange - offset - mView.children.last().height)
//                scroll = offset
            }
            else -> {
                val myOffset = offset - initialOffset
                mView.scrollBy(0, myOffset)
            }
        }
//        val myOffset = offset - initialOffset
//        mView.scrollBy(0, myOffset)
        initialOffset = offset
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

    private fun getFirstItemAdapterPosition(): Int {
        if (mView.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val itemView: View = mView.getChildAt(0)
        val linearLayoutManager =
            getVerticalLinearLayoutManager() ?: return RecyclerView.NO_POSITION
        return linearLayoutManager.getPosition(itemView)
    }

    private fun getVerticalLinearLayoutManager(): LinearLayoutManager? {
        val layoutManager = mView.layoutManager as? LinearLayoutManager ?: return null
        val linearLayoutManager = layoutManager
        return if (linearLayoutManager.orientation != RecyclerView.VERTICAL) {
            null
        } else linearLayoutManager
    }


}