package com.takaapoo.adab_parsi.add

import android.animation.AnimatorInflater
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.map
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.databinding.PoetLoadingItemBinding
import com.takaapoo.adab_parsi.network.PoetProperty
import com.takaapoo.adab_parsi.util.GlideApp
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
        private val workManager = WorkManager.getInstance(context)
        private var downloadStarted = false

        fun bind(item: PoetProperty) {
            downloadStarted = false
            val workInfosLiveData = workManager.getWorkInfosForUniqueWorkLiveData(item.poetID.toString())
            viewModel.downloadInfo.getOrPut(
                key = item.poetID,
                defaultValue = {
                    workInfosLiveData.map { workInfoList ->
                        workInfoList.firstOrNull()?.let {
                            PoetDownloadInfo(
                                progress = it.progress.getInt(PROGRESS, -2),
                                state = it.state,
                                installing = it.progress.getBoolean(INSTALLING, false),
                                outputData = it.outputData
                            )
                        }
                    }
                }
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
                viewModel.reportEvent(AddEvent.PoetTouched(binding.publications, pubHeight))
            }
            binding.downloadButton.setOnClickListener {
                buttonClicked(item)
            }
            binding.stopButton.setOnClickListener {
                buttonClicked(item)
            }

            binding.executePendingBindings()
        }

        private fun buttonClicked(item: PoetProperty){
            viewModel.reportEvent(AddEvent.DownloadPoet(item))
            val workState = viewModel.downloadInfo[item.poetID]?.value?.state
            if (workState == WorkInfo.State.RUNNING || workState == WorkInfo.State.ENQUEUED){
                (binding.stopButton.background as AnimatedVectorDrawable).reset()
                binding.downloadButton.isVisible = false
                binding.stopButton.isVisible = true
                (binding.stopButton.background as AnimatedVectorDrawable).start()
            }
        }

        fun addObserver(item: PoetProperty){
            viewModel.downloadInfo[item.poetID]?.observe(owner){ poetDownloadInfo ->
                // It's here because of Fail state. In Fail state downloadButton become visible
                binding.downloadButton.isVisible = (poetDownloadInfo?.state == null ||
                        poetDownloadInfo.state == WorkInfo.State.ENQUEUED ||
                        (poetDownloadInfo.state?.isFinished == true && !downloadStarted) ||
                        (poetDownloadInfo.state == WorkInfo.State.RUNNING && poetDownloadInfo.progress < 0))

                binding.stopButton.isVisible = ((poetDownloadInfo?.state?.isFinished == true &&
                        downloadStarted && !poetDownloadInfo.installing) ||
                        (poetDownloadInfo?.state == WorkInfo.State.RUNNING && poetDownloadInfo.progress >= 0 &&
                                !poetDownloadInfo.installing))

                binding.downloadProgress.isIndeterminate = (poetDownloadInfo?.state == WorkInfo.State.ENQUEUED ||
                        poetDownloadInfo?.progress == -1)
                binding.downloadProgress.visibility = if ((poetDownloadInfo?.state == WorkInfo.State.ENQUEUED ||
                            poetDownloadInfo?.state == WorkInfo.State.RUNNING) && !poetDownloadInfo.installing) View.VISIBLE
                else View.INVISIBLE

                if (poetDownloadInfo?.installing == true) {
                    binding.wrench.isVisible = true
                    if (!wrenchAnimator.isStarted){
                        wrenchAnimator.start()
                        wrenchAnimator.doOnEnd {
                            if (viewModel.downloadInfo.containsKey(item.poetID))
                                Handler(Looper.getMainLooper()).postDelayed({wrenchAnimator.start()}, 200)
                        }
                    }
                } else {
                    binding.wrench.isVisible = false
                }

                when(poetDownloadInfo?.state){
                    WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                        if (!downloadStarted){
                            (binding.stopButton.background as AnimatedVectorDrawable).reset()
                            (binding.downloadButton.background as AnimatedVectorDrawable).reset()
                            binding.downloadButton.isVisible = true
                            binding.stopButton.isVisible = false
                            (binding.downloadButton.background as AnimatedVectorDrawable).start()
                        }
                        downloadStarted = true
                    }
                    WorkInfo.State.SUCCEEDED -> {
//                        viewModel.modifyAllPoet(item.poetID)
                        (binding.stopButton.background as AnimatedVectorDrawable).reset()
                        (binding.downloadButton.background as AnimatedVectorDrawable).reset()
                        binding.downloadButton.isVisible = true
                        binding.stopButton.isVisible = false
                        downloadStarted = false
                    }
                    WorkInfo.State.FAILED -> {
                        binding.downloadButton.isVisible = false
                        binding.stopButton.isVisible = true
                        (binding.stopButton.background as AnimatedVectorDrawable).start()
                        if (downloadStarted)
                            poetDownloadInfo.outputData.getString(Error)?.let { errorMessage ->
                                viewModel.reportEvent(AddEvent.ShowSnack(errorMessage))
                            }
                        downloadStarted = false
                    }
                    WorkInfo.State.CANCELLED -> {
                        downloadStarted = false
                    }
                    else -> {}
                }

                poetDownloadInfo?.progress?.let {
                    if (it > 0)
                        binding.downloadProgress.setProgressCompat(it, true)
                }
            }
        }

        fun removeObserver(item: PoetProperty){
            viewModel.downloadInfo[item.poetID]?.removeObservers(owner)
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