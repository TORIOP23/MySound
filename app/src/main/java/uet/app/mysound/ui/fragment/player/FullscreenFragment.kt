package uet.app.mysound.ui.fragment.player

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.daimajia.swipe.SwipeLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uet.app.mysound.R
import uet.app.mysound.databinding.BottomSheetFullscreenBinding
import uet.app.mysound.extension.setEnabledAll
import uet.app.mysound.service.RepeatState
import uet.app.mysound.viewModel.SharedViewModel
import uet.app.mysound.viewModel.UIEvent

@AndroidEntryPoint
class FullscreenFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
    private val binding by lazy { BottomSheetFullscreenBinding.inflate(layoutInflater) }
    var job: Job? = null
    private var showJob: Job? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.window?.navigationBarColor = Color.TRANSPARENT
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val miniplayer = activity.findViewById<SwipeLayout>(R.id.miniplayer)
        bottom.visibility = View.GONE
        miniplayer.visibility = View.GONE
        hideSystemUI()
        if (binding.playerView.player == null) {
            binding.playerView.player = viewModel.simpleMediaServiceHandler?.player
        }

        with(binding) {
            overlayLayout.visibility = View.VISIBLE
            showJob = lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    delay(3000)
                    overlayLayout.visibility = View.GONE
                }
            }
            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            btOutFullScreen.setOnClickListener {
                findNavController().popBackStack()
            }
            btNext.setOnClickListener {
                viewModel.onUIEvent(UIEvent.Next)
            }
            btPrevious.setOnClickListener {
                viewModel.onUIEvent(UIEvent.Previous)
            }
            btPlayPause.setOnClickListener {
                viewModel.onUIEvent(UIEvent.PlayPause)
            }
            binding.btShuffle.setOnClickListener {
                viewModel.onUIEvent(UIEvent.Shuffle)
            }
            binding.btRepeat.setOnClickListener {
                viewModel.onUIEvent(UIEvent.Repeat)
            }
            rootLayout.setOnClickListener {
                if (overlayLayout.visibility == View.VISIBLE) {
                    showJob?.cancel()
                    overlayLayout.visibility = View.GONE
                } else {
                    val shortAnimationDuration =
                        resources.getInteger(android.R.integer.config_mediumAnimTime)
                    overlayLayout.alpha = 0f
                    overlayLayout.apply {
                        visibility = View.VISIBLE
                        animate()
                            .alpha(1f)
                            .setDuration(shortAnimationDuration.toLong())
                            .setListener(null)
                    }
                    showJob = lifecycleScope.launch {
                        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                            delay(3000)
                            overlayLayout.visibility = View.GONE
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val time = launch {
                    viewModel.progress.collect { prog ->
                        binding.progressSong.value = prog * 100
                    }
                }
                val timeString = launch {
                    viewModel.progressString.collect { prog ->
                        binding.tvCurrentTime.text = prog
                    }
                }
                val duration = launch {
                    viewModel.duration.collect { dur ->
                        binding.tvFullTime.text = viewModel.formatDuration(dur)
                    }
                }
                val isPlaying = launch {
                    viewModel.isPlaying.collect { isPlaying ->
                        if (isPlaying) {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_pause_circle_24)
                        } else {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_play_circle_24)
                        }
                    }
                }
                val title = launch {
                    viewModel.simpleMediaServiceHandler?.nowPlaying?.collectLatest {
                        if (it != null) {
                            binding.toolbar.title = it.mediaMetadata.title.toString()
                        }
                    }
                }
                val shuffle = launch {
                    viewModel.shuffleModeEnabled.collect { shuffle ->
                        when (shuffle) {
                            true -> {
                                binding.btShuffle.setImageResource(R.drawable.baseline_shuffle_24_enable)
                            }

                            false -> {
                                binding.btShuffle.setImageResource(R.drawable.baseline_shuffle_24)
                            }
                        }
                    }
                }
                val repeat = launch {
                    viewModel.repeatMode.collect { repeatMode ->
                        when (repeatMode) {
                            RepeatState.None -> {
                                binding.btRepeat.setImageResource(R.drawable.baseline_repeat_24)
                            }

                            RepeatState.One -> {
                                binding.btRepeat.setImageResource(R.drawable.baseline_repeat_one_24)
                            }

                            RepeatState.All -> {
                                binding.btRepeat.setImageResource(R.drawable.baseline_repeat_24_enable)
                            }
                        }
                    }
                }
                val job11 = launch {
                    viewModel.simpleMediaServiceHandler?.previousTrackAvailable?.collect { available ->
                        setEnabledAll(binding.btPrevious, available)
                    }
                }
                val job12 = launch {
                    viewModel.simpleMediaServiceHandler?.nextTrackAvailable?.collect { available ->
                        setEnabledAll(binding.btNext, available)
                    }
                }
                time.join()
                timeString.join()
                duration.join()
                isPlaying.join()
                title.join()
                repeat.join()
                job11.join()
                job12.join()
                shuffle.join()
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(
            requireActivity().window,
            binding.root
        ).show(WindowInsetsCompat.Type.systemBars())
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.player = null
        showSystemUI()
    }
}