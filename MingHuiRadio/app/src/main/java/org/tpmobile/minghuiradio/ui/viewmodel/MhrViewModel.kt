package org.tpmobile.minghuiradio.ui.viewmodel

import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.tpmobile.minghuiradio.MyApp
import org.tpmobile.minghuiradio.R
import org.tpmobile.minghuiradio.data.CategorySelectionInfo
import org.tpmobile.minghuiradio.data.FavoriteItem
import org.tpmobile.minghuiradio.data.MusicItem
import org.tpmobile.minghuiradio.data.UiState
import org.tpmobile.minghuiradio.data.UiStateWithProgress
import org.tpmobile.minghuiradio.data.UrlIndexed
import org.tpmobile.minghuiradio.data.WebSiteCategory
import org.tpmobile.minghuiradio.data.WebSiteInfo
import org.tpmobile.minghuiradio.db.RadioRepository
import org.tpmobile.minghuiradio.util.Logger
import org.tpmobile.minghuiradio.util.PREF_WEB_SITE_CATEGORY_SELECTED
import org.tpmobile.minghuiradio.util.PREF_WEB_SITE_SECTION_SELECTED
import org.tpmobile.minghuiradio.util.PREF_WEB_SITE_SELECTED
import org.tpmobile.minghuiradio.util.ParseHelper
import org.tpmobile.minghuiradio.util.RESOURCE_DEFAULT
import org.tpmobile.minghuiradio.util.RESOURCE_MHR_MAIN_CATEGORY_RMJM
import org.tpmobile.minghuiradio.util.RESOURCE_MHR_MAIN_CATEGORY_ZXST
import org.tpmobile.minghuiradio.util.RESOURCE_MHR_MAIN_SECTION
import org.tpmobile.minghuiradio.util.RESOURCE_MING_HUI_RADIO
import org.tpmobile.minghuiradio.util.RESOURCE_SOUND_OF_HOPE
import org.tpmobile.minghuiradio.util.ext.CoroutineViewModel
import org.tpmobile.minghuiradio.util.ktx.getPref
import org.tpmobile.minghuiradio.util.ktx.resIdToRealString
import org.tpmobile.minghuiradio.util.ktx.toDisplayPageName

class MhrViewModel() : CoroutineViewModel() {

  private val TAG = "MhrViewModel"
  val webSiteInfos = ParseHelper.parseSettingsJson()
  private val repository: RadioRepository = RadioRepository.getInstance()
  private var categorySelectionInfo: CategorySelectionInfo? = null

  init {
    getWebSiteInfos(webSiteInfos)
  }

  ////////////////////////////////
  //
  ////////////////////////////////
  private val _mediaControllerState = MutableStateFlow<UiState<MediaController>>(UiState.Loading)
  val mediaControllerState = _mediaControllerState.asStateFlow()

  fun setMediaControllerState(uiState: UiState<MediaController>) {
    _mediaControllerState.value = uiState
  }

  ////////////////////////////////
  //
  ////////////////////////////////
  private val _newMediaTitleState = MutableStateFlow<String>("")
  val newMediaTitleState = _newMediaTitleState.asStateFlow()

  fun setNewMediaTitleState(title: String) {
    _newMediaTitleState.value = title
  }

  ////////////////////////////////
  //
  ////////////////////////////////
  private val _siteCategories = MutableStateFlow<List<WebSiteCategory>>(emptyList())
  val siteCategories = _siteCategories.asStateFlow()

  fun setSiteCategories(siteCategories: List<WebSiteCategory>) {
    _siteCategories.value = siteCategories
  }

  ////////////////////////////////
  //
  ////////////////////////////////
  private val _musicScreenUiState =
    MutableStateFlow<UiStateWithProgress<Pair<String, List<MusicItem>>>>(
      UiStateWithProgress.Loading(
        0
      )
    )
  val musicScreenUiState = _musicScreenUiState.asStateFlow()
  fun setMusicScreenUiState(uiState: UiStateWithProgress<Pair<String, List<MusicItem>>>) {
    _musicScreenUiState.value = uiState
  }

  ////////////////////////////////
  //
  ////////////////////////////////
  private val _favoriteScreenUiState =
    MutableStateFlow<UiState<List<FavoriteItem>>>(UiState.Loading)
  val favoriteScreenUiState = _favoriteScreenUiState.asStateFlow()
  fun setFavoriteScreenUiState(uiState: UiState<List<FavoriteItem>>) {
    _favoriteScreenUiState.value = uiState
  }

  ////////////////////////////////
  //
  ////////////////////////////////
  fun refreshWebSiteInfos() {
    getWebSiteInfos(webSiteInfos)
  }

