package com.takaapoo.adab_parsi.book

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.databinding.ContentItemBinding
import com.takaapoo.adab_parsi.poet.PoetViewModel
import com.takaapoo.adab_parsi.util.allSubCategories
import com.takaapoo.adab_parsi.util.allUpCategories
import com.takaapoo.adab_parsi.util.engNumToFarsiNum
import kotlin.collections.set


class BookContentAdapter(private val bookViewModel: BookViewModel,
                         private val poetViewModel: PoetViewModel,
                         private val bookItemId: Int
                         ) : ListAdapter<Content, BookContentAdapter.ViewHolder>(ContentDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ContentItemBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding, bookViewModel, poetViewModel, bookItemId)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.setOnClickListener(null)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.setupOnClickListener(getItem(holder.bindingAdapterPosition))
    }

    inner class ViewHolder(val binding: ContentItemBinding, 
                           private val bookViewModel: BookViewModel,
                           private val poetViewModel: PoetViewModel,
                           private val bookItemId: Int
    ) : RecyclerView.ViewHolder(binding.root){

        private lateinit var newList: MutableList<Content>

        fun bind(item: Content) {
            if (bookViewModel.listOpen[item.id] == null && item.rowOrder == 2)
                bookViewModel.listOpen[item.id] = false

            binding.apply {
                content = item
                (myContent.layoutParams as ViewGroup.MarginLayoutParams).marginStart =
                    item.rank * (itemView.resources).getDimension(R.dimen.book_content_margin).toInt()

                triImage.rotation = if (bookViewModel.listOpen[item.id] == true) -90f else 0f
                triImage.visibility = if (item.rowOrder == 2 &&
                    poetViewModel.poemListSubItems[item.id]?.isNotEmpty() == true)
                        View.VISIBLE else View.GONE
                var number = poetViewModel.poemListItems[bookItemId]?.indexOfFirst { it.id == item.id }
                if (number == null || number == -1)
                    number = poetViewModel.poemListItems[bookItemId]
                        ?.indexOfFirst { allSubCategories(item.id).contains(it.parentID) } ?: 0
                pageText.text = engNumToFarsiNum(number + 1)
            }
        }

        fun setupOnClickListener(item: Content){
            itemView.setOnClickListener {
                if (binding.triImage.isVisible) {
                    newList = currentList.toMutableList()
                    if (bookViewModel.listOpen[item.id]!!){
                        newList.filter { elem -> elem.rowOrder == 2 && elem.rank > item.rank &&
                                allUpCategories(elem.id).contains(item.id)}.forEach {
                            bookViewModel.listOpen[it.id] = false
                        }
                        newList.removeAll { elem -> elem.rank > item.rank &&
                                allUpCategories(elem.parentID).contains(item.id) }
                        binding.triImage.animate().setStartDelay(50).rotation(0f).start()
                    } else {
                        newList.addAll(currentList.indexOfFirst {it.id == item.id} + 1
                            , poetViewModel.poemListSubItems[item.id]!!)
                        binding.triImage.animate().setStartDelay(50).rotation(-90f).start()
                    }
                    submitList(newList)
                    bookViewModel.listOpen[item.id] = !bookViewModel.listOpen[item.id]!!
                    bookViewModel.reportEvent(BookEvent.OpenCloseContent(newList))
                } else
                    bookViewModel.reportEvent(BookEvent.NavigateToPoem(itemId = item.id))
            }
        }
    }

}

class ContentDiffCallback : DiffUtil.ItemCallback<Content>(){
    override fun areItemsTheSame(oldItem: Content, newItem: Content): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Content, newItem: Content): Boolean {
        return oldItem == newItem
    }
}