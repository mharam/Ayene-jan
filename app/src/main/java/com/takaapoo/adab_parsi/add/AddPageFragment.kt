package com.takaapoo.adab_parsi.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.takaapoo.adab_parsi.databinding.PagerAddBinding
import com.takaapoo.adab_parsi.util.BounceEdgeEffectFactory
import com.takaapoo.adab_parsi.util.GlideApp
import com.takaapoo.adab_parsi.util.Orientation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddPageFragment: Fragment() {

    private val addViewModel: AddViewModel by activityViewModels()

    private var _binding: PagerAddBinding? = null
    private val binding get() = _binding!!
    private var adapter: AddListAdapter? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = PagerAddBinding.inflate(inflater, container, false)

        adapter = AddListAdapter(addViewModel, viewLifecycleOwner)

        binding.viewModel = addViewModel
        binding.remainingPoetList.adapter = adapter
        binding.remainingPoetList.edgeEffectFactory = BounceEdgeEffectFactory(Orientation.VERTICAL)
//        binding.remainingPoetList.recycledViewPool.setMaxRecycledViews(0, 0)
        binding.remainingPoetList.setItemViewCacheSize(10)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        val allPoetValue = (parentFragment as AddFragment).allPoetValue

        arguments?.takeIf { it.containsKey(ARG_ADD_PAGE) }?.apply {
            addViewModel.allPoet.observe(viewLifecycleOwner) { items ->
                items?.filter { it.parentID == 0 && it.ancient == getInt(ARG_ADD_PAGE) }
                    .let { list ->
                        adapter?.submitList(list)
                    }
            }

            val sizeProvider = ViewPreloadSizeProvider<String>()
            val modelProvider = MyPreloadModelProvider(addViewModel.allPoet.value?.filter {
                it.parentID == 0 && it.ancient == getInt(ARG_ADD_PAGE)}, this@AddPageFragment)
            val preLoader = RecyclerViewPreloader<String>(
                GlideApp.with(this@AddPageFragment), modelProvider, sizeProvider, 8)

            binding.remainingPoetList.addOnScrollListener(preLoader)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.remainingPoetList.clearOnScrollListeners()
        binding.remainingPoetList.adapter = null

        adapter = null
        _binding = null
    }



}