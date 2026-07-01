package org.tpmobile.minghuiradio.data

import androidx.annotation.Keep

@Keep
sealed class UiStateWithProgress<out T> {
  data class Loading(val progress: Int) : UiStateWithProgress<Nothing>()
  data class Data<T>(val data: T) : UiStateWithProgress<T>()
  data class Error(val message: String) : UiStateWithProgress<Nothing>()
}