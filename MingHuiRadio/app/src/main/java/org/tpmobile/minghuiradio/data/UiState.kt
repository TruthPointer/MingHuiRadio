package org.tpmobile.minghuiradio.data

import androidx.annotation.Keep

@Keep
sealed class UiState<out T> {
  object Loading : UiState<Nothing>()
  data class Data<T>(val data: T) : UiState<T>()
  data class Error(val message: String) : UiState<Nothing>()
}