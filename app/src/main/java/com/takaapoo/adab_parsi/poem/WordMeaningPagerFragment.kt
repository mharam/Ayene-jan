package com.takaapoo.adab_parsi.poem

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.PagerPoemMeaningBinding
import com.takaapoo.adab_parsi.util.BounceEdgeEffectFactory
import com.takaapoo.adab_parsi.util.Orientation
import com.takaapoo.adab_parsi.util.getColorFromAttr

class WordMeaningPagerFragment : Fragment() {

    val poemViewModel: PoemViewModel by activityViewModels()
    private var _binding: PagerPoemMeaningBinding? = null
    private val binding get() = _binding!!

    private val statusTextAnimator = ValueAnimator.ofInt(0, 4)
    private val statusTextUpdateListener = ValueAnimator.AnimatorUpdateListener { updatedValue ->
        binding.statusText.text =
            context?.resources?.getString(R.string.loading,
                ".".repeat(updatedValue.animatedValue as Int))
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        _binding = PagerPoemMeaningBinding.inflate(inflater, container, false)


        arguments?.takeIf { it.containsKey(ARG_POEM_MEANING) }?.apply {
            val dictionary = getInt(ARG_POEM_MEANING)
            var result = poemViewModel.meaning.value?.filter { it.dictionary == dictionary }
            if (result?.map { it.word }?.contains(poemViewModel.meanWord) == true) {
                result = result.filter { it.word.startsWith(poemViewModel.meanWord) ||
                        it.word.endsWith(poemViewModel.meanWord) }
            }

            val adapter = WordMeaningAdapter()
            binding.meanRecycle.adapter = adapter
            binding.meanRecycle.edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)
            binding.meanRecycle.recycledViewPool.setMaxRecycledViews(0, 0)
            binding.meanRecycle.setItemViewCacheSize(100)

            binding.statusText.visibility = View.VISIBLE
            binding.statusText.text = resources.getString(R.string.loading, "")

            when (poemViewModel.meanLoadStatus.value){
                MeaningLoadStatus.ERROR -> {
                    binding.statusText.visibility = View.VISIBLE
                    binding.statusText.setTextColor(requireContext().getColorFromAttr(R.attr.colorError))
                    binding.statusText.text = resources.getString(R.string.connection_failed)
                }
                MeaningLoadStatus.LOADING -> {
                    binding.statusText.visibility = View.VISIBLE
                    binding.statusText.text = resources.getString(R.string.loading, "")
                    statusTextAnimator.apply {
                        duration = 1000
                        repeatCount = ValueAnimator.INFINITE
                        addUpdateListener(statusTextUpdateListener)
                        start()
                    }
                }
                MeaningLoadStatus.DONE -> {
                    statusTextAnimator.removeUpdateListener(statusTextUpdateListener)
                    if (!result.isNullOrEmpty()){
                        binding.statusText.visibility = View.GONE
                        adapter.submitList(result)
                    } else {
                        binding.statusText.visibility = View.VISIBLE
                        binding.statusText.setTextColor(
                            ResourcesCompat.getColor(resources, R.color.orange_200, context?.theme))
                        binding.statusText.text = resources.getString(R.string.no_result)
                    }
                }
                else -> {}
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        statusTextAnimator.removeUpdateListener(statusTextUpdateListener)

        _binding = null
    }
}