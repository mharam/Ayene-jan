package com.takaapoo.adab_parsi.add

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.PoetLoadingItemBinding
import com.takaapoo.adab_parsi.network.PoetProperty
import com.takaapoo.adab_parsi.util.GlideApp
import timber.log.Timber
import java.util.*


class AddListAdapter(
    private val vm: AddViewModel,
    private val lifeOwner: LifecycleOwner
    ) : ListAdapter<PoetProperty, AddListAdapter.ViewHolder>(NotLoadedPoetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, lifeOwner, vm)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.addObserver(item)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        if (position != RecyclerView.NO_POSITION){
            val item = getItem(position)
            holder.removeObserver(item)
        }

        super.onViewRecycled(holder)
    }

    class ViewHolder private constructor(val binding: PoetLoadingItemBinding,
                                         val context: Context,
                                         private val owner: LifecycleOwner,
                                         val viewModel: AddViewModel)
        : RecyclerView.ViewHolder(binding.root) {

        private var pubHeight = 0
        private val wrenchAnimator = AnimatorInflater.loadAnimator(context, R.animator.wrench_rotate)

        fun bind(item: PoetProperty) {
            viewModel.progress.getOrPut(
                key = item.poetID,
                defaultValue = { MutableLiveData(-2) }
            )
            viewModel.installing.getOrPut(
                key = item.poetID,
                defaultValue = { MutableLiveData(false) }
            )
            binding.apply {
                property = item
                addViewModel = viewModel
                lifecycleOwner = owner
                downloadButton.background = ResourcesCompat.getDrawable(context.resources
                    , R.drawable.download_to_stop, context.theme)
                stopButton.background = ResourcesCompat.getDrawable(context.resources
                    , R.drawable.stop_to_download, context.theme)
            }
            wrenchAnimator.setTarget(binding.wrench)

            binding.publications.apply {
                doOnPreDraw {
                    val staticLayout = StaticLayout.Builder.obtain(
                        text, 0, text.length, paint, width
                    )
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .build()

                    pubHeight = staticLayout.height + paddingBottom + paddingTop
                    height = 0
                }
            }

            itemView.setOnClickListener {
                ObjectAnimator.ofInt(binding.publications, "height"
                    , if (binding.publications.height == 0) pubHeight else 0).apply {
                    interpolator = AccelerateInterpolator(1f)
                }.start()
            }

            binding.executePendingBindings()
        }

        fun addObserver(item: PoetProperty){
            viewModel.installing[item.poetID]?.observe(owner){ installing ->
                if (installing && !wrenchAnimator.isStarted) {
                    wrenchAnimator.start()
                    wrenchAnimator.doOnEnd {
                        if (viewModel.installing.containsKey(item.poetID))
                            Handler(Looper.getMainLooper()).postDelayed({wrenchAnimator.start()}, 200)
                    }
                }
            }

            viewModel.progress[item.poetID]?.observe(owner){ progress ->
                when (progress) {
                    -1 -> {
                        (binding.downloadButton.background as AnimatedVectorDrawable).start()
                        binding.stopButton.background = ResourcesCompat.getDrawable(context.resources
                            , R.drawable.stop_to_download, context.theme)

                        (binding.stopButton.background as AnimatedVectorDrawable).reset()
//                        binding.downloadProgress.setProgressCompat(0, false)
                    }
                    -2 -> {
                        binding.downloadButton.background = ResourcesCompat.getDrawable(context.resources
                            , R.drawable.download_to_stop, context.theme)
                    }
                    -3 -> {
                        (binding.stopButton.background as AnimatedVectorDrawable).start()
                        binding.downloadButton.background = ResourcesCompat.getDrawable(context.resources
                            , R.drawable.download_to_stop, context.theme)
                        (binding.downloadButton.background as AnimatedVectorDrawable).reset()
                    }
                    else -> {
                        binding.stopButton.background = ResourcesCompat.getDrawable(context.resources
                            , R.drawable.stop_to_download, context.theme)
                        (binding.stopButton.background as AnimatedVectorDrawable).reset()
                        (binding.downloadButton.background as AnimatedVectorDrawable).reset()
                        binding.downloadProgress.setProgressCompat(progress, true)
                    }
                }
            }
        }

        fun removeObserver(item: PoetProperty){
            viewModel.installing[item.poetID]?.removeObservers(owner)
            viewModel.progress[item.poetID]?.removeObservers(owner)
        }


        companion object {
            fun from(parent: ViewGroup, owner: LifecycleOwner, vm: AddViewModel): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = PoetLoadingItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context, owner, vm)
            }
        }
    }
}

class NotLoadedPoetDiffCallback : DiffUtil.ItemCallback<PoetProperty>() {
    override fun areItemsTheSame(oldItem: PoetProperty, newItem: PoetProperty): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PoetProperty, newItem: PoetProperty): Boolean {
        return oldItem == newItem
    }
}

class MyPreloadModelProvider(private val properties: List<PoetProperty>?, val frag: Fragment) :
    PreloadModelProvider<String?> {

    private val scale = frag.resources.displayMetrics.density

    override fun getPreloadItems(position: Int): List<String?> {
        val url: String? = properties?.get(position)?.thumbnailURL
        return if (TextUtils.isEmpty(url)) {
            Collections.emptyList()
        } else
            Collections.singletonList(url)
    }

    override fun getPreloadRequestBuilder(url: String): RequestBuilder<*> {
        return GlideApp.with(frag)
            .load(url)
            .transform(RoundedCorners((10*scale).toInt()))
    }
}