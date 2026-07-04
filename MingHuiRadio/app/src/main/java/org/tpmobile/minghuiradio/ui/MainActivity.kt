package org.tpmobile.minghuiradio.ui

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import org.tpmobile.minghuiradio.R
import org.tpmobile.minghuiradio.data.UiState
import org.tpmobile.minghuiradio.service.PlaybackService
import org.tpmobile.minghuiradio.ui.ScreenRoute.ROUTE_FAVORITE_PAGE
import org.tpmobile.minghuiradio.ui.ScreenRoute.ROUTE_MUSIC_PAGE
import org.tpmobile.minghuiradio.ui.theme.MingHuiRadioTheme
import org.tpmobile.minghuiradio.ui.viewmodel.MhrViewModel
import org.tpmobile.minghuiradio.util.Logger
import org.tpmobile.minghuiradio.util.ktx.toast
import java.util.concurrent.ExecutionException


class MainActivity : ComponentActivity() {

  val TAG = "MainActivity"
  private var exitTime = 0L
  val mhrViewModel: MhrViewModel by viewModels()
  private lateinit var controllerFuture: ListenableFuture<MediaController>
  private lateinit var mediaController: MediaController
  private val playerListener = object : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
      super.onPlayerError(error)
      Logger.e(TAG, "onPlayerError: ${error.message}")
      toast(getString(R.string.playing_error_with_param, error.message))
    }

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
      super.onAvailableCommandsChanged(availableCommands)
      if (availableCommands.contains(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)) {
        mhrViewModel.setNewMediaTitleState(mediaController.currentMediaItem?.mediaMetadata?.title.toString())
      }
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        try {
          initializeController()
          awaitCancellation()
        } finally {
          releaseController()
        }
      }
    }

    enableEdgeToEdge()
    dispatchOnBackEvent()

    setContent {
      MhrApp(mhrViewModel)
    }
  }

  fun dispatchOnBackEvent() {
    onBackPressedDispatcher.addCallback(
      this, onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          Logger.i(TAG, "dispatchOnBackEvent: handleOnBackPressed...")
          if ((System.currentTimeMillis() - exitTime) > 2000) {
            toast(R.string.quit_the_app_after_pressing_back_key_once_again)
            exitTime = System.currentTimeMillis()
          } else {
            mhrViewModel.clearAllMusicItemPlayState()
            mhrViewModel.clearAllFavoriteItemPlayState()
            if (::mediaController.isInitialized) {
              mediaController.stop()
              mediaController.setMediaItems(emptyList())
            }
            finish()
          }
        }
      })
  }

  override fun onDestroy() {
    super.onDestroy()
    Logger.i(TAG, "onDestroy()...")
    //1、明慧广播源：每次运行都从头开始，即从主页栏目开始，因为要展示”最新收听““热门节目”
    //2、希望之声源：从上次选择分类的开始
    //setPref(PREF_WEB_SITE_SELECTED, "")
    //setPref(PREF_WEB_SITE_SECTION_SELECTED, "")
    //setPref(PREF_WEB_SITE_CATEGORY_SELECTED, "")
    //setPref(PREF_WEB_SITE_PAGE_SELECTED, "")
  }

  private suspend fun initializeController() {
    controllerFuture =
      MediaController.Builder(
        this,
        SessionToken(this, ComponentName(this, PlaybackService::class.java)),
      )
        .buildAsync()
    setController()
  }

  private fun releaseController() {
    MediaController.releaseFuture(controllerFuture)
  }

  private suspend fun setController() {
    try {
      mediaController = controllerFuture.await()
      mhrViewModel.setMediaControllerState(UiState.Data(mediaController))
    } catch (e: ExecutionException) {
      Logger.w(TAG, "Failed to connect to MediaController", e)
      mhrViewModel.setMediaControllerState(
        UiState.Error(
          getString(
            R.string.failed_to_connect_media_controller,
            e.cause
          )
        )
      )
      return
    }
    mediaController.addListener(playerListener)
  }

}

///////////////////////////////////////////////////////
@Composable
fun MhrApp(viewModel: MhrViewModel) {
  MingHuiRadioTheme {
    val mediaControllerState by viewModel.mediaControllerState.collectAsState()
    when (mediaControllerState) {
      is UiState.Loading -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,

          ) {
          CircularProgressIndicator()
          Text(
            stringResource(R.string.connecting_media_controller),
            modifier = Modifier.padding(32.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      is UiState.Error -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Icon(
            Icons.Outlined.Info,
            modifier = Modifier.size(48.dp),
            contentDescription = stringResource(R.string.tips_error),
            tint = MaterialTheme.colorScheme.error
          )
          Spacer(Modifier.height(12.dp))
          Text(
            (mediaControllerState as UiState.Error).message,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
          )
          /*Spacer(Modifier.height(12.dp))
          OutlinedButton(onClick = {
            viewModel.refreshWebSiteInfos()
          }) {
            Text("再次尝试", style = MaterialTheme.typography.titleSmall)
          }*/
        }
      }

      is UiState.Data -> {
        val controller = (mediaControllerState as UiState.Data<MediaController>).data
        val navController = rememberNavController()
        navController.addOnDestinationChangedListener { controller, destination, bundle ->
          Logger.i(
            "MainActivity",
            "OnDestinationChangedListener: ${controller.currentDestination?.route}"
          )
        }
        NavHost(
          navController = navController,
          startDestination = ROUTE_MUSIC_PAGE,
          enterTransition = { EnterTransition.None },
          exitTransition = { ExitTransition.None },
        ) {
          composable(ROUTE_MUSIC_PAGE) {
            //val musicItems by viewModel.musicItems.collectAsState()
            MusicScreen(
              viewModel = viewModel,
              navController = navController,
              mediaController = controller
            )
          }
          composable(ROUTE_FAVORITE_PAGE) {
            FavoriteScreen(
              viewModel = viewModel,
              navController = navController,
              mediaController = controller
            )
          }
        }
      }
    }
  }
}

//////////////////////////////////////
@Composable
fun AnimatedVectorDrawable(atEnd: Boolean) {
  val image = AnimatedImageVector.animatedVectorResource(R.drawable.audio_wave_animated)
  Image(
    painter = rememberAnimatedVectorPainter(image, atEnd),
    contentDescription = stringResource(R.string.playing_animation),
    modifier = Modifier.size(48.dp),
    contentScale = ContentScale.Fit,// .Crop
    colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary)
  )
}
