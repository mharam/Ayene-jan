package com.takaapoo.adab_parsi.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.SearchSuggest
import com.takaapoo.adab_parsi.databinding.SearchSuggestionItemBinding


class SearchListAdapter(private val searchViewModel: SearchViewModel) :
    ListAdapter<SearchSuggest, SearchListAdapter.ViewHolder>(RecentSearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, searchViewModel)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(val binding: SearchSuggestionItemBinding, private val searchViewModel: SearchViewModel)
        : RecyclerView.ViewHolder(binding.root){

        fun bind(searchItem: SearchSuggest) {
            binding.searchText.text = searchItem.text
            binding.icon.setImageResource( if (searchItem.isRecentSearch)
                R.drawable.ic_baseline_history_24
            else
                R.drawable.ic_baseline_search_24
            )
            binding.edit.setOnClickListener{
                searchViewModel.searchSubmit = false
                searchViewModel.searchQuery.value = searchItem.text
            }
            binding.searchText.setOnClickListener{
                searchViewModel.searchSubmit = true
                searchViewModel.searchQuery.value = searchItem.text
            }
        }

        companion object {
            fun from(parent: ViewGroup, svm: SearchViewModel): ViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SearchSuggestionItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, svm)
            }
        }

    }

}


class RecentSearchDiffCallback : DiffUtil.ItemCallback<SearchSuggest>(){
    override fun areItemsTheSame(oldItem: SearchSuggest, newItem: SearchSuggest): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: SearchSuggest, newItem: SearchSuggest): Boolean {
        return oldItem == newItem
    }
}