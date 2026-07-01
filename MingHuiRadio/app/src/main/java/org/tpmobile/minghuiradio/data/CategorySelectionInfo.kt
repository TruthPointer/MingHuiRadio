package org.tpmobile.minghuiradio.data

import androidx.annotation.Keep
import org.tpmobile.minghuiradio.util.ParseHelper

@Keep
class CategorySelectionInfo(
  val webSiteInfo: WebSiteInfo,
  val webSiteCategory: WebSiteCategory,
  /**
   * 显示为：第X页，X年
   */
  val page: String
) {
  fun selectedUrl() = String.format(
    webSiteInfo.musicUrlFormat,
    webSiteCategory.categoryId,
    ParseHelper.parseDigital(page)
  )

  fun selectedOptions() = webSiteCategory.toSelectedOptions() + page
}