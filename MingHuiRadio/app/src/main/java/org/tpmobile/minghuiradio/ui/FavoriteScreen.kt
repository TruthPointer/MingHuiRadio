package org.tpmobile.minghuiradio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import kotlinx.coroutines.launch
import org.tpmobile.minghuiradio.R
import org.tpmobile.minghuiradio.data.FavoriteItem
import org.tpmobile.minghuiradio.data.UiState
import org.tpmobile.minghuiradio.exoplayer.ui.MusicPlayer
import org.tpmobile.minghuiradio.ui.viewmodel.MhrViewModel
import org.tpmobile.minghuiradio.ui.widget.LottieMusicAnimation
import org.tpmobile.minghuiradio.util.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
  modifier: Modifier = Modifier,
  viewModel: MhrViewModel,
  navController: NavController,
  mediaController: MediaController
) {
  val TAG = "FavoriteScreen"
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val favoriteScreenUiState by viewModel.favoriteScreenUiState.collectAsState()

  var showConfirmationDialog by remember { mutableStateOf(false) }
  var showExoplayer by remember { mutableStateOf(false) }

  val currentMp3Url = rememberSaveable { mutableStateOf("") }

  LaunchedEffect(context) {
    println("LaunchedEffect...")
    viewModel.getAllFavoriteItems()
  }

  val listener = object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
      super.onPlaybackStateChanged(playbackState)
      Logger.i(TAG, "onPlaybackStateChanged: $playbackState, ${currentMp3Url.value}")
      if(currentMp3Url.value.isEmpty()) return
      if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
        viewModel.updateFavoriteItemPlayState(currentMp3Url.value, false)
      } else if (playbackState == Player.STATE_READY) {
        viewModel.updateFavoriteItemPlayState(currentMp3Url.value, true)
      }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
      super.onIsPlayingChanged(isPlaying)
      Logger.i(TAG, "onIsPlayingChanged: $isPlaying, ${currentMp3Url.value}")
      if(currentMp3Url.value.isEmpty()) return
      viewModel.updateFavoriteItemPlayState(currentMp3Url.value, isPlaying)
    }
  }
  mediaController.addListener(listener)

  LifecycleStartEffect(Unit) {
    //mediaController.addListener(listener)
    onStopOrDispose {
      Logger.i(TAG, "LifecycleStartEffect => onStopOrDispose...")
      mediaController.removeListener(listener)
    }
  }

  BackHandler {
    coroutineScope.launch {
      if (showExoplayer) {
        showExoplayer = false
        currentMp3Url.value = ""
        viewModel.clearAllFavoriteItemPlayState()
        mediaController.stop()
        mediaController.setMediaItems(emptyList())
      }
      navController.popBackStack()
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Logger.i(TAG, favoriteScreenUiState.toString())
    when (favoriteScreenUiState) {
      is UiState.Loading -> {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          CircularProgressIndicator()
          Text(
            stringResource(R.string.loading_data),
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
            .padding(top = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          //再加个失败图片会更好
          //Image(ImageVector.vectorResource(R.drawable.mhradio_logo), null)
          Icon(
            Icons.Outlined.Error,
            modifier = Modifier.size(48.dp),
            contentDescription = stringResource(R.string.tips_error),
            tint = MaterialTheme.colorScheme.error
          )
          Spacer(Modifier.height(12.dp))
          Text(
            (favoriteScreenUiState as UiState.Error).message,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      is UiState.Data -> {
        val favoriteItems = (favoriteScreenUiState as UiState.Data<List<FavoriteItem>>).data
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          {
            TopAppBar(
              title = {},
              navigationIcon = {
                IconButton(onClick = {
                  if (showExoplayer) {
                    showExoplayer = false
                    currentMp3Url.value = ""
                    viewModel.clearAllFavoriteItemPlayState()
                    mediaController.stop()
                    mediaController.setMediaItems(emptyList())
                  }
                  navController.popBackStack()
                }) {
                  Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.return_last_screen)
                  )
                }
              },
              actions = {
                IconButton(onClick = {
                  showConfirmationDialog = true
                }) {
                  Icon(
                    Icons.Outlined.DeleteSweep,
                    stringResource(R.string.clear_all_favorites)
                  )
                }
              },
            )
          },
        ) { innerPadding ->
          Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
              .fillMaxSize()
              .padding(top = innerPadding.calculateTopPadding()),
          ) {
            Box(
              modifier = Modifier.weight(1f),
              contentAlignment = Alignment.TopCenter,
            ) {
              if (favoriteItems.isEmpty()) {
                Column(
                  modifier = Modifier.fillMaxSize(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
                ) {
                  //再加个失败图片会更好
                  Icon(
                    Icons.Outlined.Info,
                    modifier = Modifier.size(48.dp),
                    contentDescription = stringResource(R.string.tips_error)
                  )
                  Spacer(Modifier.height(12.dp))
                  Text(
                    stringResource(R.string.not_found_favorite_list),
                    modifier = Modifier
                      .padding(16.dp)
                      .wrapContentWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                  )
                }
              } else {
                FavoriteList(
                  favoriteItems,
                  Modifier
                    .wrapContentHeight()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                  onClick = { itemSelected ->
                    Logger.i(TAG, "点击了：$itemSelected")
                    viewModel.clearAllMusicItemPlayState()//20260713
                    currentMp3Url.value = favoriteItems[itemSelected].url
                    showExoplayer = true
                    //context.setPref(PREF_CURRENT_PLAYING, "$ROUTE_FAVORITE_PAGE|${favoriteItems[itemSelected].url}")
                    mediaController.apply {
                      setMediaItems(listOf(favoriteItems[itemSelected].toMediaItem()))
                      prepare()
                      play()
                    }
                  },
                  viewModel,
                  onDelete = { itemDeleted ->
                    //1、检查当前播放状态，相同时停止播放等
                    if (favoriteItems[itemDeleted].isPlaying) {
                      showExoplayer = false
                      currentMp3Url.value = ""
                      mediaController.stop()
                      mediaController.setMediaItems(emptyList())
                    }
                    //2.
                    viewModel.deleteFavoriteItem(favoriteItems[itemDeleted])
                    viewModel.updateMusicItemFavorite(favoriteItems[itemDeleted].url, false)
                    viewModel.updateMusicItemPlayState(favoriteItems[itemDeleted].url, false)//20260713
                  }
                )
              }
            }
            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(visible = showExoplayer) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .shadow(10.dp, RectangleShape)
                //.padding(8.dp),
              ) {
                //MusicPlayer(currentMediaItems)
                MusicPlayer(
                  mediaController,
                  modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                  viewModel,
                  onClick = { isPlaying ->
                    if (currentMp3Url.value.isNotEmpty()) {
                      viewModel.updateFavoriteItemPlayState(currentMp3Url.value, isPlaying)
                    }
                  }
                )
              }
            }
          }
        }
      }
    }
  }

  if (showConfirmationDialog) {
    AlertDialog(
      icon = { Icon(Icons.Outlined.QuestionMark, contentDescription = "") },
      title = { Text(text = stringResource(R.string.dialog_result_ask)) },
      text = { Text(text = stringResource(R.string.confirm_deleting_all_favorites)) },
      onDismissRequest = {
        showConfirmationDialog = false
      },
      confirmButton = {
        TextButton(
          onClick = {
            showConfirmationDialog = false
            //1.
            if(showExoplayer){
              showExoplayer = false
              currentMp3Url.value = ""
              mediaController.stop()
              mediaController.setMediaItems(emptyList())
            }
            //2.
            viewModel.clearAllFavoriteItems()
          }
        ) { Text(stringResource(R.string.dialog_result_ok)) }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showConfirmationDialog = false
          }
        ) { Text(stringResource(R.string.dialog_result_cancel)) }
      }
    )
  }
}