  /**
   *  1、明慧广播源：每次运行都从头开始，即从主页栏目开始，因为要展示”最新收听““热门节目”
   *  2、希望之声源：从上次选择分类的开始，但是页面从最近的年份开始
   */
  fun getWebSiteInfos(webSiteInfos: List<WebSiteInfo>): Job {
    return launch(Dispatchers.IO) {
      setMusicScreenUiState(UiStateWithProgress.Loading(0))
      //1、获取 SiteCategories 分类数据
      val list = repository.getAllSiteCategories(webSiteInfos)
      if (list.isEmpty()) {
        setMusicScreenUiState(UiStateWithProgress.Error(resIdToRealString(R.string.failed_to_retrieve_data)))
        return@launch
      }
      //在此忽略进度报告
      list.filter { it.webSiteName == RESOURCE_SOUND_OF_HOPE }
        .forEach { it.pageRange = it.pageRange.sortedDescending() }
      setSiteCategories(list)
      repository.insertSiteCategories(list)
      //2、初始化数据
      val defaultWebSite = MyApp.appContext.getPref(PREF_WEB_SITE_SELECTED, "")
      var resource = defaultWebSite.ifEmpty { RESOURCE_DEFAULT }
      val hasSoh = list.find { it.webSiteName == RESOURCE_SOUND_OF_HOPE } == null
      val hasMhr = list.find { it.webSiteName == RESOURCE_MING_HUI_RADIO } == null
      if (hasSoh && !hasMhr) {
        resource = RESOURCE_SOUND_OF_HOPE
      } else if (!hasSoh && hasMhr) {
        resource = RESOURCE_MING_HUI_RADIO
      }
      //var selectedOption: String
      categorySelectionInfo = if (resource == RESOURCE_MING_HUI_RADIO) {//首页栏目>最新收听/热门节目>1
        val webSiteInfo = webSiteInfos.first { it.webSiteName == RESOURCE_MING_HUI_RADIO }
        val mhrs = list.filter { it.webSiteName == RESOURCE_MING_HUI_RADIO }
        val zxst =
          mhrs.find { it.sectionName == RESOURCE_MHR_MAIN_SECTION && it.categoryName == RESOURCE_MHR_MAIN_CATEGORY_ZXST }
        val rmjm =
          mhrs.find { it.sectionName == RESOURCE_MHR_MAIN_SECTION && it.categoryName == RESOURCE_MHR_MAIN_CATEGORY_RMJM }
        val category = zxst ?: (rmjm ?: mhrs.first())
        val page = category.pageRange[0].toDisplayPageName(category.webSiteName)
        //selectedOption = category.toSelectedOptions() +  page.toDisplayPageName(resource)
        CategorySelectionInfo(webSiteInfo, category, page)
      } else {//RESOURCE_SOUND_OF_HOPE 短波收听>明慧时事>pageRange[0]
        val webSiteInfo = webSiteInfos.first { it.webSiteName == RESOURCE_SOUND_OF_HOPE }
        val sohs = list.filter { it.webSiteName == RESOURCE_SOUND_OF_HOPE }
        val category =
          if (defaultWebSite == RESOURCE_SOUND_OF_HOPE) {
            val defaultWebSiteSection =
              MyApp.appContext.getPref(PREF_WEB_SITE_SECTION_SELECTED, "")
            val defaultWebSiteCategory =
              MyApp.appContext.getPref(PREF_WEB_SITE_CATEGORY_SELECTED, "")
            if (defaultWebSiteSection.isNotEmpty() && defaultWebSiteCategory.isNotEmpty()) {
              sohs.find { it.sectionName == defaultWebSiteSection && it.categoryName == defaultWebSiteCategory }
                ?: sohs.first()
            } else {
              sohs.first()
            }
          } else {
            sohs.first()
          }
        val page = category.pageRange[0].toDisplayPageName(category.webSiteName)
        CategorySelectionInfo(webSiteInfo, category, page)
      }
      fetchMusicList(categorySelectionInfo!!)
    }
  }

