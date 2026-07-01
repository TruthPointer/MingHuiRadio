package org.tpmobile.minghuiradio.ui.widget

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.tpmobile.minghuiradio.R
import org.tpmobile.minghuiradio.util.Logger
import org.tpmobile.minghuiradio.util.PREF_PERMISSION_DENIED_PERMANENTLY
import org.tpmobile.minghuiradio.util.PREF_RUN_TIMES
import org.tpmobile.minghuiradio.util.PREF_RUN_TIMES_FOR_RATIONALE
import org.tpmobile.minghuiradio.util.ktx.getPref
import org.tpmobile.minghuiradio.util.ktx.setPref

@Composable
fun CardDialog(
  title: @Composable (() -> Unit),
  content: @Composable (() -> Unit),
  buttons: @Composable (() -> Unit),
  buttonsHorizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
  onDismissRequest: () -> Unit,
) {
  Dialog(onDismissRequest = { onDismissRequest() }) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(horizontal = 16.dp, vertical = 32.dp),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier
          .wrapContentHeight()
          .wrapContentWidth()
          .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
      ) {
        title()
        Spacer(Modifier.height(16.dp))
        content()
        Spacer(Modifier.height(16.dp))
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = buttonsHorizontalArrangement
        ) {
          buttons()
        }
      }
    }
  }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionSelection(
  permission: String,
) {
  val context = LocalContext.current
  var runTimes: Int = context.getPref(PREF_RUN_TIMES, 0)
  Logger.i("MusicScreen", "runTimes = $runTimes")
  if (runTimes <= 3) {
    context.setPref(PREF_RUN_TIMES, ++runTimes)
    return
  }
  if (context.getPref(PREF_PERMISSION_DENIED_PERMANENTLY, false)) return

  val permissionState = rememberPermissionState(permission)
  var launchPermissionDialog by remember { mutableStateOf(true) }
  var showRationale by remember { mutableStateOf(true) }

  when {
    permissionState.status.isGranted -> {
      //LocalContext.current.toast("获得通知权限了")
      Logger.i("PermissionSelection", "获得通知权限了")
    }

    permissionState.status.shouldShowRationale -> {
      var runTimesForRationale: Int = context.getPref(PREF_RUN_TIMES_FOR_RATIONALE, 0)
      Logger.i("MusicScreen", "runTimesForRationale = $runTimesForRationale")
      if (runTimesForRationale <= 3) {
        context.setPref(PREF_RUN_TIMES_FOR_RATIONALE, ++runTimesForRationale)
        return
      }
      if (showRationale) {
        RationalPermissionDialog(
          onDismissRequest = { showRationale = false }
        )
      }
    }

    else -> {
      if (launchPermissionDialog) {
        LaunchPermissionDialog(
          permissionState,
          onDismissRequest = { launchPermissionDialog = false }
        )

        /*SideEffect {
          permissionState.launchPermissionRequest()
        }*/
      }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LaunchPermissionDialog(
  permissionState: PermissionState,
  onDismissRequest: () -> Unit
) {
  val context = LocalContext.current
  CardDialog(
    title = {
      Text(
        text = stringResource(R.string.permission_requirement),
        modifier = Modifier.padding(16.dp),
        textAlign = TextAlign.Left,
        style = MaterialTheme.typography.titleLarge,
      )
    },
    content = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(Icons.Outlined.Info, "", modifier = Modifier.size(48.dp))
      }
      Spacer(Modifier.height(8.dp))
      Text(text = stringResource(R.string.permission_request))
    },
    buttons = {
      TextButton(
        onClick = {
          onDismissRequest()
          context.setPref(PREF_PERMISSION_DENIED_PERMANENTLY, true)
        },
      ) { Text(stringResource(R.string.do_not_remind_again)) }
      TextButton(
        onClick = { onDismissRequest() },
      ) { Text(stringResource(R.string.dialog_result_cancel)) }
      FilledTonalButton(
        onClick = {
          onDismissRequest()
          permissionState.launchPermissionRequest()
        },
      ) { Text(stringResource(R.string.dialog_result_ok)) }
    },
    onDismissRequest = { onDismissRequest() }
  )
}

@Composable
fun RationalPermissionDialog(
  onDismissRequest: () -> Unit
) {
  val context = LocalContext.current
  CardDialog(
    title = {
      Text(
        text = stringResource(R.string.permission_requirement),
        modifier = Modifier.padding(16.dp),
        textAlign = TextAlign.Left,
        style = MaterialTheme.typography.titleLarge,
      )
    },
    content = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(Icons.Outlined.Info, "", modifier = Modifier.size(48.dp))
      }
      Spacer(Modifier.height(8.dp))
      Text(text = stringResource(R.string.permission_request_in_app_settings))
    },
    buttons = {
      TextButton(
        onClick = {
          onDismissRequest()
          context.setPref(PREF_PERMISSION_DENIED_PERMANENTLY, true)
        },
      ) { Text(stringResource(R.string.do_not_remind_again)) }
      TextButton(
        onClick = { onDismissRequest() },
      ) { Text(stringResource(R.string.dialog_result_cancel)) }
      FilledTonalButton(
        onClick = {
          onDismissRequest()
          val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
          }
          context.startActivity(intent, null)
        },
      ) { Text(stringResource(R.string.go_to_settings)) }
    },
    onDismissRequest = { onDismissRequest() }
  )
}