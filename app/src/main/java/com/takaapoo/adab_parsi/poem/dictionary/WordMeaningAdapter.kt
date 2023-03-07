package com.takaapoo.adab_parsi.poem.dictionary

import android.animation.ObjectAnimator
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.PoemMeaningItemBinding
import com.takaapoo.adab_parsi.network.DictionaryProperty
import com.takaapoo.adab_parsi.util.engNumToFarsiNum
import kotlin.math.abs

class WordMeaningAdapter : ListAdapter<DictionaryProperty, WordMeaningAdapter.ViewHolder>(
    DictionaryDiffCallback()
) {

    override fun getItemViewType(position: Int) = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(val binding: PoemMeaningItemBinding) : RecyclerView.ViewHolder(binding.root){

        private var meanHeight = 0
        private val smoothScroller = object : LinearSmoothScroller(binding.root.context) {
            override fun getVerticalSnapPreference() = SNAP_TO_START

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return 300/(abs(itemView.y) * (displayMetrics?.density ?: 1f))
            }
        }

        fun bind(item: DictionaryProperty) {
            binding.word.text = binding.word.context.resources
                .getString(R.string.numbered_title, engNumToFarsiNum(layoutPosition+1), item.word)
            binding.wordMean.apply {
                text = item.mean
                    .replace("<br>", "\n")
                    .replace("<BR>", "\n")
                    .removeSuffix("\n")

                doOnPreDraw {
                    meanHeight = measuredHeight
                    height = meanHeight.coerceAtMost(context.resources.getDimension(R.dimen.word_meaning_max_height).toInt())
                }
            }

            itemView.setOnClickListener {
                binding.wordMean.apply {
                    val maximumHeight = context.resources.getDimension(R.dimen.word_meaning_max_height).toInt()
                    if (meanHeight > maximumHeight)
                        if (height == maximumHeight)
                            ObjectAnimator.ofInt(this, "height", meanHeight)?.let {
                                it.doOnEnd {
                                    invalidate()
                                }
                                it.start()
                            }
                        else {
                            ObjectAnimator.ofInt(this, "height", maximumHeight)?.let {
                                it.doOnEnd {
                                    invalidate()
                                }
                                it.start()
                            }

                            if (itemView.y < 0){
                                smoothScroller.targetPosition = layoutPosition
                                (binding.root.parent as RecyclerView).layoutManager?.startSmoothScroll(smoothScroller)
                            }
                        }
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = PoemMeaningItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }


}

class DictionaryDiffCallback : DiffUtil.ItemCallback<DictionaryProperty>(){
    override fun areItemsTheSame(oldItem: DictionaryProperty, newItem: DictionaryProperty): Boolean {
        return oldItem.word == newItem.word
    }

    override fun areContentsTheSame(oldItem: DictionaryProperty, newItem: DictionaryProperty): Boolean {
        return oldItem == newItem
    }
}