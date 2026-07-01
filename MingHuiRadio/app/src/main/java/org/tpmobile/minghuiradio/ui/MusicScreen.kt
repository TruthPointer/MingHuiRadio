package org.tpmobile.minghuiradio.ui

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import org.tpmobile.minghuiradio.BuildConfig.MY_DEBUG
import org.tpmobile.minghuiradio.MyApp
import org.tpmobile.minghuiradio.R
import org.tpmobile.minghuiradio.data.MusicItem
import org.tpmobile.minghuiradio.data.UiStateWithProgress
import org.tpmobile.minghuiradio.exoplayer.ui.MusicPlayer
import org.tpmobile.minghuiradio.ui.ScreenRoute.ROUTE_FAVORITE_PAGE
import org.tpmobile.minghuiradio.ui.viewmodel.MhrViewModel
import org.tpmobile.minghuiradio.ui.widget.CardDialog
import org.tpmobile.minghuiradio.ui.widget.CategorySelectionDialog
import org.tpmobile.minghuiradio.ui.widget.PermissionSelection
import org.tpmobile.minghuiradio.util.Logger


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
  modifier: Modifier = Modifier,
  viewModel: MhrViewModel,
  navController: NavController,
  mediaController: MediaController
) {
  val TAG = "MusicScreen"
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val siteCategories by viewModel.siteCategories.collectAsState()
  val musicScreenUiState by viewModel.musicScreenUiState.collectAsState()

  var showAboutDialog by remember { mutableStateOf(false) }
  var showExoplayer by remember { mutableStateOf(false) }
  var showCategorySelectionDialog by remember { mutableStateOf(false) }
  var expandProxySelectionMenu by remember { mutableStateOf(false) }

  val categorySelected = rememberSaveable { mutableStateOf("") }
  val loadingInner = rememberSaveable { mutableStateOf(false) }
  val currentMp3Url = rememberSaveable { mutableStateOf("") }

  val listener = object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
      super.onPlaybackStateChanged(playbackState)
      Logger.i(TAG, "onPlaybackStateChanged: $playbackState, ${currentMp3Url.value}")
      if(currentMp3Url.value.isEmpty()) return
      if (playbackState == Player.STATE_ENDED) {
        viewModel.updateMusicItemSelection(currentMp3Url.value, false)
      } else if (playbackState == Player.STATE_READY) {
        viewModel.updateMusicItemSelection(currentMp3Url.value, true)
      }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
      super.onIsPlayingChanged(isPlaying)
      Logger.i(TAG, "onIsPlayingChanged: $isPlaying, ${currentMp3Url.value}")
      if(currentMp3Url.value.isEmpty()) return
      viewModel.updateMusicItemSelection(currentMp3Url.value, isPlaying)
    }
  }

  LifecycleStartEffect(Unit) {
    mediaController.addListener(listener)
    onStopOrDispose {
      Logger.i(TAG, "LifecycleStartEffect => onStopOrDispose...")
      //showExoplayer = false
      //viewModel.clearAllMusicItemSelectionState()
      mediaController.removeListener(listener)
      //mediaController.stop()
      //mediaController.setMediaItems(emptyList())
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Logger.i(TAG, musicScreenUiState.toString())

    when (musicScreenUiState) {
      is UiStateWithProgress.Loading -> {
        val progress = (musicScreenUiState as UiStateWithProgress.Loading).progress
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          //CircularProgressIndicator()
          Box(
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(
              progress = { progress / 100f },
              modifier = Modifier.size(56.dp),//.padding(16.dp),
              color = ProgressIndicatorDefaults.circularColor,
              strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth,
              trackColor = MaterialTheme.colorScheme.secondary,
              strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
              gapSize = 8.dp
            )
            Text(
              text = "$progress%",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Text(
            stringResource(R.string.loading_data),
            modifier = Modifier.padding(32.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      is UiStateWithProgress.Error -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
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
            (musicScreenUiState as UiStateWithProgress.Error).message,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
          )
          Spacer(Modifier.height(12.dp))
          OutlinedButton(onClick = {
            viewModel.retrieveMusicList()
          }) {
            Text(
              stringResource(R.string.try_again),
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      is UiStateWithProgress.Data -> {
        Logger.i(TAG, "UiState.Data...")
        loadingInner.value = false
        //
        if (Build.VERSION.SDK_INT >= 33)
          PermissionSelection(Manifest.permission.POST_NOTIFICATIONS)
        //
        val data =
          (musicScreenUiState as UiStateWithProgress.Data<Pair<String, List<MusicItem>>>).data
        val musicItems = data.second
        categorySelected.value = data.first
        //val mediaItems = musicItems.map { it.toMediaItem() }
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            TopAppBar(
              title = {},
              navigationIcon = {
                Image(ImageVector.vectorResource(R.drawable.mhradio_logo), null)
              },
              actions = {
                IconButton(onClick = {
                  /*mediaController.stop()
                  showExoplayer = false*/
                  navController.navigate(ROUTE_FAVORITE_PAGE)
                }) { Icon(Icons.Outlined.FavoriteBorder, stringResource(R.string.favorite_screen)) }
                IconButton(onClick = {
                  expandProxySelectionMenu = true
                }) { Icon(Icons.Filled.SettingsEthernet, stringResource(R.string.proxy_setting)) }
                DropdownMenu(
                  expanded = expandProxySelectionMenu,
                  onDismissRequest = { expandProxySelectionMenu = false }
                ) {
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_vpn_freegate_8590)) },
                    onClick = {
                      MyApp.setProxy(8590)
                    }
                  )
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_vpn_wujie_19966)) },
                    onClick = {
                      MyApp.setProxy(19966)
                    }
                  )
                }
                IconButton(onClick = {
                  showAboutDialog = true
                }) { Icon(Icons.Outlined.QuestionMark, "关于") }
              },
              //scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            )
          },
        ) { innerPadding ->
          Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
              .fillMaxSize()
              .padding(top = innerPadding.calculateTopPadding()),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background, shape = CircleShape)
                .padding(horizontal = 8.dp)
                .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.secondary,
                  shape = CircleShape
                ),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = /*categoryOptions,*/categorySelected.value,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier
                  .weight(1f)
                  .padding(start = 12.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
              )
              IconButton(onClick = {
                showCategorySelectionDialog = true
              }) {
                Icon(
                  imageVector = Icons.Filled.Search,
                  stringResource(R.string.searching_program)
                )
              }
            }
            Spacer(Modifier.height(8.dp))
            Box(
              modifier = Modifier.weight(1f),
              contentAlignment = Alignment.TopCenter,
            ) {
              if (loadingInner.value) {
                CircularProgressIndicator()
                Text(
                  stringResource(R.string.loading_data),
                  modifier = Modifier.padding(top = 16.dp)
                )
              } else {
                if (musicItems.isEmpty()) {
                  Column(
                    modifier = Modifier.fillMaxSize(),
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
                      stringResource(R.string.failed_to_retrieve_play_list),
                      modifier = Modifier
                        .padding(16.dp)
                        .wrapContentWidth(),
                      textAlign = TextAlign.Center,
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.error,
                    )
                  }
                } else {
                  MusicList(
                    musicItems,
                    Modifier
                      .wrapContentHeight()
                      .padding(bottom = innerPadding.calculateBottomPadding()),
                    onClick = { itemSelected ->
                      Logger.i(TAG, "点击了：$itemSelected")
                      currentMp3Url.value = musicItems[itemSelected].url
                      showExoplayer = true
                      //context.setPref(PREF_CURRENT_PLAYING, "$ROUTE_MUSIC_PAGE|${musicItems[itemSelected].url}")
                      mediaController.apply {
                        setMediaItems(listOf(musicItems[itemSelected].toMediaItem()))
                        prepare()
                        play()
                      }
                    },
                    viewModel
                  )
                }
              }
            }
            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(visible = showExoplayer) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .shadow(10.dp, RectangleShape)
              ) {
                MusicPlayer(
                  mediaController = mediaController,
                  modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                  viewModel,
                  onClick = { isPlaying ->
                    if (currentMp3Url.value.isNotEmpty()) {
                      viewModel.updateMusicItemSelection(currentMp3Url.value, isPlaying)
                    }
                  }
                )
              }
            }

            if (showCategorySelectionDialog) {
              CategorySelectionDialog(
                webSites = siteCategories,
                webSiteInfos = viewModel.webSiteInfos,
                onDismissRequest = {
                  showCategorySelectionDialog = false
                },
                onConfirmation = { info ->
                  showCategorySelectionDialog = false
                  /*showExoplayer = false
                  mediaController.stop()
                  mediaController.setMediaItems(emptyList())*/

                  //viewModel.setCategoryOptionsState(info.selectedOptions())
                  categorySelected.value = info.selectedOptions()
                  viewModel.fetchMusicList(info)
                }
              )
            }
            if (showAboutDialog) {
              CardDialog(
                title = {
                  Text(
                    text = stringResource(R.string.dialog_about),
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Left,
                    style = MaterialTheme.typography.titleLarge,
                  )
                },
                buttonsHorizontalArrangement = Arrangement.End,
                content = {
                  Text(text = stringResource(R.string.app_message))
                  Spacer(Modifier.height(12.dp))
                  Image(painter = painterResource(id = R.drawable.my_hope), "")
                },
                buttons = {
                  TextButton(
                    onClick = {
                      showAboutDialog = false
                    },
                  ) { Text(stringResource(R.string.dialog_button_i_know)) }
                },
                onDismissRequest = { showAboutDialog = false }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun MusicList(
  list: List<MusicItem>, modifier: Modifier, onClick: (Int) -> Unit, viewModel: MhrViewModel
) {
  Logger.i(
    "MusicScreen",
    "已经被选择的：" + list.filter { it.isSelected }.joinToString("\r\n") { it.title })
  LazyColumn(
    modifier = modifier,//.wrapContentHeight(),//Modifier.padding(top = paddingTop),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
  ) {
    itemsIndexed(items = list) { index, item ->
      //var favoriteIconId by remember { mutableStateOf(Icons.Default.FavoriteBorder) }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 16.dp)
          .clickable(onClick = {
            val clickIt = !item.isSelected
            viewModel.clearAllMusicItemSelectionState()
            if (clickIt)
              viewModel.updateMusicItemSelection(item.url, true)
            viewModel.getAllMusicItems()
            onClick(index)
          }),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        AnimatedVectorDrawable(item.isSelected)
        Column(Modifier.wrapContentHeight()) {
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (MY_DEBUG) "[${item.index}]${item.title}" else item.title,
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
              IconButton(onClick = {
                val selection = !item.isFavorite
                viewModel.updateMusicItemFavorite(item.url, selection)
                if (selection) {
                  viewModel.insertFavoriteItem(item.toFavoriteItem())
                  //Icons.Default.Favorite
                } else {
                  viewModel.deleteFavoriteItem(item.toFavoriteItem())
                  //Icons.Default.FavoriteBorder
                }
                viewModel.getAllMusicItems()
              }) {
                Icon(
                  imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                  contentDescription = "",
                  modifier = Modifier
                    .padding(end = 8.dp),
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
