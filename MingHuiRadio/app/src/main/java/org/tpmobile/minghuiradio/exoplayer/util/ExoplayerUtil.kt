/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tpmobile.minghuiradio.exoplayer.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import org.tpmobile.minghuiradio.exoplayer.datasource.HttpDataSourceWithProxy

/**
 * Utility methods for the demo app.
 */
object ExoplayerUtil {

  private const val TAG = "ExoplayerUtil"

  @Volatile
  private var dataSourceFactory: DataSource.Factory? = null

  @Volatile
  private var httpDataSourceFactory: HttpDataSource.Factory? = null

  @Volatile
  private var mediaSourceFactory: MediaSource.Factory? = null

  @OptIn(UnstableApi::class)
  fun getHttpDataSourceFactory(): HttpDataSource.Factory =
    httpDataSourceFactory ?: synchronized(this) {
      httpDataSourceFactory ?: HttpDataSourceWithProxy.Factory().apply {
        httpDataSourceFactory = this
      }
    }

  fun getDataSourceFactory(context: Context): DataSource.Factory =
    dataSourceFactory ?: synchronized(this) {
      dataSourceFactory ?: DefaultDataSource.Factory(
        context.applicationContext,
        getHttpDataSourceFactory()
      ).apply {
        dataSourceFactory = this
      }
    }

  @OptIn(UnstableApi::class)
  fun getMediaSourceFactory(context: Context): MediaSource.Factory =
    mediaSourceFactory ?: synchronized(this) {
      mediaSourceFactory ?: DefaultMediaSourceFactory(getDataSourceFactory(context)).apply {
        mediaSourceFactory = this
      }
    }

}