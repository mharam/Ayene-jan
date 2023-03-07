package com.takaapoo.adab_parsi.poem.touch_handler

import android.graphics.Rect
import android.view.MotionEvent
import android.widget.Button
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.poem.PoemViewModel

class OnRecyclerViewItemTouchListener(val poemViewModel: PoemViewModel) : RecyclerView.OnItemTouchListener {

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val itemView = rv.findChildViewUnder(e.x, e.y) ?: return false
        val mComment = itemView.findViewById<NestedScrollView>(R.id.comment)
        val saveButton = itemView.findViewById<Button>(R.id.save)

        poemViewModel.touchedViewID = rv.getChildItemId(itemView).toInt()
        val commentRect = Rect(mComment.left, mComment.top, mComment.right, mComment.bottom)
        val margin = 10
        val saveRect = Rect(saveButton.left - margin, saveButton.top - margin,
            saveButton.right + margin, saveButton.bottom + margin)

        val xPos = (e.x - itemView.x).toInt()
        val yPos = (e.y - itemView.y).toInt()

        return if (commentRect.contains(xPos, yPos) || saveRect.contains(xPos, yPos))
            false
        else
            poemViewModel.gestureDetector!!.onTouchEvent(e)
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

}