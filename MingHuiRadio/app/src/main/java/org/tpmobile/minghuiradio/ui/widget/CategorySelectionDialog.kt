package org.tpmobile.minghuiradio.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.tpmobile.minghuiradio.R
import org.tpmobile.minghuiradio.data.CategorySelectionInfo
import org.tpmobile.minghuiradio.data.WebSiteCategory
import org.tpmobile.minghuiradio.data.WebSiteInfo
import org.tpmobile.minghuiradio.util.Logger
import org.tpmobile.minghuiradio.util.PREF_WEB_SITE_CATEGORY_SELECTED
import org.tpmobile.minghuiradio.util.PREF_WEB_SITE_PAGE_SELECTED
import org.tpmobile.minghuiradio.util.PREF_WEB_SITE_SECTION_SELECTED
import org.tpmobile.minghuiradio.util.PREF_WEB_SITE_SELECTED
import org.tpmobile.minghuiradio.util.ParseHelper
import org.tpmobile.minghuiradio.util.RESOURCE_MHR_MAIN_SECTION
import org.tpmobile.minghuiradio.util.RESOURCE_MING_HUI_RADIO
import org.tpmobile.minghuiradio.util.TRY_TIMES
import org.tpmobile.minghuiradio.util.ktx.getPref
import org.tpmobile.minghuiradio.util.ktx.resIdToRealString
import org.tpmobile.minghuiradio.util.ktx.setPref
import org.tpmobile.minghuiradio.util.ktx.toDisplayPageName
import org.tpmobile.minghuiradio.util.ktx.toast

/**
 * webSites 数据各字段不能有空数据，必须排除掉
 */
