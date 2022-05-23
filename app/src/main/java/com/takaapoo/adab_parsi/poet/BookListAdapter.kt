package com.takaapoo.adab_parsi.poet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.drawToBitmap
import androidx.core.view.updateLayoutParams
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.databinding.BookItemBinding
import com.takaapoo.adab_parsi.util.dpTOpx


class BookListAdapter(private val poetPagerFragment: PoetPagerFragment)
    : ListAdapter<Content, RecyclerView.ViewHolder>(CategoryDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            in 0 until poetPagerFragment.poetViewModel.bookShelfSpanCount  -> 0
            else -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            0 -> EmptyViewHolder.from(parent)
            else -> ViewHolder.from(parent, poetPagerFragment)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            val item = getItem(position)
            holder.bind(item, itemCount, currentList)
        }
    }

//    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
//        super.onViewDetachedFromWindow(holder)
//        holder.itemView.setOnClickListener(null)
//    }

    class ViewHolder(val binding: BookItemBinding, private val poetPagerFragment: PoetPagerFragment)
        : RecyclerView.ViewHolder(binding.root){

        private val aspectRatio = poetPagerFragment.requireView().let { it.width.toFloat() / it.height}
        fun bind(item: Content, itemCount: Int, currentList: List<Content> ) {

            binding.bookItemLayout.transitionName = item.text
            binding.bookTitle.text = item.text
            binding.imageView.updateLayoutParams {
                width =
                    (aspectRatio * height).coerceAtLeast(
                        poetPagerFragment.resources.getDimension(R.dimen.book_min_width)).toInt()
            }
//            binding.imageView.drawable.apply {
//                setTintMode(PorterDuff.Mode.SRC_ATOP)
//                setTint(ResourcesCompat.getColor(binding.root.resources, R.color.black_overlay_light,
//                        binding.root.context?.theme))
//            }

//            binding.imageView.apply {
//                doOnPreDraw {
//                    val params = layoutParams
//                    params.height =
//                        ((0.85f * poetPagerFragment.requireView().height / poetPagerFragment.requireView().width)
//                            .coerceAtLeast(1.35f) * width).toInt()
//                    layoutParams = params
//                }
//            }

            itemView.setOnClickListener { view ->
                poetPagerFragment.poetViewModel.apply {
                    bookPosition = bindingAdapterPosition - bookShelfSpanCount
                    count = itemCount - bookShelfSpanCount
                    bookListItems = currentList.subList(bookShelfSpanCount, currentList.size)
                }
                poetPagerFragment.apply {
                    bookViewModel.bookFirstOpening = true
                    binding.root.doOnLayout {
                        bookViewModel.poetLibContentShot = binding.poetLayout.drawToBitmap()
                    }
//                    bookViewModel.poetLibContentShot = Bitmap.createBitmap(binding.poetLayout.width
//                        , binding.poetLayout.height, Bitmap.Config.ARGB_8888)
//                    this.view?.draw(Canvas(bookViewModel.poetLibContentShot!!))
                }


//                poetPagerFragment.setTransitionType(binding.bookItemLayout)
                try {
                    val extras = FragmentNavigatorExtras(
                        binding.bookItemLayout to binding.bookItemLayout.transitionName)
                    val action = PoetFragmentDirections.actionPoetFragmentToBookFragment()
                    view.findNavController().navigate(action, extras)
                } catch (e: Exception) { }
            }

        }

        companion object {
            fun from(parent: ViewGroup, poetPagerFragment: PoetPagerFragment): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = BookItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, poetPagerFragment)
            }
        }
    }

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        companion object {
            fun from(parent: ViewGroup): EmptyViewHolder {
                LayoutInflater.from(parent.context)
                val view = View(parent.context)
                view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    (parent.resources.getDimension(R.dimen.poet_ghab_height) * when {
                        parent.width > 750.dpTOpx(parent.resources) -> 1.5f
                        parent.width > 550.dpTOpx(parent.resources) -> 1.25f
                        else -> 1f
                    }).toInt()
                )

                return EmptyViewHolder(view)
            }
        }
    }
}

class CategoryDiffCallback : DiffUtil.ItemCallback<Content>(){
    override fun areItemsTheSame(oldItem: Content, newItem: Content): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Content, newItem: Content): Boolean {
        return oldItem == newItem
    }
}