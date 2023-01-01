package com.takaapoo.adab_parsi.home

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.databinding.PoetItemAncientBinding
import com.takaapoo.adab_parsi.databinding.PoetItemRecentBinding
import com.takaapoo.adab_parsi.util.Destinations
import com.takaapoo.adab_parsi.util.GlideApp

class PoetListAdapter(
    private val homeViewModel: HomeViewModel,
    private val ancient: Int
    ): ListAdapter<Category, RecyclerView.ViewHolder>(CategoryDiffCallback()) {

    private var tracker: SelectionTracker<Long>? = null
    init {
        setHasStableIds(true)
    }

    fun setTracker(tracker: SelectionTracker<Long>?) {
        this.tracker = tracker
    }

    override fun getItemViewType(position: Int) = ancient
    override fun getItemId(position: Int) = getItem(position).poetID!!.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) ViewHolderAncient.from(parent) else ViewHolderRecent.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ViewHolderAncient -> holder.bind(item, itemCount, homeViewModel, tracker!!)
            is ViewHolderRecent -> holder.bind(item, itemCount, homeViewModel, tracker!!)
        }
    }

//    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
//        super.onViewDetachedFromWindow(holder)
//        holder.itemView.setOnClickListener(null)
//    }


    class ViewHolderAncient(val binding: PoetItemAncientBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(
            item: Category,
            itemCount: Int,
            homeViewModel: HomeViewModel,
            tracker: SelectionTracker<Long>
        ) {
            binding.catItem = item
            binding.cardView.transitionName = item.text

            binding.checkBox.visibility = if (tracker.hasSelection()) View.VISIBLE else View.INVISIBLE
            binding.checkBox.isChecked = tracker.isSelected(item.poetID!!.toLong())

            tracker.addObserver(object: SelectionTracker.SelectionObserver<Long>() {
                override fun onSelectionChanged() {
                    if (homeViewModel.ancientPoetIds.indexOf(item.poetID) == bindingAdapterPosition){
                        binding.checkBox.visibility = if (tracker.hasSelection()) View.VISIBLE else View.INVISIBLE
                        binding.checkBox.isChecked = tracker.isSelected(item.poetID.toLong())
                    }
                }
            })

//            val params = itemView.layoutParams as ViewGroup.MarginLayoutParams
//            if (bindingAdapterPosition < homeViewModel.spanCount) {
//                params.updateMargins(
//                    top = itemView.context.resources.getDimensionPixelSize(R.dimen.poet_item_margins))
//            } else
//                params.updateMargins(top = 0)

//            if (bindingAdapterPosition >= itemCount - ((itemCount-1) % homeViewModel.spanCount + 1)) {
//                params.updateMargins(bottom = 24.dpTOpx(itemView.context.resources).toInt())
//            } else
//                params.updateMargins(bottom = 0)

            itemView.setOnClickListener {
                homeViewModel.apply {
                    viewpagePosition = bindingAdapterPosition
                    firstOpenedAncient = bindingAdapterPosition
                    firstOpenedRecent = -1
                    count = itemCount
                    ancient = item.ancient!!
                    navigatorExtra = FragmentNavigatorExtras(
                        binding.cardView to binding.cardView.transitionName)
                    reportEvent(HomeEvent.Navigate(Destinations.POET))
                }
            }
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object: ItemDetailsLookup.ItemDetails<Long>() {
                override fun getSelectionKey(): Long = itemId
                override fun getPosition(): Int = bindingAdapterPosition
            }

        companion object {
            fun from(parent: ViewGroup): ViewHolderAncient {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = PoetItemAncientBinding.inflate(layoutInflater, parent, false)
                return ViewHolderAncient(binding)
            }
        }
    }

    class ViewHolderRecent(val binding: PoetItemRecentBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(
            item: Category,
            itemCount: Int,
            homeViewModel: HomeViewModel,
            tracker: SelectionTracker<Long>
        ) {
            binding.catItem = item
            binding.cardView.transitionName = item.text

            binding.checkBox.visibility = if (tracker.hasSelection()) View.VISIBLE else View.INVISIBLE
            binding.checkBox.isChecked = tracker.isSelected(item.poetID!!.toLong())
//            itemView.isActivated = tracker.isSelected(item.poetID.toLong())
            GlideApp.with(binding.root.context).load(R.drawable.background_wood).into(binding.backgroundView)

            tracker.addObserver(object: SelectionTracker.SelectionObserver<Long>() {
                override fun onSelectionChanged() {
                    if (homeViewModel.recentPoetIds.indexOf(item.poetID) == bindingAdapterPosition) {
                        binding.checkBox.visibility = if (tracker.hasSelection()) View.VISIBLE else View.INVISIBLE
                        binding.checkBox.isChecked = tracker.isSelected(item.poetID.toLong())
                    }
                }
            })

//            val params = itemView.layoutParams as ViewGroup.MarginLayoutParams
//            if (bindingAdapterPosition < homeViewModel.spanCount) {
//                params.updateMargins(
//                    top = itemView.context.resources.getDimensionPixelSize(R.dimen.poet_item_margins))
//            } else
//                params.updateMargins(top = 0)

//            if (bindingAdapterPosition >= itemCount - ((itemCount-1) % homeViewModel.spanCount + 1)) {
//                params.updateMargins(bottom = 24.dpTOpx(itemView.context.resources).toInt())
//            } else
//                params.updateMargins(bottom = 0)

            itemView.setOnClickListener {
                homeViewModel.apply {
                    viewpagePosition = bindingAdapterPosition
                    firstOpenedAncient = -1
                    firstOpenedRecent = bindingAdapterPosition
                    count = itemCount
                    ancient = item.ancient!!
                    navigatorExtra = FragmentNavigatorExtras(
                        binding.cardView to binding.cardView.transitionName)
                    reportEvent(HomeEvent.Navigate(Destinations.POET))
                }
            }
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object: ItemDetailsLookup.ItemDetails<Long>() {
                override fun getSelectionKey(): Long = itemId
                override fun getPosition(): Int = bindingAdapterPosition
            }

        companion object {
            fun from(parent: ViewGroup): ViewHolderRecent {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = PoetItemRecentBinding.inflate(layoutInflater, parent, false)
                return ViewHolderRecent(binding)
            }
        }
    }

}

class CategoryDiffCallback : DiffUtil.ItemCallback<Category>(){
    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem == newItem
    }
}

class MyLookup(private val rv: RecyclerView, val ancient: Int) : ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = rv.findChildViewUnder(event.x, event.y)
        if(view != null) {
            return if (ancient == 0)
                (rv.getChildViewHolder(view) as PoetListAdapter.ViewHolderAncient).getItemDetails()
            else
                (rv.getChildViewHolder(view) as PoetListAdapter.ViewHolderRecent).getItemDetails()
        }
        return null
    }
}

class RecyclerViewIdKeyProvider(
    private val recyclerView: RecyclerView
    ) : ItemKeyProvider<Long>(SCOPE_MAPPED) {

    override fun getKey(position: Int): Long {
        return recyclerView.adapter?.getItemId(position)
            ?: throw IllegalStateException("RecyclerView adapter is not set!")
    }

    override fun getPosition(key: Long): Int {
        val viewHolder = recyclerView.findViewHolderForItemId(key)
        return viewHolder?.layoutPosition ?: RecyclerView.NO_POSITION
    }
}