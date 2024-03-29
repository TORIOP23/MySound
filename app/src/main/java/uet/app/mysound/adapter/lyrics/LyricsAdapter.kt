package uet.app.mysound.adapter.lyrics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uet.app.mysound.data.model.metadata.Line
import uet.app.mysound.data.model.metadata.Lyrics
import uet.app.mysound.databinding.ItemLyricsActiveBinding
import uet.app.mysound.databinding.ItemLyricsNormalBinding

class LyricsAdapter(private var originalLyrics: Lyrics?, var translated: Lyrics? = null): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var activeLyrics: Line? = null

    fun updateOriginalLyrics(lyrics: Lyrics) {
        if (lyrics != originalLyrics) {
            originalLyrics = lyrics
            translated = null
            notifyDataSetChanged()
        }
    }
    fun updateTranslatedLyrics(lyrics: Lyrics?) {
        if (lyrics != null) {
            translated = lyrics
            notifyDataSetChanged()
        }
    }

    fun setActiveLyrics(index: Int) {
        if (index == -1) {
            if (activeLyrics != null) {
                activeLyrics = null
                notifyDataSetChanged()
            }
        }
        else {
            if (originalLyrics?.lines?.get(index) != activeLyrics) {
                activeLyrics = originalLyrics?.lines?.get(index)
                notifyItemChanged(index)
                if (index > 0) {
                    notifyItemChanged(index - 1)
                }
            }
        }
    }

    inner class ActiveViewHolder(val binding: ItemLyricsActiveBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(line: Line) {
            binding.tvNowLyrics.text = line.words
            if (translated != null && translated?.lines?.find { it.startTimeMs == line.startTimeMs }?.words != null) {
                translated?.lines?.find { it.startTimeMs == line.startTimeMs }?.words?.let {
                    binding.tvTranslatedLyrics.visibility = View.VISIBLE
                    binding.tvTranslatedLyrics.text = it
                }
            }
            else {
                binding.tvTranslatedLyrics.visibility = View.GONE
            }
        }
    }
    inner class NormalViewHolder(val binding: ItemLyricsNormalBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(line: Line) {
            binding.tvLyrics.text = line.words
            if (translated != null && translated?.lines?.find { it.startTimeMs == line.startTimeMs }?.words != null) {
                translated?.lines?.find { it.startTimeMs == line.startTimeMs }?.words?.let {
                    binding.tvTranslatedLyrics.visibility = View.VISIBLE
                    binding.tvTranslatedLyrics.text = it
                }
            }
            else {
                binding.tvTranslatedLyrics.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ACTIVE -> ActiveViewHolder(ItemLyricsActiveBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> NormalViewHolder(ItemLyricsNormalBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun getItemCount(): Int {
        return originalLyrics?.lines?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ActiveViewHolder -> {
                holder.bind(originalLyrics?.lines?.get(position) ?: return)
            }
            is NormalViewHolder -> {
                holder.bind(originalLyrics?.lines?.get(position) ?: return)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (originalLyrics?.lines?.get(position) == activeLyrics) {
                TYPE_ACTIVE
            }
            else {
                TYPE_NORMAL
            }
        }

    companion object {
        const val TYPE_ACTIVE = 0
        const val TYPE_NORMAL = 1
    }
}