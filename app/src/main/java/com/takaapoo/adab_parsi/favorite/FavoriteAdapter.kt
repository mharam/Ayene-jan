package com.takaapoo.adab_parsi.favorite

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
import com.takaapoo.adab_parsi.database.RankedFavoriteContent
import com.takaapoo.adab_parsi.databinding.SearchResultItemBinding
import com.takaapoo.adab_parsi.poem.DARK_ALPHA_MAX
import com.takaapoo.adab_parsi.setting.SettingViewModel
import com.takaapoo.adab_parsi.util.engNumToFarsiNum
import com.takaapoo.adab_parsi.util.widthNormalizer
import kotlin.math.max

class FavoriteAdapter(private val favoriteFragment: FavoriteFragment, private val day: Boolean)
    : ListAdapter<RankedFavoriteContent, FavoriteAdapter.ViewHolder>(FavoriteDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return (item.verse1Order.toLong() shl 32) + (item.poemm.id?.toLong() ?: 0L)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, favoriteFragment)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), day)
    }

    class ViewHolder(val binding: SearchResultItemBinding, private val fvm: FavoriteViewModel,
                     private val svm: SettingViewModel, private val fvf: FavoriteFragment
    ) : RecyclerView.ViewHolder(binding.root){

//        private val paperColorIds = arrayOf(R.color.paper_1, R.color.paper_2, R.color.paper_3)

        fun bind(item: RankedFavoriteContent, day: Boolean) {
            binding.cardView.transitionName = "TN${item.poemm.id},${item.verse1Order}"
            binding.number.text = itemView.context.resources.getString(R.string.number_dot,
                engNumToFarsiNum(bindingAdapterPosition+1))
            binding.address.text = fvm.favoriteAddress(item)

            svm.brightness.observe(fvf.viewLifecycleOwner){
                binding.darkener.alpha = DARK_ALPHA_MAX * (1 - it / 100)
            }
            svm.paperColorPref.observe(fvf.viewLifecycleOwner){
                if (day)
                    binding.cardView.setCardBackgroundColor(svm.paperColor)
            }

            itemView.setOnClickListener{ view ->
                val frag = itemView.findFragment<FavoriteFragment>()
                val mainActivity = frag.activity as MainActivity
                frag.setTransitionType(binding.cardView)

                fvm.poemPosition = bindingAdapterPosition
                fvm.lastResultOpened = bindingAdapterPosition
                fvm.detailPagerPosition = bindingAdapterPosition
                fvm.comeFromFavoriteFragment = true
                fvm.bottomViewedResultHeight = 0
                fvm.topViewedResultHeight = 0
                fvm.favoriteListDisplace = 0
                fvm.favoriteListAddedScroll = 0

                if (mainActivity.binding.drawerLayout.tag == "land_scape"){
                    if (mainActivity.getContainerFrag() is FavoriteDetailFragment)
                        (mainActivity.getContainerFrag() as FavoriteDetailFragment).setViewPagerItem()
                    else {
                        mainActivity.addFragmentToContainer(FavoriteDetailFragment())
                    }
                } else {
                    try {
                        val extras =
                            FragmentNavigatorExtras(binding.cardView to binding.cardView.transitionName)
                        val action = FavoriteFragmentDirections.actionFavoriteFragmentToFavoriteDetailFragment()
                        view.findNavController().navigate(action, extras)
                    } catch (e: Exception){}
                }
            }

            val mesra1 = item.verse1Text?.trim() ?: ""
            val mesra2 = item.verse2Text?.trim() ?: ""
            val mesra1Width = svm.paint.measureText(mesra1).toInt()
            val mesra2Width = svm.paint.measureText(mesra2).toInt()
            val mesraMaxWidth = maxOf(mesra1Width, mesra2Width)

            when (item.verse1Position) {
                0, 1 -> {
                    binding.paragText.visibility = View.GONE
                    binding.mesra1Text.visibility = View.VISIBLE
                    binding.mesra2Text.visibility = View.VISIBLE

                    val beitOneLine = fvm.mesraContainerWidth > mesraMaxWidth + fvm.versePadding + 4
                    val mesraOverlapWidth = max((mesraMaxWidth/2 + 2 * fvm.versePadding)
                        , 2*(mesraMaxWidth + 2 * fvm.versePadding) -
                                (fvm.rootWidth - (fvm.startMargin + fvm.endMargin)))


                    binding.mesra1Text.apply {
                        val params = layoutParams as ConstraintLayout.LayoutParams
                        if (beitOneLine) {
                            (binding.sepView.layoutParams as ConstraintLayout.LayoutParams).width =
                                2*fvm.verseSeparation/3
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
                        val normalizedText = widthNormalizer(mesra1, mesra1Width, mesraMaxWidth, svm)
                        text =  normalizedText
                    }
                    binding.mesra2Text.apply {
                        val params = layoutParams as ConstraintLayout.LayoutParams
                        if (beitOneLine) {
                            params.startToStart = ConstraintLayout.LayoutParams.UNSET
                            params.startToEnd = R.id.sep_view
                            params.updateMargins(top = 0)
                        } else {
                            params.startToEnd = ConstraintLayout.LayoutParams.UNSET
                            params.startToStart = R.id.sep_view
                            params.updateMargins(top = (svm.textHeight * svm.verseVertSep).toInt())
                        }
                        layoutParams = params
                        val normalizedText = widthNormalizer(mesra2, mesra2Width, mesraMaxWidth, svm)
                        text =  normalizedText
                    }
                }
                2, 3 -> {
                    binding.mesra1Text.visibility = View.GONE
                    binding.mesra2Text.visibility = View.GONE

                    binding.paragText. apply {
                        visibility = View.VISIBLE

                        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        val beit = context.getString(
                            R.string.beit2,
                            widthNormalizer(mesra1, mesra1Width, mesraMaxWidth, svm),
                            widthNormalizer(mesra2, mesra2Width, mesraMaxWidth, svm)
                        )
                        text = beit
                    }
                }
                else -> {
                    binding.mesra1Text.visibility = View.GONE
                    binding.mesra2Text.visibility = View.GONE
                    binding.paragText. apply {
                        visibility = View.VISIBLE
                        textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START

                        val paragText = if (mesra1.length <= 200) mesra1 else
                            mesra1.substring(0, 200).substringBeforeLast(' ') + " ..."
                        text = paragText
                    }
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup, fvf: FavoriteFragment): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SearchResultItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, fvf.favoriteViewModel, fvf.settingViewModel, fvf)
            }
        }

    }

}

class FavoriteDiffCallback : DiffUtil.ItemCallback<RankedFavoriteContent>(){
    override fun areItemsTheSame(oldItem: RankedFavoriteContent, newItem: RankedFavoriteContent): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: RankedFavoriteContent, newItem: RankedFavoriteContent): Boolean {
        return oldItem == newItem
    }
}