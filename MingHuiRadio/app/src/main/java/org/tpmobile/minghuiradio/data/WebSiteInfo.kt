package org.tpmobile.minghuiradio.data

import androidx.annotation.Keep

@Keep
data class WebSiteInfo(
  val webSiteName: String,
  val webSiteUrl: String,
  val musicUrlHead: String,
  val musicUrlFormat: String,
)