@Composable
fun CategorySelectionDialog(
  webSites: List<WebSiteCategory>,
  webSiteInfos: List<WebSiteInfo>,
  onDismissRequest: () -> Unit,
  onConfirmation: (CategorySelectionInfo) -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val webSiteNames = webSites.map { it.webSiteName }.distinct().toList()
  //读取保存的值
  val defaultWebSiteName = context.getPref(PREF_WEB_SITE_SELECTED, "").ifEmpty {
    webSiteNames.first()
  }
  val defaultWebSiteSection = context.getPref(PREF_WEB_SITE_SECTION_SELECTED, "").ifEmpty {
    webSites.filter { it.webSiteName == defaultWebSiteName }.map { it.sectionName }.distinct()
      .first()
  }
  val defaultWebSiteCategory = context.getPref(PREF_WEB_SITE_CATEGORY_SELECTED, "").ifEmpty {
    webSites.filter { it.webSiteName == defaultWebSiteName && it.sectionName == defaultWebSiteSection }
      .map { it.categoryName }.distinct().first()
  }
  val defaultWebSitePage = context.getPref(PREF_WEB_SITE_PAGE_SELECTED, "").ifEmpty {
    webSites.first { it.webSiteName == defaultWebSiteName && it.sectionName == defaultWebSiteSection && it.categoryName == defaultWebSiteCategory }.pageRange.first()
      .toDisplayPageName(defaultWebSiteName)
  }

  val webSiteSelected = rememberSaveable() { mutableStateOf(defaultWebSiteName) }
  val webSiteSectionSelected = rememberSaveable() { mutableStateOf(defaultWebSiteSection) }
  val webSiteCategorySelected = rememberSaveable() { mutableStateOf(defaultWebSiteCategory) }
  val webSitePageSelected = rememberSaveable() { mutableStateOf(defaultWebSitePage) }

  val webSiteSections = rememberSaveable() { mutableStateOf(listOf<String>()) }
  val webSiteCategories = rememberSaveable() { mutableStateOf(listOf<String>()) }
  val webSitePages = rememberSaveable() { mutableStateOf(listOf<String>()) }

  val fetchWebSitePages = rememberSaveable() { mutableStateOf(LoadingState.SUCCESS) }

  Dialog(onDismissRequest = { onDismissRequest() }) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier
          .wrapContentHeight()
          .wrapContentWidth()
          .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.select_radio_program_page),
          modifier = Modifier.padding(16.dp),
        )
        CategorySelectionSpinner(
          index = 1,
          title = stringResource(R.string.select_program_resource),
          options = webSiteNames,
          onSelect = { selected ->
            webSiteSelected.value = selected

            //从新设定初始值
            webSites.filter { it.webSiteName == webSiteSelected.value }.apply {
              webSiteSections.value = map { it.sectionName }.distinct().toList().also { sections ->
                webSiteSectionSelected.value = sections.first()
              }
              webSiteCategories.value =
                filter { it.sectionName == webSiteSectionSelected.value }.map { it.categoryName }
                  .toList().also { categories ->
                    webSiteCategorySelected.value = categories.first()
                  }
              //获取page信息
              val category =
                first { it.sectionName == webSiteSectionSelected.value && it.categoryName == webSiteCategorySelected.value }
              if (webSiteSelected.value == RESOURCE_MING_HUI_RADIO && category.sectionName != RESOURCE_MHR_MAIN_SECTION) { //明慧广播源 需要更新页码；希望之声源 不需要
                fetchWebSitePages.value = LoadingState.LOADING
                //启动查询
                coroutineScope.launch {
                  val webSiteInfo = webSiteInfos.first { it.webSiteName == RESOURCE_MING_HUI_RADIO }
                  //清零
                  webSitePages.value = emptyList()
                  webSitePageSelected.value = ""
                  //从新获取
                  var tryTimes = 1
                  while (tryTimes <= TRY_TIMES) {
                    ParseHelper.parsePageValueUpperLimitForMHR(category.toFetchingUrl(webSiteInfo))
                      .fold(onSuccess = { pageUpperLimit ->
                        category.pageRange = (1..pageUpperLimit).toList()
                        webSitePages.value =
                          category.pageRange.map { it.toDisplayPageName(webSiteInfo.webSiteName) }
                        webSitePageSelected.value = if (category.pageRange.isEmpty()) ""
                        else category.pageRange.first().toDisplayPageName(webSiteSelected.value)
                        fetchWebSitePages.value = LoadingState.SUCCESS
                        break
                      }, onFailure = {
                        context.toast(resIdToRealString(R.string.failed_to_retrieve_category_info))//"获取当前分类页面信息失败！"
                        fetchWebSitePages.value = LoadingState.ERROR
                      })
                    tryTimes++
                  }
                }
              } else {
                webSitePages.value =
                  category.pageRange.map { it.toDisplayPageName(webSiteSelected.value) }
                webSitePageSelected.value =
                  if (webSitePages.value.isEmpty()) "" else webSitePages.value.first()
              }
            }
          },
          applyDefault = {
            webSiteSelected.value.ifEmpty { webSiteNames[0] }//0,把那个放在第一位就用默认优先哪个源
          })
        CategorySelectionSpinner(
          index = 2,
          title = stringResource(R.string.select_program_section),
          options = webSiteSections.value.ifEmpty {
            webSites.filter { it.webSiteName == defaultWebSiteName }.map { it.sectionName }
              .distinct().toList()
          },
          onSelect = { selected ->
            webSiteSectionSelected.value = selected

            webSites.filter { it.webSiteName == webSiteSelected.value && it.sectionName == webSiteSectionSelected.value }
              .apply {
                webSiteCategories.value =
                  filter { it.sectionName == webSiteSectionSelected.value }.map { it.categoryName }
                    .toList().also { categories ->
                      webSiteCategorySelected.value = categories.first()
                    }
                //获取page信息
                val category =
                  first { it.sectionName == webSiteSectionSelected.value && it.categoryName == webSiteCategorySelected.value }
                if (webSiteSelected.value == RESOURCE_MING_HUI_RADIO && category.sectionName != RESOURCE_MHR_MAIN_SECTION) { //明慧广播源 需要更新页码；希望之声源 不需要
                  fetchWebSitePages.value = LoadingState.LOADING
                  //启动查询
                  coroutineScope.launch {
                    val webSiteInfo =
                      webSiteInfos.first { it.webSiteName == RESOURCE_MING_HUI_RADIO }
                    //清零
                    webSitePages.value = emptyList()
                    webSitePageSelected.value = ""
                    //从新获取
                    var tryTimes = 1
                    while (tryTimes <= TRY_TIMES) {
                      ParseHelper.parsePageValueUpperLimitForMHR(category.toFetchingUrl(webSiteInfo))
                        .fold(onSuccess = { pageUpperLimit ->
                          category.pageRange = (1..pageUpperLimit).toList()
                          webSitePages.value =
                            category.pageRange.map { it.toDisplayPageName(webSiteInfo.webSiteName) }
                          webSitePageSelected.value =
                            if (category.pageRange.isEmpty()) "" else category.pageRange.first()
                              .toDisplayPageName(webSiteSelected.value)
                          fetchWebSitePages.value = LoadingState.SUCCESS
                          break
                        }, onFailure = {
                          context.toast(resIdToRealString(R.string.failed_to_retrieve_category_info))//"获取当前分类页面信息失败！"
                          fetchWebSitePages.value = LoadingState.ERROR
                        })
                      tryTimes++
                    }
                  }
                } else {
                  webSitePages.value =
                    category.pageRange.map { it.toDisplayPageName(webSiteSelected.value) }
                  webSitePageSelected.value =
                    if (webSitePages.value.isEmpty()) "" else webSitePages.value.first()
                }
              }
          },
          applyDefault = {
            webSiteSectionSelected.value.ifEmpty {
              webSites.asSequence().filter { it.webSiteName == webSiteSelected.value }
                .map { it.sectionName }.distinct().toList().first()
            }
          })
        CategorySelectionSpinner(
          index = 3,
          title = stringResource(R.string.select_program_category),
          options = webSiteCategories.value.ifEmpty {
            webSites.filter { it.webSiteName == webSiteSelected.value && it.sectionName == webSiteSectionSelected.value }
              .map { it.categoryName }.distinct().toList()
          },
          onSelect = { selected ->
            //context.setPref(PREF_WEB_SITE_SECTION_SELECTED, "")
            webSiteCategorySelected.value = selected

            webSites.first { it.webSiteName == webSiteSelected.value && it.sectionName == webSiteSectionSelected.value && it.categoryName == webSiteCategorySelected.value }
              .also { category ->
                //获取page信息
                if (webSiteSelected.value == RESOURCE_MING_HUI_RADIO && category.sectionName != RESOURCE_MHR_MAIN_SECTION) { //明慧广播源 需要更新页码；希望之声源 不需要
                  fetchWebSitePages.value = LoadingState.LOADING
                  //启动查询
                  coroutineScope.launch {
                    val webSiteInfo =
                      webSiteInfos.first { it.webSiteName == RESOURCE_MING_HUI_RADIO }
                    //清零
                    webSitePages.value = emptyList()
                    webSitePageSelected.value = ""
                    //从新获取
                    var tryTimes = 1
                    while (tryTimes <= TRY_TIMES) {
                      ParseHelper.parsePageValueUpperLimitForMHR(category.toFetchingUrl(webSiteInfo))
                        .fold(onSuccess = { pageUpperLimit ->
                          category.pageRange = (1..pageUpperLimit).toList()
                          webSitePages.value =
                            category.pageRange.map { it.toDisplayPageName(webSiteInfo.webSiteName) }
                          webSitePageSelected.value = if (category.pageRange.isEmpty()) ""
                          else category.pageRange.first().toDisplayPageName(webSiteSelected.value)
                          fetchWebSitePages.value = LoadingState.SUCCESS
                          break
                        }, onFailure = {
                          context.toast(resIdToRealString(R.string.failed_to_retrieve_category_info))//"获取当前分类页面信息失败！"
                          fetchWebSitePages.value = LoadingState.ERROR
                        })
                      tryTimes++
                    }
                  }
                } else { //希望之声源 不需要，直接赋值
                  webSitePages.value =
                    category.pageRange.map { it.toDisplayPageName(webSiteSelected.value) }
                  webSitePageSelected.value =
                    if (webSitePages.value.isEmpty()) "" else webSitePages.value.first()
                }
              }
          },
          applyDefault = {
            webSiteCategorySelected.value.ifEmpty {
              webSites.filter { it.webSiteName == webSiteSelected.value && it.sectionName == webSiteSectionSelected.value }
                .map { it.categoryName }.distinct().first()
            }
          })
        when (fetchWebSitePages.value) {
          LoadingState.LOADING -> {
            Spacer(Modifier.height(6.dp))
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 12.dp))
            Spacer(Modifier.height(6.dp))
          }

          LoadingState.ERROR -> {
            TextButton(onClick = {
              val category =
                webSites.first { it.webSiteName == webSiteSelected.value && it.sectionName == webSiteSectionSelected.value && it.categoryName == webSiteCategorySelected.value }
              if (webSiteSelected.value == RESOURCE_MING_HUI_RADIO && category.sectionName != RESOURCE_MHR_MAIN_SECTION) { //明慧广播源 需要更新页码；希望之声源 不需要
                fetchWebSitePages.value = LoadingState.LOADING
                //启动查询
                coroutineScope.launch {
                  val webSiteInfo = webSiteInfos.first { it.webSiteName == RESOURCE_MING_HUI_RADIO }
                  //清零
                  webSitePages.value = emptyList()
                  webSitePageSelected.value = ""
                  //从新获取
                  var tryTimes = 1
                  while (tryTimes <= TRY_TIMES) {
                    ParseHelper.parsePageValueUpperLimitForMHR(category.toFetchingUrl(webSiteInfo))
                      .fold(onSuccess = { pageUpperLimit ->
                        Logger.i(
                          "CategorySelectionDialog",
                          "parsePageValueUpperLimitForMHR: $pageUpperLimit"
                        )
                        category.pageRange = (1..pageUpperLimit).toList()
                        webSitePages.value =
                          category.pageRange.map { it.toDisplayPageName(webSiteInfo.webSiteName) }
                        webSitePageSelected.value =
                          if (category.pageRange.isEmpty()) "" else category.pageRange.first()
                            .toDisplayPageName(webSiteSelected.value)
                        fetchWebSitePages.value = LoadingState.SUCCESS
                        break
                      }, onFailure = {
                        context.toast(resIdToRealString(R.string.failed_to_retrieve_category_info))
                        fetchWebSitePages.value = LoadingState.ERROR
                      })
                    tryTimes++
                  }
                }
              } else { //希望之声源 不需要，直接赋值
                webSitePages.value =
                  category.pageRange.map { it.toDisplayPageName(webSiteSelected.value) }
                webSitePageSelected.value =
                  if (webSitePages.value.isEmpty()) "" else webSitePages.value.first()
              }
            }) {
              Spacer(Modifier.height(6.dp))
              Text(stringResource(R.string.try_again))
              Spacer(Modifier.height(6.dp))
            }
          }

          LoadingState.SUCCESS -> {
            CategorySelectionSpinner(
              index = 4,
              title = stringResource(R.string.select_program_page),
              options = webSitePages.value.ifEmpty {
                webSites.first { it.webSiteName == webSiteSelected.value && it.sectionName == webSiteSectionSelected.value && it.categoryName == webSiteCategorySelected.value }.pageRange.map {
                  it.toDisplayPageName(
                    webSiteSelected.value
                  )
                }
              },
              onSelect = { selected ->
                webSitePageSelected.value = selected
              },
              applyDefault = {
                webSitePageSelected.value.ifEmpty {
                  val pageRange =
                    webSites.first { it.webSiteName == webSiteSelected.value && it.sectionName == webSiteSectionSelected.value && it.categoryName == webSiteCategorySelected.value }.pageRange
                  if (pageRange.isNotEmpty()) pageRange.first()
                    .toDisplayPageName(webSiteSelected.value)
                  else ""

                }
              })
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          TextButton(
            onClick = { onDismissRequest() },
            modifier = Modifier.padding(8.dp),
          ) {
            Text(stringResource(R.string.dialog_result_cancel))
          }
          TextButton(
            onClick = {
              //保存
              context.setPref(PREF_WEB_SITE_SELECTED, webSiteSelected.value)
              context.setPref(PREF_WEB_SITE_SECTION_SELECTED, webSiteSectionSelected.value)
              context.setPref(PREF_WEB_SITE_CATEGORY_SELECTED, webSiteCategorySelected.value)
              context.setPref(PREF_WEB_SITE_PAGE_SELECTED, webSitePageSelected.value)
              //计算返回值
              val category = webSites.first {
                it.webSiteName == webSiteSelected.value && it.sectionName == webSiteSectionSelected.value && it.categoryName == webSiteCategorySelected.value
              }
              val webSiteInfo = webSiteInfos.find { it.webSiteName == webSiteSelected.value }!!
              onConfirmation(
                CategorySelectionInfo(
                  webSiteInfo, category, webSitePageSelected.value
                )
              )
            },
            modifier = Modifier.padding(8.dp),
          ) {
            Text(stringResource(R.string.dialog_result_ok))
          }
        }
      }
    }
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionSpinner(
  index: Int,
  modifier: Modifier = Modifier,
  title: String,
  options: List<String>,
  onSelect: (String) -> Unit,
  applyDefault: () -> String,
) {
  val context = LocalContext.current
  var expanded by remember { mutableStateOf(false) }
  var selectedOption by remember { mutableStateOf(if (options.isEmpty()) "" else options.first()) }.apply {
    this.value = applyDefault()
  }
  ExposedDropdownMenuBox(
    expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier
  ) {
    TextField(
      modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      value = selectedOption,
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text(title, style = MaterialTheme.typography.labelSmall) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
      colors = ExposedDropdownMenuDefaults.textFieldColors()
    )
    ExposedDropdownMenu(
      expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
          onClick = {
            selectedOption = option
            onSelect(option)
            expanded = false
          },
          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
        )
      }
    }
  }
}


internal enum class LoadingState {
  LOADING,
  ERROR,
  SUCCESS,
}