package com.takaapoo.adab_parsi.poem

import android.annotation.SuppressLint
import android.text.Editable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Verse
import com.takaapoo.adab_parsi.databinding.PoemItemBinding
import com.takaapoo.adab_parsi.util.*


class PoemContentAdapter(private val poemPagerFragment: PoemPagerFragment)
    : ListAdapter<Verse, PoemContentAdapter.PoemContentViewHolder>(PoemDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).verseOrder.toLong()
    }

    override fun getItemViewType(position: Int) = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoemContentViewHolder {
        return PoemContentViewHolder.from(parent, poemPagerFragment)
    }

    override fun onBindViewHolder(holder: PoemContentViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onViewRecycled(holder: PoemContentViewHolder) {
        super.onViewRecycled(holder)
        holder.removeWatcher()
    }

    class PoemContentViewHolder(val binding: PoemItemBinding, private val poemPagerFragment: PoemPagerFragment)
        : RecyclerView.ViewHolder(binding.root) {

        private val poemViewModel = poemPagerFragment.poemViewModel
        private val stvm = poemPagerFragment.settingViewModel
        private val svm = poemPagerFragment.searchViewModel

        private val splittedQuery =
            if (svm.openedFromDetailFrag) makeTextBiErab(svm.submittedSearch
                .filter { it.isLetterOrDigit() || it.isWhitespace() }).splitToSequence(' ').toList()
            else emptyList()

        private var commentHeight = 0
        private lateinit var commentWatcher: Watcher

        init {
            try {
                commentHeight = poemPagerFragment.resources.getDimension(R.dimen.comment_height).toInt()
            } catch (_: Exception){}
        }

        inner class Watcher(val item: Verse) : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) {
                binding.save.isVisible = (s.toString() != (item.note ?: ""))
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        fun bind(item: Verse){
            if (!poemPagerFragment.isAdded)
                return

            itemView.isActivated = poemViewModel.selectedVerses[item.poemId]?.value?.contains(itemId.toInt()) == true
            binding.beitFavorite.visibility = if (item.favorite == null) View.GONE else View.VISIBLE
            binding.beitComment.visibility = if (item.note == null) View.GONE else View.VISIBLE

            val commentParams = binding.comment.layoutParams
            if (poemViewModel.noteOpenedVerses[item.poemId]?.contains(item.verseOrder) == true){
                binding.comment.visibility = View.VISIBLE
                commentParams.height = commentHeight
            } else {
                binding.comment.visibility = View.GONE
                commentParams.height = 0
            }
            binding.comment.layoutParams = commentParams
            TooltipCompat.setTooltipText(binding.save, poemPagerFragment.resources.getString(R.string.save))

            commentWatcher = Watcher(item)
            binding.commentText.addTextChangedListener(commentWatcher)
            binding.commentText.setText(item.note)

            binding.save.setOnClickListener {
                poemPagerFragment.saveComment(binding.commentText.text.toString(), item.verseOrder,
                    binding.save)
            }

            if (item.text!!.contains('¶') && item.position == 0){
                val mesra1 = item.text!!.substringBefore("¶").trim()
                val mesra2 = item.text!!.substringAfter("¶").trim()

                binding.paragText.visibility = View.GONE

                binding.mesra1Text.apply {
                    visibility = View.VISIBLE

                    val params = layoutParams as ConstraintLayout.LayoutParams
                    if (poemPagerFragment.beitOneLine)
                        params.endToStart = R.id.guid_view
                    else {
                        (binding.guidView.layoutParams as ConstraintLayout.LayoutParams).width =
                            poemPagerFragment.mesraOverlapWidth
                        binding.guidView.requestLayout()
                        params.endToEnd = R.id.guid_view
                    }

                    val normalizedText = poemPagerFragment.getString(
                        R.string.appendSpace, widthNormalizer(
                            mesra1,
                            poemPagerFragment.mesraWidth[item.verseOrder - 1]!!,
                            poemPagerFragment.mesraMaxWidth,
                            stvm
                        )
                    )

                    searchBackSpanIndex = normalizedText.findSpanIndex3(poemViewModel.searchQuery)
                    spanIndex = normalizedText.findSpanIndex2(splittedQuery)
                    hilightBackSpanIndex = mutableListOf()
                    val verseHilight = item.hilight?.substringBefore('¶').let{
                        if (it.isNullOrEmpty()) null else it
                    }

                    item.hilight?.let {
                        val hilightSpanIndexList = it.substringBefore('¶').split(',')
                        if (hilightSpanIndexList.size > 1)
                            hilightBackSpanIndex = hilightSpanIndexList
                                .mapIndexed { index, s -> if ((index+1) % 3 == 0) s.toInt() else
                                    indexInNormalizedFinder(mesra1, normalizedText, s.toInt()) }
                    }
                    selectSpanNumber =
                        if (poemViewModel.resultVersePositions.isNotEmpty() &&
                            poemViewModel.resultVersePositions[poemViewModel.positionPointer][0] == item.verseOrder)
                            poemViewModel.resultVersePositions[poemViewModel.positionPointer][1]
                        else
                            null

                    layoutParams = params.apply { width = poemPagerFragment.mesraMaxWidth +
                            2 * poemPagerFragment.resources.getDimensionPixelSize(R.dimen.verse_padding) }
                    text =  normalizedText

                    setOnLongClickListener {
                        poemPagerFragment.deselectText()
                        hasSel = true
                        myOnSelectionChanged(selStart, selEnd)
                        invalidate()
                        poemViewModel.apply {
                            textMenuVisible = true
                            textHilight = verseHilight
                            textVerseOrder = item.verseOrder
                            textNoteVerseOrder = item.verseOrder
                            reportEvent(PoemEvent.OnRefreshTextMenu)
                        }
                        true
                    }

                }
                binding.mesra2Text.apply {
                    visibility = View.VISIBLE

                    val params = layoutParams as ConstraintLayout.LayoutParams
                    if (poemPagerFragment.beitOneLine)
                        params.startToEnd = R.id.guid_view
                    else{
                        params.startToStart = R.id.guid_view
                        params.updateMargins(top = (stvm.textHeight * stvm.verseVertSep).toInt())
                    }

                    val normalizedText = poemPagerFragment.getString(
                        R.string.prependSpace, widthNormalizer(
                            mesra2,
                            poemPagerFragment.mesraWidth[item.verseOrder]!!,
                            poemPagerFragment.mesraMaxWidth,
                            stvm
                        )
                    )

                    searchBackSpanIndex = normalizedText.findSpanIndex3(poemViewModel.searchQuery)
                    spanIndex = normalizedText.findSpanIndex2(splittedQuery)
                    hilightBackSpanIndex = mutableListOf()
                    val verseHilight = item.hilight?.substringAfter('¶').let{
                        if (it.isNullOrEmpty()) null else it
                    }

                    item.hilight?.let {
                        val hilightSpanIndexList = it.substringAfter('¶').split(',')
                        if (hilightSpanIndexList.size > 1)
                            hilightBackSpanIndex = hilightSpanIndexList
                                .mapIndexed { index, s -> if ((index+1) % 3 == 0) s.toInt() else
                                    indexInNormalizedFinder(mesra2, normalizedText, s.toInt()) }
                    }
                    selectSpanNumber =
                        if (poemViewModel.resultVersePositions.isNotEmpty() &&
                            poemViewModel.resultVersePositions[poemViewModel.positionPointer][0] == item.verseOrder+1)
                            poemViewModel.resultVersePositions[poemViewModel.positionPointer][1]
                        else
                            null

                    layoutParams = params.apply { width = poemPagerFragment.mesraMaxWidth +
                            2 * poemPagerFragment.resources.getDimensionPixelSize(R.dimen.verse_padding) }
                    text =  normalizedText

                    setOnLongClickListener {
                        poemPagerFragment.deselectText()
                        hasSel = true
                        myOnSelectionChanged(selStart, selEnd)
                        invalidate()
                        poemViewModel.apply {
                            textMenuVisible = true
                            textHilight = verseHilight
                            textVerseOrder = item.verseOrder + 1
                            textNoteVerseOrder = item.verseOrder
                            reportEvent(PoemEvent.OnRefreshTextMenu)
                        }
                        true
                    }
                }
            } else if (item.text!!.contains('¶') && item.position == 2){
                val mesra1 = item.text!!.substringBefore("¶").trim()
                val mesra2 = item.text!!.substringAfter("¶").trim()
                val mesra1Normalized = widthNormalizer(
                    inString = mesra1,
                    initialWidth = poemPagerFragment.mesraWidth[item.verseOrder - 1]!!,
                    finalWidth = poemPagerFragment.mesraMaxWidth,
                    stvm = stvm
                )
                val mesra2Normalized = widthNormalizer(
                    inString = mesra2,
                    initialWidth = poemPagerFragment.mesraWidth[item.verseOrder]!!,
                    finalWidth = poemPagerFragment.mesraMaxWidth,
                    stvm = stvm
                )

                binding.mesra1Text.visibility = View.GONE
                binding.mesra2Text.visibility = View.GONE
                binding.paragText.apply {
                    visibility = View.VISIBLE
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER

                    setLineSpacing(6.dpTOpx(resources), 1f)

                    val beit = poemPagerFragment.getString(R.string.beit2, mesra1, mesra2)
                    val beitNormalized = poemPagerFragment.getString(
                        R.string.beit2, mesra1Normalized, mesra2Normalized
                    )

//                    post {
                    searchBackSpanIndex = beitNormalized.findSpanIndex3(poemViewModel.searchQuery)
                    spanIndex = beitNormalized.findSpanIndex2(splittedQuery)
                    hilightBackSpanIndex = mutableListOf()

                    item.hilight?.let {
                        val hilightSpanIndexList1 = it.split(',')
                        if (hilightSpanIndexList1.size > 1)
                            hilightBackSpanIndex = hilightSpanIndexList1
                                .mapIndexed { index, s -> if ((index+1) % 3 == 0) s.toInt() else
                                    indexInNormalizedFinder(beit, beitNormalized, s.toInt()) }.toMutableList()
                    }

                    selectSpanNumber =
                        if (poemViewModel.resultVersePositions.isNotEmpty() &&
                            poemViewModel.resultVersePositions[poemViewModel.positionPointer][0] == item.verseOrder)
                            poemViewModel.resultVersePositions[poemViewModel.positionPointer][1]
                        else
                            null

                    text = beitNormalized

                    setOnLongClickListener {
                        poemPagerFragment.deselectText()
                        hasSel = true
                        myOnSelectionChanged(selStart, selEnd)
                        invalidate()
                        poemViewModel.apply {
                            textMenuVisible = true
                            textHilight = item.hilight
                            textVerseOrder = item.verseOrder
                            textNoteVerseOrder = item.verseOrder
                            reportEvent(PoemEvent.OnRefreshTextMenu)
                        }
                        true
                    }
//                    }
                }
            } else {
                binding.mesra1Text.visibility = View.GONE
                binding.mesra2Text.visibility = View.GONE
                binding.paragText.apply {
                    visibility = View.VISIBLE
//                    @RequiresApi(Build.VERSION_CODES.O)
//                    justificationMode = JUSTIFICATION_MODE_INTER_WORD

//                    textAlignment =  TextView.TEXT_ALIGNMENT_VIEW_START
                    textAlignment = if (item.position == 2 || item.position == 3) TextView.TEXT_ALIGNMENT_CENTER
                    else TextView.TEXT_ALIGNMENT_VIEW_START

                    val paragText = poemPagerFragment.getString(R.string.tabbedText, item.text)
                    val staticLayout = StaticLayout.Builder.obtain(
                        paragText, 0, paragText.length, paint,
                        poemPagerFragment.paragraphWidth - (stvm.mWidth).toInt()
                    )
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(
                            if (stvm.fontPref == 0) 0f else 12.dpTOpx(resources), 1f)
                        .build()

                    val normalizedLine = Array<String?>(staticLayout.lineCount){null}
                    for (i in 0 until staticLayout.lineCount) {
                        val lineStart = staticLayout.getLineStart(i)
                        val lineEnd = staticLayout.getLineEnd(i)
                        val line = paragText.substring(lineStart, lineEnd).trim(' ', '‌')
                        normalizedLine[i] = if (i < staticLayout.lineCount - 1 && line.isNotEmpty()
                            && line.last() != '\n')
                            widthNormalizer(line, paint.measureText(line).toInt(),
                                poemPagerFragment.paragraphWidth - (stvm.mWidth).toInt(), stvm)
                        else line
                    }
                    val normalizedText = normalizedLine.joinToString(" ")
                        .replace("\n ", "\n")
                    height = staticLayout.height + paddingTop + paddingBottom

                    post{
                        searchBackSpanIndex = normalizedText.findSpanIndex3(poemViewModel.searchQuery)
                        spanIndex = normalizedText.findSpanIndex2(splittedQuery)
                        hilightBackSpanIndex = mutableListOf()

                        item.hilight?.let {
                            val hilightSpanIndexList = it.split(',')
                            if (hilightSpanIndexList.size > 1)
                                hilightBackSpanIndex = hilightSpanIndexList
                                    .mapIndexed { index, s -> if ((index+1) % 3 == 0) s.toInt() else
                                        indexInNormalizedFinder(
                                            paragText,
                                            normalizedText,
                                            s.toInt()
                                        )
                                    }.toMutableList()
                        }

                        selectSpanNumber =
                            if (poemViewModel.resultVersePositions.isNotEmpty() &&
                                poemViewModel.resultVersePositions[poemViewModel.positionPointer][0] == item.verseOrder)
                                poemViewModel.resultVersePositions[poemViewModel.positionPointer][1]
                            else
                                null

                        text = normalizedText
//                        doOnPreDraw { height = layout.height + paddingTop + paddingBottom }
                    }

                    setOnLongClickListener {
                        poemPagerFragment.deselectText()
                        hasSel = true
                        myOnSelectionChanged(selStart, selEnd)
                        invalidate()
                        poemViewModel.apply {
                            textMenuVisible = true
                            textHilight = item.hilight
                            textVerseOrder = item.verseOrder
                            textNoteVerseOrder = item.verseOrder
                            reportEvent(PoemEvent.OnRefreshTextMenu)
                        }
                        true
                    }
                }
            }
        }

        fun removeWatcher(){
            binding.commentText.removeTextChangedListener(commentWatcher)
        }

        companion object {
            fun from(parent: ViewGroup, poemPagerFragment: PoemPagerFragment): PoemContentViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = PoemItemBinding.inflate(layoutInflater, parent, false)
                binding.lifecycleOwner = poemPagerFragment.viewLifecycleOwner
                return PoemContentViewHolder(binding, poemPagerFragment)
            }
        }
    }
}

class PoemDiffCallback : DiffUtil.ItemCallback<Verse>(){
    override fun areItemsTheSame(oldItem: Verse, newItem: Verse): Boolean {
        return oldItem.verseOrder == newItem.verseOrder
    }

    override fun areContentsTheSame(oldItem: Verse, newItem: Verse): Boolean {
        return oldItem == newItem
    }
}


