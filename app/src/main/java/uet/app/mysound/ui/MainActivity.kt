package uet.app.mysound.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.switchMap
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.palette.graphics.Palette
import coil.load
import coil.size.Size
import coil.transform.Transformation
import com.daimajia.swipe.SwipeLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pub.devrel.easypermissions.EasyPermissions
import uet.app.mysound.R
import uet.app.mysound.common.Config
import uet.app.mysound.common.FIRST_TIME_MIGRATION
import uet.app.mysound.common.SELECTED_LANGUAGE
import uet.app.mysound.common.SPONSOR_BLOCK
import uet.app.mysound.common.STATUS_DONE
import uet.app.mysound.common.SUPPORTED_LANGUAGE
import uet.app.mysound.common.SUPPORTED_LOCATION
import uet.app.mysound.data.dataStore.DataStoreManager
import uet.app.mysound.data.dataStore.DataStoreManager.Settings.RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE
import uet.app.mysound.data.model.browse.album.Track
import uet.app.mysound.data.queue.Queue
import uet.app.mysound.data.repository.MainRepository
import uet.app.mysound.databinding.ActivityMainBinding
import uet.app.mysound.extension.isMyServiceRunning
import uet.app.mysound.extension.navigateSafe
import uet.app.mysound.extension.setTextAnimation
import uet.app.mysound.myAPI.AppData
import uet.app.mysound.myAPI.User.LoginResponse
import uet.app.mysound.service.SimpleMediaService
import uet.app.mysound.service.SimpleMediaServiceHandler
import uet.app.mysound.viewModel.SharedViewModel
import uet.app.mysound.viewModel.UIEvent
import uet.app.youtubeExtractor.YouTube
import uet.app.youtubeExtractor.models.YouTubeLocale
import java.util.Locale
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        var loginResponse: LoginResponse? = null
    }
    private lateinit var binding: ActivityMainBinding // ViewBinding
    val viewModel by viewModels<SharedViewModel>() // ViewModel
    private var action: String? = null // Action from intent
    private var data: Uri? = null // Data from intent

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var mainRepository: MainRepository

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is SimpleMediaService.MusicBinder) {
                Log.w("MainActivity", "onServiceConnected: ")

                viewModel.simpleMediaServiceHandler = SimpleMediaServiceHandler(
                    player = service.service.player,
                    mediaSession = service.service.mediaSession,
                    mediaSessionCallback = service.service.simpleMediaSessionCallback,
                    dataStoreManager = dataStoreManager,
                    mainRepository = mainRepository,
                    context = service.service,
                    coroutineScope = lifecycleScope
                )

                viewModel.init()
                mayBeRestoreLastPlayedTrackAndQueue()
                runCollect()
                Log.w("TEST", viewModel.simpleMediaServiceHandler?.player.toString())
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w("MainActivity", "onServiceDisconnected: ")
            viewModel.simpleMediaServiceHandler = null
        }
    }

    private fun runCollect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job5 = launch {
                    viewModel.simpleMediaServiceHandler?.nowPlaying?.collect {
                        if (it != null) {
                            Log.w(
                                "Test service",
                                viewModel.simpleMediaServiceHandler?.getCurrentMediaItem()?.mediaMetadata?.title.toString()
                            )
                            binding.songTitle.setTextAnimation(it.mediaMetadata.title.toString())
                            binding.songTitle.isSelected = true
                            binding.songArtist.setTextAnimation(it.mediaMetadata.artist.toString())
                            binding.songArtist.isSelected = true
                            binding.ivArt.load(it.mediaMetadata.artworkUri) {
                                crossfade(true)
                                crossfade(300)
                                placeholder(R.drawable.outline_album_24)
                                transformations(object : Transformation {
                                    override val cacheKey: String
                                        get() = it.mediaMetadata.artworkUri.toString()

                                    override suspend fun transform(
                                        input: Bitmap,
                                        size: Size
                                    ): Bitmap {
                                        val p = Palette.from(input).generate()
                                        val defaultColor = 0x000000
                                        var startColor = p.getDarkVibrantColor(defaultColor)
                                        Log.d("Check Start Color", "transform: $startColor")
                                        if (startColor == defaultColor) {
                                            startColor = p.getDarkMutedColor(defaultColor)
                                            if (startColor == defaultColor){
                                                startColor = p.getVibrantColor(defaultColor)
                                                if (startColor == defaultColor){
                                                    startColor = p.getMutedColor(defaultColor)
                                                    if (startColor == defaultColor){
                                                        startColor = p.getLightVibrantColor(defaultColor)
                                                        if (startColor == defaultColor){
                                                            startColor = p.getLightMutedColor(defaultColor)
                                                        }
                                                    }
                                                }
                                            }
                                            Log.d("Check Start Color", "transform: $startColor")
                                        }
                                        val endColor = 0x1b1a1f
                                        val gd = GradientDrawable(
                                            GradientDrawable.Orientation.TOP_BOTTOM,
                                            intArrayOf(startColor, endColor)
                                        )
                                        gd.cornerRadius = 0f
                                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                                        gd.gradientRadius = 0.5f
                                        gd.alpha = 150
                                        val bg = ColorUtils.setAlphaComponent(startColor, 255)
                                        binding.card.setCardBackgroundColor(bg)
                                        binding.cardBottom.setCardBackgroundColor(bg)
                                        return input
                                    }

                                })
                            }
                        }
                    }
                }
                val job2 = launch {
                    viewModel.progress.collect{
                        binding.progressBar.progress = (it * 100).toInt()
                    }
                }

                val job6 = launch {
                    viewModel.simpleMediaServiceHandler?.liked?.collect{ liked ->
                        binding.cbFavorite.isChecked = liked
                    }
                }
                val job3 = launch {
                    viewModel.isPlaying.collect {
                        if (it){
                            binding.btPlayPause.setImageResource(R.drawable.baseline_pause_24)
                        }else{
                            binding.btPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)
                        }
                    }
                }
                val job4 = launch {
                    viewModel.simpleMediaServiceHandler?.sleepDone?.collect { done ->
                        if (done) {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle(getString(R.string.sleep_timer_off))
                                .setMessage(getString(R.string.good_night))
                                .setPositiveButton(getString(R.string.yes)) { d, _ ->
                                    d.dismiss()
                                }
                                .show()
                        }
                    }
                }
                job2.join()
                job3.join()
                job5.join()
                job6.join()
                job4.join()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        action = intent?.action
        data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
        Log.d("MainActivity", "onNewIntent: $data")
        viewModel.intent.value = intent
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume: ")
        loginResponse = AppData.getInstance().loginResponse
        if (loginResponse != null) {
            val token = loginResponse!!.token
            val audioToken = loginResponse!!.audioToken
            Log.i("TAG", "TOKEN_124IU_ $token")
        }
        else {
            Log.i("TAG", "TOKEN_124IU_ NULL")
        }
    }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (viewModel.simpleMediaServiceHandler == null) {
//            startMusicService()
//        }
        if (viewModel.recreateActivity.value == true) {
            viewModel.simpleMediaServiceHandler?.coroutineScope = lifecycleScope
            runCollect()
        }

        startMusicService()
        Log.d("MainActivity", "onCreate: ")
        action = intent.action
        data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
        if (data != null) {
            viewModel.intent.value = intent
        }

        viewModel.checkIsRestoring()
        Log.d("Italy", "Key: ${Locale.ITALY.toLanguageTag()}")

        // Check if the migration has already been done or not
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            Log.d("Locale Key", "onCreate: ${Locale.getDefault().toLanguageTag()}")
            if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                Log.d("Contains", "onCreate: ${SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())}")
                putString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
                if (SUPPORTED_LOCATION.items.contains(Locale.getDefault().country)) {
                    putString("location", Locale.getDefault().country)
                }
                else {
                    putString("location", "US")
                }
                YouTube.locale = YouTubeLocale(
                    gl = getString("location") ?: "US",
                    hl = Locale.getDefault().toLanguageTag()
                )
            } else {
                putString(SELECTED_LANGUAGE, "en-US")
                YouTube.locale = YouTubeLocale(
                    gl = getString("location") ?: "US",
                    hl = "en-US"
                )
            }
            // Fetch the selected language from wherever it was stored. In this case its SharedPref
            getString(SELECTED_LANGUAGE)?.let {
                Log.d("Locale Key", "getString: $it")
                // Set this locale using the AndroidX library that will handle the storage itself
                val localeList = LocaleListCompat.forLanguageTags(it)
                AppCompatDelegate.setApplicationLocales(localeList)
                // Set the migration flag to ensure that this is executed only once
                putString(FIRST_TIME_MIGRATION, STATUS_DONE)
            }
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != getString(SELECTED_LANGUAGE)) {
            Log.d("Locale Key", "onCreate: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}")
            putString(SELECTED_LANGUAGE, AppCompatDelegate.getApplicationLocales().toLanguageTags())
            YouTube.locale = YouTubeLocale(
                gl = getString("location") ?: "US",
                hl = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            )
        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
//        } else {
//            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//        }

        if (!EasyPermissions.hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                EasyPermissions.requestPermissions(this,
                    getString(R.string.this_app_needs_to_access_your_notification), 1, Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        viewModel.getLocation()
        viewModel.checkAuth()
        viewModel.checkAllDownloadingSongs()
        runBlocking { delay(500) }

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.bottomNavigationView.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }
        window.navigationBarColor = Color.parseColor("#CB0B0A0A")
        if (!isMyServiceRunning(SimpleMediaService::class.java)) {
            binding.miniplayer.visibility = View.GONE
        }
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
        val navController = navHostFragment?.findNavController()
        binding.bottomNavigationView.setupWithNavController(navController!!)
        when (action) {
            "uet.app.mysound.action.HOME" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_home
            }
            "uet.app.mysound.action.SEARCH" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_search
            }
            "uet.app.mysound.action.LIBRARY" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_library
            }
            else -> {}
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.bottom_navigation_item_home, R.id.settingsFragment, R.id.recentlySongsFragment -> {
                    binding.bottomNavigationView.menu.findItem(R.id.bottom_navigation_item_home)?.isChecked =
                        true
                }

                R.id.bottom_navigation_item_search -> {
                    binding.bottomNavigationView.menu.findItem(R.id.bottom_navigation_item_search)?.isChecked =
                        true
                }

                R.id.bottom_navigation_item_library, R.id.downloadedFragment, R.id.mostPlayedFragment, R.id.followedFragment, R.id.favoriteFragment -> {
                    binding.bottomNavigationView.menu.findItem(R.id.bottom_navigation_item_library)?.isChecked =
                        true
                }

                else -> {}
            }
        }

        binding.miniplayer.showMode = SwipeLayout.ShowMode.PullOut
        binding.miniplayer.addDrag(SwipeLayout.DragEdge.Right, binding.llBottom)
        binding.miniplayer.addSwipeListener(object : SwipeLayout.SwipeListener {
            override fun onStartOpen(layout: SwipeLayout?) {
                binding.card.radius = 0f
            }

            override fun onOpen(layout: SwipeLayout?) {
                binding.card.radius = 0f
            }

            override fun onStartClose(layout: SwipeLayout?) {
            }

            override fun onClose(layout: SwipeLayout?) {
                binding.card.radius = 8f
            }

            override fun onUpdate(layout: SwipeLayout?, leftOffset: Int, topOffset: Int) {
            }

            override fun onHandRelease(layout: SwipeLayout?, xvel: Float, yvel: Float) {
            }

        })
        binding.btRemoveMiniPlayer.setOnClickListener {
            viewModel.stopPlayer()
            viewModel.isServiceRunning.postValue(false)
            viewModel.videoId.postValue(null)
            binding.miniplayer.visibility = View.GONE
            binding.card.radius = 8f
        }

        binding.card.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("type", Config.MINIPLAYER_CLICK)
            navController.navigateSafe(R.id.action_global_nowPlayingFragment, bundle)
        }
        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job1 = launch {
                    viewModel.intent.collectLatest { intent ->
                        if (intent != null){
                            data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
                            Log.d("MainActivity", "onCreate: $data")
                            if (data != null) {
                                when (val path = data!!.pathSegments.firstOrNull()) {
                                    "playlist" -> data!!.getQueryParameter("list")?.let { playlistId ->
                                        if (playlistId.startsWith("OLAK5uy_")) {
                                            viewModel.intent.value = null
                                            navController.navigateSafe(R.id.action_global_albumFragment, Bundle().apply {
                                                putString("browseId", playlistId)
                                            })
                                        }
                                        else if (playlistId.startsWith("VL")) {
                                            viewModel.intent.value = null
                                            navController.navigateSafe(R.id.action_global_playlistFragment, Bundle().apply {
                                                putString("id", playlistId)
                                            })
                                        }
                                        else {
                                            viewModel.intent.value = null
                                            navController.navigateSafe(R.id.action_global_playlistFragment, Bundle().apply {
                                                putString("id", "VL$playlistId")
                                            })
                                        }
                                    }

                                    "channel", "c" -> data!!.lastPathSegment?.let { artistId ->
                                        if (artistId.startsWith("UC")) {
                                            viewModel.intent.value = null
                                            navController.navigateSafe(R.id.action_global_artistFragment, Bundle().apply {
                                                putString("channelId", artistId)
                                            })
                                        }
                                        else {
                                            Toast.makeText(this@MainActivity,
                                                getString(R.string.this_link_is_not_supported), Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    else -> when {
                                        path == "watch" -> data!!.getQueryParameter("v")
                                        data!!.host == "youtu.be" -> path
                                        else -> null
                                    }?.let { videoId ->
                                        val args = Bundle()
                                        args.putString("videoId", videoId)
                                        args.putString("from", getString(R.string.shared))
                                        args.putString("type", Config.SHARE)
                                        viewModel.videoId.value = videoId
                                        hideBottomNav()
                                        if (navController.currentDestination?.id == R.id.nowPlayingFragment) {
                                            findNavController(R.id.fragment_container_view).popBackStack()
                                            navController.navigateSafe(R.id.action_global_nowPlayingFragment, args)
                                        }
                                        else {
                                            navController.navigateSafe(R.id.action_global_nowPlayingFragment, args)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                job1.join()
            }
        }
        lifecycleScope.launch {
            val job1 = launch {
                viewModel.progress.collect { progress ->
                    val skipSegments = viewModel.skipSegments.first()
                    val enabled = viewModel.sponsorBlockEnabled()
                    val listCategory = viewModel.sponsorBlockCategories()
                    if (skipSegments != null && enabled == DataStoreManager.TRUE) {
                        for (skip in skipSegments) {
                            if (listCategory.contains(skip.category)) {
                                val firstPart = (skip.segment[0]/skip.videoDuration).toFloat()
                                val secondPart = (skip.segment[1]/skip.videoDuration).toFloat()
                                if (progress in firstPart..secondPart) {
                                    Log.w("Seek to", (skip.segment[1]/skip.videoDuration).toFloat().toString())
                                    viewModel.skipSegment((skip.segment[1] * 1000).toLong())
                                    Toast.makeText(this@MainActivity,
                                        getString(R.string.sponsorblock_skip_segment, getString(SPONSOR_BLOCK.listName.get(SPONSOR_BLOCK.list.indexOf(skip.category))).lowercase()), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }


            job1.join()
        }
        binding.card.animation = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top)
        binding.cbFavorite.setOnCheckedChangeListener{ _, isChecked ->
            if (!isChecked){
                Log.d("cbFavorite", "onCheckedChanged: $isChecked")
                viewModel.nowPlayingMediaItem.value?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(nowPlayingSong.mediaId, false)
                    viewModel.updateLikeInNotification(false)
                }
            }
            else {
                Log.d("cbFavorite", "onCheckedChanged: $isChecked")
                viewModel.nowPlayingMediaItem.value?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(nowPlayingSong.mediaId, true)
                    viewModel.updateLikeInNotification(true)
                }
            }
        }
    }

    private fun mayBeRestoreLastPlayedTrackAndQueue() {
        if (getString(RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE) == DataStoreManager.FALSE) {
            Log.d("Restore", "mayBeRestoreLastPlayedTrackAndQueue: ")
            viewModel.getSaveLastPlayedSong()
            val queue = viewModel.saveLastPlayedSong.switchMap { saved: Boolean ->
                if (saved) {
                    viewModel.from.postValue(viewModel.from_backup)
                    viewModel.simpleMediaServiceHandler?.reset()
                    viewModel.getSavedSongAndQueue()
                    return@switchMap viewModel.savedQueue
                } else {
                    return@switchMap null
                }
            }
            val result: MediatorLiveData<List<Track>> = MediatorLiveData<List<Track>>().apply {
                addSource(queue) {
                    value = it ?: listOf()
                }
            }
            binding.miniplayer.visibility = View.GONE
            result.observe(this) {data ->
                val queueData = data
                Log.w("Check queue saved", queueData.toString())
                binding.miniplayer.visibility = View.VISIBLE
                if (queueData.isNotEmpty()) {
                    Log.w("Check queue saved", queueData.toString())
                    Queue.clear()
                    Queue.addAll(queueData)
                    viewModel.removeSaveQueue()
                    viewModel.resetRelated()
                    viewModel.addQueueToPlayer()
                }
            }
        }
        else {
            binding.miniplayer.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
        Log.w("MainActivity", "onDestroy: ")
    }
    private fun startMusicService() {
        println("go to StartMusicService")
        if (viewModel.recreateActivity.value != true) {
            val intent = Intent(this, SimpleMediaService::class.java)
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            viewModel.isServiceRunning.value = true
            Log.d("Service", "Service started")
        }
    }
    private fun stopService(){
        if (viewModel.recreateActivity.value != true){
            viewModel.isServiceRunning.value = false
            viewModel.simpleMediaServiceHandler?.mayBeSaveRecentSong()
            viewModel.simpleMediaServiceHandler?.mayBeSavePlaybackState()
            viewModel.simpleMediaServiceHandler?.release()
            viewModel.simpleMediaServiceHandler = null
            unbindService(serviceConnection)
            Log.d("Service", "Service stopped")
            if (this.isMyServiceRunning(DownloadService:: class.java)){
                stopService(Intent(this, DownloadService::class.java))
                viewModel.changeAllDownloadingToError()
                Log.d("Service", "DownloadService stopped")
            }
        }
    }

    fun hideBottomNav(){
        binding.bottomNavigationView.visibility = View.GONE
        binding.miniplayer.visibility = View.GONE
    }
    fun showBottomNav(){
        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.miniPlayerContainer.visibility = View.VISIBLE
    }

    private fun putString(key: String, value: String) {
        viewModel.putString(key, value)
    }

    private fun getString(key: String): String? {
        return viewModel.getString(key)
    }

}