package org.tpmobile.minghuiradio.exoplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.indicator.PositionAndDurationText
import org.tpmobile.minghuiradio.exoplayer.buttons.LabeledProgressSlider
import org.tpmobile.minghuiradio.ui.viewmodel.MhrViewModel
import org.tpmobile.minghuiradio.util.Logger

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
fun MusicPlayer(
  mediaController: MediaController,
  modifier: Modifier = Modifier,
  viewModel: MhrViewModel,
  onClick: (Boolean) -> Unit
) {
  val newMediaTitle by viewModel.newMediaTitleState.collectAsState()

  Box(
    modifier
      .background(MaterialTheme.colorScheme.background)
      .wrapContentHeight()
  ) {
    val innerModifier = Modifier
      //.size(24.dp)
      .background(
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        ButtonDefaults.shape,
      )
    val defaultIconButtonColors: IconButtonColors =
      IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)

    Column(
      modifier = Modifier
        .wrapContentHeight()
        .padding(horizontal = 12.dp, vertical = 5.dp),
      verticalArrangement = Arrangement.Center
    ) {
      Logger.i("MusicPlayer", "currentMediaItem-title = $newMediaTitle")
      Text(
        text = newMediaTitle,//"${mediaController.currentMediaItem?.mediaMetadata?.title ?: "无名"}",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 2,
      )
      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PlayPauseButton(
          mediaController,
          innerModifier,
          colors = defaultIconButtonColors,
          onClick = {
            this.onClick()
            onClick(mediaController.isPlaying)
          })
        LabeledProgressSlider(mediaController, Modifier.weight(0.6f))
        PositionAndDurationText(mediaController, color = MaterialTheme.colorScheme.primary)
      }
    }
  }
}