@Composable
fun FavoriteList(
  list: List<FavoriteItem>,
  modifier: Modifier,
  onClick: (Int) -> Unit,
  viewModel: MhrViewModel,
  onDelete: (Int) -> Unit
) {
  val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.audio_wave_animated))
  val dynamicProperties =  rememberLottieDynamicProperties(
    rememberLottieDynamicProperty(
      property = LottieProperty.COLOR,
      value = MaterialTheme.colorScheme.secondary.toArgb(),//Color.Red.toArgb(),
      keyPath = arrayOf("**","Fill"),
    ),
  )
  LazyColumn(
    modifier = modifier,//.wrapContentHeight(),//Modifier.padding(top = paddingTop),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
  ) {
    itemsIndexed(items = list) { index, item ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 16.dp)
          .clickable(onClick = {
            val clickIt = !item.isPlaying
            viewModel.clearAllFavoriteItemPlayState()
            if (clickIt)
              viewModel.updateFavoriteItemPlayState(item.url, true)
            viewModel.getAllFavoriteItems()
            onClick(index)
          }),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        LottieMusicAnimation(index, item.isPlaying, composition, dynamicProperties)
        Column(Modifier.wrapContentHeight()) {
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = item.title,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight(800),
              textAlign = TextAlign.Left,
              minLines = 2,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f)
            )
            Row() {
              IconButton(
                onClick = {
                  onDelete(index)
                },
              ) {
                Icon(
                  Icons.Outlined.Delete,
                  stringResource(R.string.delete_favorite),
                  tint = MaterialTheme.colorScheme.secondary
                )
              }
            }
          }
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row() {
              Text(
                text = stringResource(R.string.item_duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = item.duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Left,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Row() {
              Text(
                text = stringResource(R.string.item_pub_date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = item.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Left,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}
