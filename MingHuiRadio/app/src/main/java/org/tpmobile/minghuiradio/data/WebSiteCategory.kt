package org.tpmobile.minghuiradio.data

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.TypeConverters
import org.tpmobile.minghuiradio.db.MyTypeConverters

@Keep
@Entity(
  tableName = "site_category",
  primaryKeys = ["web_site_name", "section_name", "category_name"]
)
@TypeConverters(MyTypeConverters::class)
data class WebSiteCategory(
  @ColumnInfo(name = "web_site_name")
  val webSiteName: String,
  @ColumnInfo(name = "section_name")
  val sectionName: String,
  @ColumnInfo(name = "category_name")
  val categoryName: String,
  @ColumnInfo(name = "category_id")
  val categoryId: String,
  @ColumnInfo(name = "page_range")
  var pageRange: List<Int> = emptyList(),
  @ColumnInfo(name = "page_urls")
  val programUrls: List<String> = emptyList(),//仅仅用于明慧广播源的【主页栏目】的两个分类
) {
  /**
   * @return 明慧广播源>主页栏目>最新收听> 需要补充 page
   */
  fun toSelectedOptions() = "$webSiteName>$sectionName>$categoryName>"

  fun toFetchingUrl(webSiteInfo: WebSiteInfo) = String.format(
    webSiteInfo.musicUrlFormat,
    categoryId,
    "1"
  )

}
