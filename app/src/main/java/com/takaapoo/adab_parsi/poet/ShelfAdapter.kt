package com.takaapoo.adab_parsi.poet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.databinding.ShelfPictureLayoutAncientBinding
import com.takaapoo.adab_parsi.databinding.ShelfPictureLayoutRecentBinding
import com.takaapoo.adab_parsi.util.dpTOpx


class ShelfAdapter(val itemcount: Int, private val catItem: Category)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> 0
            else -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            0 -> {
                if (catItem.ancient == 0)
                    AncientPictureViewHolder.from(parent)
                else
                    RecentPictureViewHolder.from(parent)
            }
            else -> ShelfViewHolder.from(parent)
        }
    }

    override fun getItemCount() = itemcount

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AncientPictureViewHolder -> holder.bind(catItem)
            is RecentPictureViewHolder -> holder.bind(catItem)
        }
    }


    class ShelfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        companion object {
            fun from(parent: ViewGroup): ShelfViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.shelf_layout, parent, false)

                return ShelfViewHolder(view)
            }
        }
    }

    class AncientPictureViewHolder(val binding: ShelfPictureLayoutAncientBinding)
        : RecyclerView.ViewHolder(binding.root){

        private val ghabHeight = binding.root.resources.getDimension(R.dimen.poet_ghab_height)

        fun bind(catItem: Category){
            binding.item = catItem

            binding.imageFrame.doOnPreDraw {
                when {
                    binding.root.width > 750.dpTOpx(it.resources) ->
                        binding.imageLayout.updateLayoutParams { height = (ghabHeight * 1.5f).toInt() }
                    binding.root.width > 550.dpTOpx(it.resources) ->
                        binding.imageLayout.updateLayoutParams { height = (ghabHeight * 1.25f).toInt() }
                }
                binding.imageFrame.doOnPreDraw {
                    val padding = (0.0387f * binding.imageFrame.height).toInt()
                    binding.topImage.updatePadding(left = padding, top = padding, right = padding,
                        bottom = padding)
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): AncientPictureViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ShelfPictureLayoutAncientBinding.inflate(layoutInflater, parent, false)
                return AncientPictureViewHolder(binding)
            }
        }
    }

    class RecentPictureViewHolder(val binding: ShelfPictureLayoutRecentBinding)
        : RecyclerView.ViewHolder(binding.root){

        private val ghabHeight = binding.root.resources.getDimension(R.dimen.poet_ghab_height)

        fun bind(catItem: Category){
            binding.item = catItem

            binding.imageFrame.doOnPreDraw {
                when {
                    binding.root.width > 750.dpTOpx(it.resources) ->
                        binding.imageLayout.updateLayoutParams { height = (ghabHeight * 1.5f).toInt() }
                    binding.root.width > 550.dpTOpx(it.resources) ->
                        binding.imageLayout.updateLayoutParams { height = (ghabHeight * 1.25f).toInt() }
                }
                binding.imageFrame.doOnPreDraw {
                    val padding = (0.070f * binding.imageFrame.height).toInt()
                    binding.topImage.updatePadding(left = padding, top = padding, right = padding,
                        bottom = padding)
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): RecentPictureViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ShelfPictureLayoutRecentBinding.inflate(layoutInflater, parent, false)
                return RecentPictureViewHolder(binding)
            }
        }
    }

}