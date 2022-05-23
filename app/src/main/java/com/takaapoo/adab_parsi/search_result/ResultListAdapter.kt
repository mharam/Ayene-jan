package com.takaapoo.adab_parsi.search_result

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateMargins
import androidx.fragment.app.findFragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.MainActivity
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.SearchContent
import com.takaapoo.adab_parsi.databinding.SearchResultItemBinding
import com.takaapoo.adab_parsi.poem.DARK_ALPHA_MAX
import com.takaapoo.adab_parsi.search.SearchViewModel
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.*
import kotlin.math.max


class ResultListAdapter(private val resultFragment: ResultFragment, private val day: Boolean)
    : ListAdapter<SearchContent, ResultListAdapter.ViewHolder>(ResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, resultFragment)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), day)
    }

    class ViewHolder(val binding: SearchResultItemBinding, private val svm: SearchViewModel,
                     private val stvm: SettingViewModel, val context: Context, val rf: ResultFragment)
        : RecyclerView.ViewHolder(binding.root){

        private val splittedQuery =
            makeTextBiErab(svm.submittedSearch.filter { it.isLetterOrDigit() || it.isWhitespace()})
                .splitToSequence(' ').toList()

        fun bind(item: SearchContent, day: Boolean) {
            binding.cardView.transitionName = item.rowId1.toString()
            binding.number.text =  itemView.context.resources.getString(R.string.number_dot,
                engNumToFarsiNum(bindingAdapterPosition+1))
            binding.address.text = svm.resultAddress(item)

            stvm.brightness.observe(rf.viewLifecycleOwner){
                binding.darkener.alpha = DARK_ALPHA_MAX * (1 - it / 100)
            }
            stvm.paperColorPref.observe(rf.viewLifecycleOwner){
                if (day)
                    binding.cardView.setCardBackgroundColor(stvm.paperColor)
            }

            itemView.setOnClickListener{ view ->
                val frag = itemView.findFragment<ResultFragment>()
                val mainActivity = frag.activity as MainActivity
                frag.setTransitionType(binding.cardView)

//                binding.cardView.transitionName = "Result_transition"
                svm.poemPosition = bindingAdapterPosition
                svm.lastResultOpened = bindingAdapterPosition
                svm.detailPagerPosition = bindingAdapterPosition
                svm.comeFromResultFragment = true
                svm.bottomViewedResultHeight = 0
                svm.topViewedResultHeight = 0

                if (mainActivity.binding.drawerLayout.tag == "land_scape"){
                    if (mainActivity.getContainerFrag() is DetailFragment) {
                        (mainActivity.getContainerFrag() as DetailFragment).setViewPagerItem()
                    } else {
                        mainActivity.addFragmentToContainer(DetailFragment())
                    }
                } else {
                    try {
                        val extras = FragmentNavigatorExtras(
                            binding.cardView to binding.cardView.transitionName)
                        val action = ResultFragmentDirections.actionResultFragmentToDetailFragment()
                        view.findNavController().navigate(action, extras)
                    } catch (e: Exception) { }
                }
            }

            when (item.position) {
                0, 1 -> {
                    binding.paragText.visibility = View.GONE
                    binding.mesra1Text.visibility = View.VISIBLE
                    binding.mesra2Text.visibility = View.VISIBLE

                    val mesra1 = (if (item.position == 0) item.majorText else item.minorText)?.trim() ?: ""
                    val mesra2 = (if (item.position == 0) item.minorText else item.majorText)?.trim() ?: ""

                    val mesra1Width = stvm.paint.measureText(mesra1).toInt()
                    val mesra2Width = stvm.paint.measureText(mesra2).toInt()
                    val mesraMaxWidth = maxOf(mesra1Width, mesra2Width).coerceAtMost(
                        9*(svm.rootWidth - (svm.startMargin + svm.endMargin))/10 )
                    val beitOneLine = svm.mesraContainerWidth > mesraMaxWidth + svm.versePadding + 4
                    val mesraOverlapWidth = max(
                        (mesraMaxWidth/2 + 2 * svm.versePadding),
                        2*(mesraMaxWidth + 2 * svm.versePadding) -
                                (svm.rootWidth - (svm.startMargin + svm.endMargin)))

                    binding.mesra1Text.apply {
                        val params = layoutParams as ConstraintLayout.LayoutParams
//                        params.width = mesraMaxWidth +
//                                2 * context.resources.getDimensionPixelSize(R.dimen.verse_padding)
                        if (beitOneLine) {
                            (binding.sepView.layoutParams as ConstraintLayout.LayoutParams).width =
                                2*svm.verseSeparation/3
                            binding.sepView.requestLayout()
                            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                            params.endToStart = R.id.sep_view
                        } else {
                            (binding.sepView.layoutParams as ConstraintLayout.LayoutParams).width = mesraOverlapWidth
                            binding.sepView.requestLayout()
                            params.endToStart = ConstraintLayout.LayoutParams.UNSET
                            params.endToEnd = R.id.sep_view
                        }

                        layoutParams = params

                        val normalizedText = widthNormalizer(mesra1, mesra1Width, mesraMaxWidth, stvm)
                        spanIndex = normalizedText.findSpanIndex2(splittedQuery)
                        searchHilightColor = context.getColorFromAttr(R.attr.colorSecondary)
                        text =  normalizedText
                    }
                    binding.mesra2Text.apply {
                        val params = layoutParams as ConstraintLayout.LayoutParams
//                        params.width = mesraMaxWidth +
//                                2 * context.resources.getDimensionPixelSize(R.dimen.verse_padding)
                        if (beitOneLine) {
                            params.startToStart = ConstraintLayout.LayoutParams.UNSET
                            params.startToEnd = R.id.sep_view
                            params.updateMargins(top = 0)
                        } else {
                            params.startToEnd = ConstraintLayout.LayoutParams.UNSET
                            params.startToStart = R.id.sep_view
                            params.updateMargins(top = (stvm.textHeight * stvm.verseVertSep).toInt())
                        }

                        layoutParams = params

                        val normalizedText = widthNormalizer(mesra2, mesra2Width, mesraMaxWidth, stvm)
                        spanIndex = normalizedText.findSpanIndex2(splittedQuery)
                        searchHilightColor = context.getColorFromAttr(R.attr.colorSecondary)
                        text =  normalizedText
                    }
                }
                2, 3 -> {
                    binding.mesra1Text.visibility = View.GONE
                    binding.mesra2Text.visibility = View.GONE

                    val mesra1 = (if (item.position == 2) item.majorText else item.minorText)?.trim() ?: ""
                    val mesra2 = (if (item.position == 2) item.minorText else item.majorText)?.trim() ?: ""

                    val mesra1Width = stvm.paint.measureText(mesra1).toInt()
                    val mesra2Width = stvm.paint.measureText(mesra2).toInt()
                    val mesraMaxWidth = maxOf(mesra1Width, mesra2Width)

                    binding.paragText. apply {
                        visibility = View.VISIBLE

                        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        val beit = context.getString(R.string.beit2
                            , widthNormalizer(mesra1, mesra1Width, mesraMaxWidth, stvm)
                            , widthNormalizer(mesra2, mesra2Width, mesraMaxWidth, stvm))

                        spanIndex = beit.findSpanIndex2(splittedQuery)
                        searchHilightColor = context.getColorFromAttr(R.attr.colorSecondary)
                        text = beit
                    }
                }
                else -> {
                    binding.mesra1Text.visibility = View.GONE
                    binding.mesra2Text.visibility = View.GONE
                    binding.paragText. apply {
                        visibility = View.VISIBLE
                        textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START

                        val paragText = context.getString(R.string.tabbedText,
                            item.majorText?.trim() ?: "").shorten(splittedQuery, 200)

                        spanIndex = paragText?.findSpanIndex2(splittedQuery)
                        searchHilightColor = context.getColorFromAttr(R.attr.colorSecondary)
                        text = paragText
                    }
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup, resultFragment: ResultFragment): ViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SearchResultItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, resultFragment.searchViewModel, resultFragment.settingViewModel,
                    parent.context, resultFragment)
            }
        }

    }

}

class ResultDiffCallback : DiffUtil.ItemCallback<SearchContent>(){
    override fun areItemsTheSame(oldItem: SearchContent, newItem: SearchContent): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: SearchContent, newItem: SearchContent): Boolean {
        return oldItem == newItem
    }
}