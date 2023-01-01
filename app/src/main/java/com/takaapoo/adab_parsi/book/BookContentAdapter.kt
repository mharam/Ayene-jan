package com.takaapoo.adab_parsi.book

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.databinding.ContentItemBinding
import com.takaapoo.adab_parsi.poem.PoemFragment
import com.takaapoo.adab_parsi.util.allSubCategories
import com.takaapoo.adab_parsi.util.allUpCategories
import com.takaapoo.adab_parsi.util.engNumToFarsiNum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.set


class BookContentAdapter(private val bookPagerFragment: BookPagerFragment)
    : ListAdapter<Content, BookContentAdapter.ViewHolder>(ContentDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ContentItemBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding, bookPagerFragment)
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

    inner class ViewHolder(val binding: ContentItemBinding, private val bPF: BookPagerFragment)
        : RecyclerView.ViewHolder(binding.root){

        private val mainActivity = bPF.activity as MainActivity
        private lateinit var newList: MutableList<Content>

        fun bind(item: Content) {
            if (bPF.bookViewModel.listOpen[item.id] == null && item.rowOrder == 2)
                bPF.bookViewModel.listOpen[item.id] = false

            binding.apply {
                content = item
                (myContent.layoutParams as ViewGroup.MarginLayoutParams).marginStart =
                    item.rank * (itemView.resources).getDimension(R.dimen.book_content_margin).toInt()

                triImage.rotation = if (bPF.bookViewModel.listOpen[item.id] == true) -90f else 0f
                triImage.visibility = if (item.rowOrder == 2 &&
                    bPF.poetViewModel.poemListSubItems[item.id]?.isNotEmpty() == true)
                        View.VISIBLE else View.GONE
                var number = bPF.poetViewModel.poemListItems[bPF.contentItem.id]?.indexOfFirst { it.id == item.id }
                if (number == null || number == -1)
                    number = bPF.poetViewModel.poemListItems[bPF.contentItem.id]
                        ?.indexOfFirst { allSubCategories(item.id).contains(it.parentID) } ?: 0
                pageText.text = engNumToFarsiNum(number + 1)
            }
        }

        fun setupOnClickListener(item: Content){
            itemView.setOnClickListener { view ->
                if (binding.triImage.isVisible) {
                    newList = currentList.toMutableList()
                    if (bPF.bookViewModel.listOpen[item.id]!!){
                        newList.filter { elem -> elem.rowOrder == 2 && elem.rank > item.rank &&
                                allUpCategories(elem.id).contains(item.id)}.forEach {
                            bPF.bookViewModel.listOpen[it.id] = false
                        }
                        newList.removeAll { elem -> elem.rank > item.rank &&
                                allUpCategories(elem.parentID).contains(item.id) }
                        binding.triImage.animate().setStartDelay(50).rotation(0f).start()
                    } else {
                        newList.addAll(currentList.indexOfFirst {it.id == item.id} + 1
                            , bPF.poetViewModel.poemListSubItems[item.id]!!)
                        binding.triImage.animate().setStartDelay(50).rotation(-90f).start()
                    }
                    submitList(newList)
                    bPF.bookViewModel.listOpen[item.id] = !bPF.bookViewModel.listOpen[item.id]!!

                    bPF.binding.bookContentList.doOnNextLayout {
                        Handler(Looper.getMainLooper()).postDelayed({
                            endAction(newList)
                        }, 500)
                    }

                } else {
                    bPF.poemViewModel.apply {
                        poemPosition = bPF.poetViewModel
                            .poemListItems[bPF.contentItem.id]!!.indexOfFirst {it.id == item.id }
                        poemList = bPF.poetViewModel.poemListItems[bPF.contentItem.id]!!
                        poemCount = poemList.size
//                        bPFcontentItem = bPF.contentItem

//                        bPF.binding.bookContent.doOnLayout { contentShot = bPF.binding.bookContent.drawToBitmap() }
                        contentShot = Bitmap.createBitmap(bPF.binding.bookContent.width
                            , bPF.binding.bookContent.height, Bitmap.Config.ARGB_8888).also {
                            bPF.binding.bookContent.draw(Canvas(it))
                        }
                        poemFirstOpening = true
                    }
//                    bPF.bookViewModel.offset = itemView.top
//                    bPF.bookViewModel.bookContentScrollPosition[bPF.contentItem.id] = item

                    if (mainActivity.binding.drawerLayout.tag == "land_scape") {
//                        if (mainActivity.getContainerFrag() is PoemFragment)
//                            (mainActivity.getContainerFrag() as PoemFragment).buildViewPager()
//                        else
                        mainActivity.addFragmentToContainer(PoemFragment())
                    } else {
                        try {
                            bPF.setTransitionType()
                            val action = BookFragmentDirections.actionBookFragmentToPoemFragment()
                            view.findNavController().navigate(action)
                        } catch (e: Exception) { }
                    }
                }
            }

        }

        private fun endAction(newList: MutableList<Content>){
            CoroutineScope(Dispatchers.Default).launch {
                bPF.modifyAggregatedHeight(newList)
                if((bPF.binding.bookContentList.layoutManager as LinearLayoutManager)
                        .findLastCompletelyVisibleItemPosition() == newList.size-1)
                    bPF.scrollViewHelper?.modifyScroll()

                withContext(Dispatchers.Main) { bPF.binding.bookContentList.scrollBy(0, 0) }
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