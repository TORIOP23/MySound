package uet.app.mysound.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uet.app.mysound.R
import uet.app.mysound.databinding.ActivityMainBinding
import uet.app.mysound.service.SimpleMediaService
import uet.app.mysound.ui.fragment.NowPlayingFragment
import uet.app.mysound.utils.Resource
import uet.app.mysound.viewModel.SharedViewModel
import uet.app.mysound.viewModel.UIEvent

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NowPlayingFragment.OnNowPlayingSongChangeListener {
    private lateinit var binding: ActivityMainBinding // View binding
    private val viewModel by viewModels<SharedViewModel>() // Delegate initialize SharedViewModel

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view) // Set view activity_main

        // Now_playing card
        binding.card.visibility = View.GONE

        // Set up navigation component
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
        val navController = navHostFragment?.findNavController()
        binding.bottomNavigationView.setupWithNavController(navController!!)

        // Show nowPlayingFragment when click
        binding.card.setOnClickListener {
            val nowPlayingFragment = NowPlayingFragment()
            nowPlayingFragment.show(supportFragmentManager, "NowPlayingFragment")
        }


        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }
        lifecycleScope.launch {
            val job1 = launch {
                viewModel.metadata.observe(this@MainActivity) {
                    if (it is Resource.Success) {
                        binding.card.visibility = View.VISIBLE
                        startService()
                    } else {
                        binding.card.visibility = View.GONE
                    }
                }
            }
            val job2 = launch {
                viewModel.progress.collect {
                    binding.progressBar.progress = (it * 100).toInt()
                }
            }
            job1.join()
            job2.join()
        }
        binding.card.animation = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Service", "Service destroyed")
    }

    private fun startService() {
        if (viewModel.isServiceRunning.value == false) {
            val intent = Intent(this, SimpleMediaService::class.java)
            startForegroundService(intent)
            viewModel.isServiceRunning.postValue(true)
            Log.d("Service", "Service started")
        }
    }

    private fun stopService() {
        if (viewModel.isServiceRunning.value == true) {
            val intent = Intent(this, SimpleMediaService::class.java)
            stopService(intent)
            viewModel.isServiceRunning.postValue(false)
            Log.d("Service", "Service stopped")
        }
    }

    override fun onNowPlayingSongChange() {
        viewModel.metadata.observe(this, Observer {
            when (it) {
                is Resource.Success -> {
                    binding.songTitle.text = it.data?.title
                    binding.songTitle.isSelected = true
                    var artistName = ""
                    if (it.data?.artists != null) {
                        for (artist in it.data.artists) {
                            artistName += artist.name + ", "
                        }
                    }
                    artistName = removeTrailingComma(artistName)
                    artistName = removeComma(artistName)
                    binding.songArtist.text = artistName
                    binding.songArtist.isSelected = true
                    viewModel.lyricsBackground.value?.let { it1 ->
                        binding.card.setCardBackgroundColor(
                            it1
                        )
                    }
                    binding.ivArt.load(it.data?.thumbnails?.get(0)?.url)
                }

                is Resource.Loading -> {

                }

                is Resource.Error -> {

                }

                else -> {}
            }
        })
    }

    override fun onIsPlayingChange() {
        viewModel.isPlaying.observe(this) {
            if (it) {
                binding.btPlayPause.setImageResource(R.drawable.baseline_pause_24)
            } else {
                binding.btPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)
            }
        }
    }

    override fun onUpdateProgressBar(progress: Float) {

    }

    private fun removeTrailingComma(sentence: String): String {
        val trimmed = sentence.trimEnd()
        return if (trimmed.endsWith(", ")) {
            trimmed.dropLast(2)
        } else {
            trimmed
        }
    }

    private fun removeComma(string: String): String {
        return if (string.endsWith(',')) {
            string.substring(0, string.length - 1)
        } else {
            string
        }
    }
}