  fun fetchMusicList(categorySelection: CategorySelectionInfo): Job {
    categorySelectionInfo = categorySelection
    return launch(Dispatchers.IO) {
      setMusicScreenUiState(UiStateWithProgress.Loading(0))
      val selectedOption =
        categorySelection.webSiteCategory.toSelectedOptions() + categorySelection.page
      if (categorySelection.webSiteCategory.webSiteName == RESOURCE_MING_HUI_RADIO) {
        val result =
          if (categorySelection.webSiteCategory.sectionName == RESOURCE_MHR_MAIN_SECTION) {
            val urlIndexedList =
              categorySelection.webSiteCategory.programUrls.mapIndexed { index, url ->
                UrlIndexed(index, url)
              }
            Logger.i(TAG, "urlIndexedList: ${urlIndexedList.joinToString("\r\n") { "${it.index}-${it.url}" }}")
            ParseHelper.fetchMusicItemsOfMainSectionForMHR(
              categorySelection.webSiteInfo, categorySelection.webSiteCategory, urlIndexedList,
              onProgress = { progress ->
                //进度1
                Logger.i(TAG, "fetchMusicList - fetchMusicItemsOfMainSectionForMHR: $progress %")
                setMusicScreenUiState(UiStateWithProgress.Loading(progress))
              }
            )
          } else { // 短波收听>明慧时事>1
            ParseHelper.fetchMusicItemsOfOtherSectionsForMHR(
              categorySelection.webSiteInfo, categorySelection.webSiteCategory,
              categorySelection.selectedUrl(), onProgress = { progress ->
                //进度2
                Logger.i(TAG, "fetchMusicList - fetchMusicItemsForMHR: $progress %")
                setMusicScreenUiState(UiStateWithProgress.Loading(progress))
              }
            )
          }
        result.fold(
          onSuccess = { pair ->
            //1
            val urls = pair.first.joinToString("\r\n") { it.url }
            Logger.i(TAG, "[$RESOURCE_MING_HUI_RADIO]获取的mp3播放连接：${urls}")
            Logger.i(TAG, "未成功获取mp3播放连接的有：${pair.second.joinToString("\r\n")}")
            //2
            insertMusicDataWithFavoriteState(selectedOption, pair.first)
          },
          onFailure = { e ->
            setMusicScreenUiState(UiStateWithProgress.Error("${e.message}"))
          }
        )
      } else { //RESOURCE_SOUND_OF_HOPE 短波收听>明慧时事>pageRange[0]
        ParseHelper.fetchMusicItemsForMHRInSOH(
          categorySelection.webSiteInfo, categorySelection.selectedUrl(), onProgress = { progress ->
            //进度3
            Logger.i(TAG, "fetchMusicList - fetchMusicItemsForMHRInSOH: $progress %")
            setMusicScreenUiState(UiStateWithProgress.Loading(progress))
          }
        ).fold(
          onSuccess = { items ->
            insertMusicDataWithFavoriteState(selectedOption, items)
          },
          onFailure = { e ->
            setMusicScreenUiState(UiStateWithProgress.Error("${e.message}"))
          }
        )
      }
    }
  }

  fun insertMusicDataWithFavoriteState(selectedOption: String, items: List<MusicItem>): Job {
    return launch(Dispatchers.IO) {
      //更新 favorite 状态
      val favoriteItemUrls = repository.getAllFavoriteItemsWithoutFlow().map { it.url }
      items.forEach {
        it.isFavorite = favoriteItemUrls.contains(it.url)
      }
      //插入
      repository.insertMusicItems(items)
      setMusicScreenUiState(UiStateWithProgress.Data(Pair(selectedOption, items)))
    }
  }

  fun retrieveMusicList() {
    Logger.i(TAG, "retrieveMusicList: categorySelectionInfo = $categorySelectionInfo")
    categorySelectionInfo?.let { fetchMusicList(it) }
  }

  fun getAllMusicItems(): Job {
    return launch(Dispatchers.IO) {
      repository.getAllMusicItems().collectLatest { list ->
        val selectedOption = categorySelectionInfo?.selectedOptions()
          ?: resIdToRealString(R.string.category_record_error)
        setMusicScreenUiState(UiStateWithProgress.Data(Pair(selectedOption, list)))
      }
    }
  }

  fun updateMusicItemPlayState(url: String, isPlaying: Boolean): Job {
    return launch(Dispatchers.IO) {
      repository.updateMusicItemPlayState(url, isPlaying)
    }
  }

  fun updateMusicItemFavorite(url: String, isFavorite: Boolean): Job {
    return launch(Dispatchers.IO) {
      repository.updateMusicItemFavorite(url, isFavorite)
    }
  }

  fun clearAllMusicItemPlayState(): Job {
    return launch(Dispatchers.IO) {
      repository.clearAllMusicItemPlayState()
    }
  }

  ////////////////////////////////
  //
  ////////////////////////////////
  fun insertFavoriteItem(favoriteItem: FavoriteItem): Job {
    return launch(Dispatchers.IO) {
      Logger.i(TAG, "insertFavoriteItem: ${favoriteItem.title}")
      repository.insertFavoriteItem(favoriteItem)
    }
  }

  fun deleteFavoriteItem(favoriteItem: FavoriteItem): Job {
    return launch(Dispatchers.IO) {
      Logger.i(TAG, "deleteFavoriteItem: ${favoriteItem.title}")
      repository.deleteFavoriteItem(favoriteItem)
    }
  }

  fun getAllFavoriteItems(): Job {
    return launch(Dispatchers.IO) {
      repository.getAllFavoriteItems().collectLatest { list ->
        setFavoriteScreenUiState(UiState.Data(list))
      }
    }
  }

  fun clearAllFavoriteItems(): Job {
    return launch(Dispatchers.IO) {
      repository.clearAllFavoriteItems()
    }
  }

  fun updateFavoriteItemPlayState(url: String, isPlaying: Boolean): Job {
    return launch(Dispatchers.IO) {
      repository.updateFavoriteItemPlayState(url, isPlaying)
    }
  }

  fun clearAllFavoriteItemPlayState(): Job {
    return launch(Dispatchers.IO) {
      repository.clearAllFavoriteItemPlayState()
    }
  }

}

