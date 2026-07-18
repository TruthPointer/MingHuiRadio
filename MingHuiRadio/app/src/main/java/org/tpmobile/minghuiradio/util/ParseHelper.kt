package org.tpmobile.minghuiradio.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.tpmobile.minghuiradio.MyApp
import org.tpmobile.minghuiradio.R
import org.tpmobile.minghuiradio.data.MusicItem
import org.tpmobile.minghuiradio.data.UrlIndexed
import org.tpmobile.minghuiradio.data.WebSiteCategory
import org.tpmobile.minghuiradio.data.WebSiteInfo
import org.tpmobile.minghuiradio.util.ktx.resIdToRealString
import java.util.concurrent.atomic.AtomicInteger

object ParseHelper {
  private const val TAG = "ParseHelper"

  val IDS_OF_MHR = hashMapOf(
    "duanbuoshouting" to "短波收听",
    "xiulianyuandi" to "修炼园地",
    "xinwenshishi" to "新闻时事",
    "yinyuexinshang" to "音乐欣赏",
  )

  fun makeJsoupConnection(url: String): Connection {
    val connection = Jsoup.connect(url)
      //.userAgent()
      .timeout(10000)
    if (MyApp.USE_PROXY) connection.proxy(MyApp.proxy)
    return connection
  }

  ////////////////////////////////////////
  // 解析settings.json
  ////////////////////////////////////////
  fun parseSettingsJson(): List<WebSiteInfo> {
    try {
      val json = FileUtil.readFileInAssets("settings.json")
      val assetsWebSiteInfos: List<WebSiteInfo> =
        Gson().fromJson(json, object : TypeToken<List<WebSiteInfo>>() {}.type)
      if (assetsWebSiteInfos.isEmpty()) return emptyList()

      return assetsWebSiteInfos
    } catch (e: Exception) {
      println(e.message ?: "未知原因")
      return emptyList()
    }
  }

  ////////////////////////////////////////
  // 解析明慧广播官网
  ////////////////////////////////////////
  /**
   * 解析明慧广播官网主页，获取模块、栏目信息
   * @param url = https://www.mhradio.org
   * 其中：
   * 1、主页栏目，
   * （1）programUrls 不为空，url 为 https://www.mhradio.org/showprogram/21062.html
   * （2）pageRange = listOf("1")
   * 2、其它栏目
   *  (1）programUrls 为空
   *  (2）pageRange，待后续访问 分类地址时 https://www.mhradio.org//showcategory/50/1.html 时获取
   */
  suspend fun parseCategoryInfoForMHR(
    webSiteInfo: WebSiteInfo,
    url: String
  ): Result<List<WebSiteCategory>> =
    withContext(Dispatchers.IO) {
      Logger.i(TAG, "fetchMusicItemsOfMainSectionForMHR: url = $url")
      val siteCategories = mutableListOf<WebSiteCategory>()
      try {
        val doc: Document = makeJsoupConnection(url).get()
        val REG_CATEGORY_ID = "/showcategory/(\\d+)/\\d+.html"
        var categoryUrl = ""
        var categoryId = ""
        var categoryName = ""
        if (webSiteInfo.webSiteName == RESOURCE_MING_HUI_RADIO) {
          doc.select("div[id='content']").forEach { div ->
            val sectionName = RESOURCE_MHR_MAIN_SECTION
            val titles = div.select("div[id='detailtitle']")
            val categories = div.select("div[id='detailtitle'] + div[id='detaillist']")
            if (titles.size != categories.size) {
              Logger.e("${webSiteInfo.webSiteName}: 主页栏目的分类名和对应列表数不同！")
              return@forEach
            }
            titles.forEachIndexed { index, elements ->
              categoryName = elements.text()
              val programUrls =
                categories[index].select("#detaillist a").toSet().mapNotNull { element ->
                  element.attr("href")
                }.filter { it.isNotEmpty() }.map { "${webSiteInfo.musicUrlHead}$it" }
                  .toMutableList()
              if (programUrls.isEmpty()) return@forEachIndexed
              siteCategories.add(
                WebSiteCategory(
                  webSiteInfo.webSiteName,
                  sectionName,
                  categoryName,
                  categoryId,
                  listOf(1),
                  programUrls
                )
              )
            }
          }
        }
        IDS_OF_MHR.forEach { (id, sectionName) ->
          doc.select("div[id='${id}'] div[class='categorylist'] a").toList()
            .forEach { element ->
              categoryUrl = element.attr("href")
              categoryName = element.text()
              Regex(REG_CATEGORY_ID).matchEntire(categoryUrl)?.let {
                categoryId = it.groupValues[1]
              }
              if (categoryUrl.contains("/showcategory/") && categoryName.isNotEmpty() && categoryId.isNotEmpty()) {
                siteCategories.add(
                  WebSiteCategory(
                    webSiteInfo.webSiteName,
                    sectionName,
                    categoryName,
                    categoryId
                  )
                )
              }
            }
        }
        Logger.i(
          TAG,
          "parseCategoryInfoForMHR: 找到 siteSections ${siteCategories.size} 个。"
        )
        siteCategories.forEach { category ->
          Logger.i(
            TAG, "ParseSiteSectionsForMHR: [${category.webSiteName}-${category.sectionName}]:" +
              " ${category.categoryName}, ${category.categoryId}"
          )
        }
        return@withContext Result.success(siteCategories)
      } catch (e: Exception) {
        Logger.e(TAG, "[E]ParseSiteSectionsForMHR：${e.message}")
        return@withContext Result.failure(
          Exception(
            resIdToRealString(
              R.string.failed_to_retrieve_categories_for_a_website,
              arrayOf(webSiteInfo.webSiteName, e.message ?: "Unknow reason")
            )
          )
        )
      }
    }

  /**
   * 获取【主页栏目】下分类所有的MP3的 musicItem
   * @param urls 里的 Url　为 : https://www.mhradio.org/showprogram/21065.html
   * @return 返回未能成功获取mp3的连接
   */
  suspend fun fetchMusicItemsOfMainSectionForMHR(
    webSiteInfo: WebSiteInfo,
    webSiteCategory: WebSiteCategory,
    urlIndexedList: List<UrlIndexed>,
    onProgress: (Int) -> Unit
  ): Result<Pair<List<MusicItem>, List<UrlIndexed>>> = withContext(Dispatchers.IO) {
    Logger.i(
      TAG,
      "fetchMusicItemsOfMainSectionForMHR: urls = ${urlIndexedList.joinToString("\r\n") { "${it.index}-${it.url}" }}"
    )
    try {
      //1
      //siteCategory.pageRange = listOf("第1页")
      //2
      val musicItems = mutableListOf<MusicItem>()
      val intermediateUrls = mutableListOf<UrlIndexed>()
      intermediateUrls.addAll(urlIndexedList)
      //1.step1 此时 MusicItem 里的 url 还不是最终可用的 url
      //2. step2 由 ulr 获取音乐的真实的 url
      //批量获取真实url，三次尝试
      //val urlChannel = Channel<String>() // 一个共享的 table（桌子）
      //val urlCount = AtomicInteger()
      var tryTimes = 1
      while (tryTimes <= TRY_TIMES) {
        Logger.i(
          TAG,
          "fetchMusicItemsOfMainSectionForMHR： 第${tryTimes}获取mp3播放连接，本次要获取的数量为 ${intermediateUrls.size} 个"
        )
        fetchRealMp3UrlsForMHR(
          webSiteInfo, 3, /*urlCount,*/ intermediateUrls, /*urlChannel,*/ onProgress = { progress ->
            Logger.i(TAG, "fetchMusicItemsOfMainSectionForMHR: $progress %")
            onProgress(progress)
          }).fold(
          onSuccess = { pair ->
            musicItems.addAll(pair.first)
            if (pair.second.isEmpty()) {
              intermediateUrls.clear()
              break
            }
            intermediateUrls.clear()
            intermediateUrls.addAll(pair.second)
          },
          onFailure = {/*忽略*/ },
        )
        tryTimes++
        //urlCount.set(0)
      }
      if (intermediateUrls.isNotEmpty()) {
        Logger.e(
          TAG,
          "[E]fetchMusicItemsOfMainSectionForMHR: 有 ${intermediateUrls.size} 个mp3连接获取失败！"
        )
      }
      return@withContext Result.success(Pair(musicItems.sortedBy { it.index }, intermediateUrls))
    } catch (e: Exception) {
      Logger.e(TAG, "[E]fetchMusicItemsOfMainSectionForMHR: ${e.message}")
      return@withContext Result.failure(e)
    }
  }

  /**
   * 获取一个 非 【主页栏目】之外的栏目下分类其所有的MP3的 musicItem
   * @param url : https://www.mhradio.org/showcategory/53/1.html
   * @return musicItems,未能成功获取mp3的连接、
   */
  suspend fun fetchMusicItemsOfOtherSectionsForMHR(
    webSiteInfo: WebSiteInfo,
    webSiteCategory: WebSiteCategory,
    url: String,
    onProgress: (Int) -> Unit
  ): Result<Pair<List<MusicItem>, List<UrlIndexed>>> = withContext(Dispatchers.IO) {
    Logger.i(TAG, "fetchMusicItemsOfOtherSectionsForMHR: url = $url")
    try {
      val musicItems = mutableListOf<MusicItem>()
      //1.step1 此时 MusicItem 里的 url 还不是最终可用的 url
      val doc: Document = makeJsoupConnection(url).get()
      //1.1
      val pageUpperLimit = parsePageValueUpperLimitForMHRByDoc(doc)
      webSiteCategory.pageRange = (1..pageUpperLimit).toList()
      onProgress(5)
      //1.2
      val intermediateUrls = doc.select("#detaillist a").toSet().mapNotNull { element ->
        element.attr("href")
      }.filter { it.isNotEmpty() }
        .mapIndexed { index, url -> UrlIndexed(index, "${webSiteInfo.musicUrlHead}$url") }
        .toMutableList()
      if (intermediateUrls.isEmpty()) {
        return@withContext Result.failure(Exception(resIdToRealString(R.string.failed_to_retrieve_data)))
      }
      //2. step2 由 ulr 获取音乐的真实的 url
      //批量获取真实url，三次尝试
      //val urlChannel = Channel<String>()
      //val urlCount = AtomicInteger()
      var tryTimes = 0
      while (tryTimes < TRY_TIMES) {
        fetchRealMp3UrlsForMHR(
          webSiteInfo, 3, /*urlCount,*/ intermediateUrls, /*urlChannel,*/ onProgress = { progress ->
            Logger.i(TAG, "fetchMusicItemsOfOtherSectionsForMHR: $progress %")
            onProgress(5 + progress * 95 / 100)
          }).fold(
          onSuccess = { pair ->
            musicItems.addAll(pair.first)
            if (pair.second.isEmpty()) {
              intermediateUrls.clear()
              break
            }
            intermediateUrls.clear()
            intermediateUrls.addAll(pair.second)
          },
          onFailure = {/*忽略*/ },
        )
        tryTimes++
        //urlCount.set(0)
      }
      if (intermediateUrls.isNotEmpty()) {
        Logger.e(TAG, "有 ${intermediateUrls.size} 个mp3连接获取失败！")
      }
      return@withContext Result.success(Pair(musicItems.sortedBy { it.index }, intermediateUrls))
    } catch (e: Exception) {
      Logger.e(TAG, "[E]fetchMusicItemsOfOtherSectionsForMHR: ${e.message}")
      return@withContext Result.failure(e)
    }
  }


  /**
   * 获取一组 mp3 的播放 musicItem
   * url: https://www.mhradio.org/showprogram/21068.html
   */
  suspend fun fetchRealMp3UrlsForMHR(
    webSiteInfo: WebSiteInfo,
    workerNumber: Int = 3,
    urlIndexedList: List<UrlIndexed>,
    onProgress: (Int) -> Unit
  ): Result<Pair<List<MusicItem>, List<UrlIndexed>>> = withContext(Dispatchers.IO) {
    Logger.i(
      TAG,
      "fetchRealMp3UrlsForMHR: urls = ${urlIndexedList.joinToString("\r\n") { it.url }}"
    )
    try {
      val musicItems = mutableListOf<MusicItem>()
      val failedUrls = mutableListOf<UrlIndexed>()
      val urlChannel = Channel<UrlIndexed>() // 一个共享的 table（桌子）
      val urlCount = AtomicInteger()
      //1.prepare
      repeat(workerNumber) {
        launch(Dispatchers.IO) {
          for (url in urlChannel) {
            parseOneMusicDetailsForMHRByResult(webSiteInfo, url).fold(
              onSuccess = { item ->
                //[未完成]保存 item 到数据库
                Logger.i(TAG, "fetchRealMp3UrlsForMHR 返回一个mp3连接：${item.title} > ${item.url}")
                musicItems.add(item)
              },
              onFailure = { e ->
                Logger.i(
                  TAG,
                  "[E]fetchRealMp3UrlsForMHR 未能获取回一个mp3连接：$url， 详情：${e.message}"
                )
                failedUrls.add(url)
              },
            )
            // 完成后关闭
            val countCompleted = urlCount.addAndGet(1)
            onProgress.invoke(countCompleted * 100 / urlIndexedList.size)
            if (countCompleted == urlIndexedList.size) {
              urlChannel.close()
              Logger.i(TAG, "fetchRealMp3UrlsForMHR urlChannel.close()")
            }
          }
        }
      }
      //send
      urlIndexedList.forEach { urlChannel.send(it) }
      Logger.i(TAG, "fetchRealMp3UrlsForMHR return@withContext")
      return@withContext Result.success(Pair(musicItems, failedUrls))
    } catch (e: Exception) {
      Logger.e(TAG, "[E]fetchRealMp3UrlsForMHR: ${e.message}")
      return@withContext Result.failure(e)
    }
  }

  /**
   * 解析一个 Mp3 的 musicItem
   * @param url https://www.mhradio.org/showprogram/21068.html
   */
  suspend fun parseOneMusicDetailsForMHR(
    webSiteInfo: WebSiteInfo,
    urlIndexed: UrlIndexed
  ): Result<MusicItem> =
    withContext(Dispatchers.IO) {
      Logger.i(TAG, "parseOneMusicDetailsForMHR: url = $urlIndexed")
      try {
        val doc: Document = makeJsoupConnection(urlIndexed.url).get()
        val articleBody = doc.select("#articlebody")
        val name = articleBody.selectFirst("h3")?.text() ?: ""
        val url = articleBody.select("a").filter {
          it.attr("href").endsWith(".mp3", ignoreCase = true)
        }[0].attr("href")

        if (name.isEmpty() || url.isEmpty()) return@withContext Result.failure(Exception("获取失败！"))
        var pubDate = ""
        var duraton = ""
        doc.select("td").forEach { element ->
          if (element.text().contains("发表日期")) {
            pubDate = Regex(" |发表日期：").replace(element.text(), "")
          } else if (element.text().contains("节目长度")) {
            duraton = Regex(" |节目长度：").replace(element.text(), "")
          }
        }
        println("$name, $url, $pubDate, $duraton")
        return@withContext Result.success(
          MusicItem(
            title = name,
            date = pubDate,
            duration = duraton,
            url = webSiteInfo.musicUrlHead + url
          ).also { it.index = urlIndexed.index }
        )
      } catch (e: Exception) {
        Logger.e(TAG, "[E]parseOneMusicDetailsForMHR: ${e.message}")
        return@withContext Result.failure(e)
      }
    }

  /**
   * 解析一个 Mp3 的 musicItem
   * @param url https://www.mhradio.org/showprogram/21068.html
   */
  suspend fun parseOneMusicDetailsForMHRByResult(
    webSiteInfo: WebSiteInfo,
    urlIndexed: UrlIndexed,
  ): Result<MusicItem> =
    withContext(Dispatchers.IO) {
      Logger.i(TAG, "parseOneMusicDetailsForMHRByResult: url = $urlIndexed")
      try {
        val doc: Document = makeJsoupConnection(urlIndexed.url).get()
        val articleBody = doc.select("#articlebody")
        val name = articleBody.selectFirst("h3")?.text() ?: ""
        val url = articleBody.select("a").filter {
          it.attr("href").endsWith(".mp3", ignoreCase = true)
        }[0].attr("href")

        if (name.isEmpty() || url.isEmpty())
          return@withContext Result.failure(Exception("[E]parseOneMusicDetailsForMHRByResult: 获取失败！name or url is empty!"))
        var pubDate = ""
        var duraton = ""
        doc.select("td").forEach { element ->
          if (element.text().contains("发表日期")) {
            pubDate = Regex(" |发表日期：").replace(element.text(), "")
          } else if (element.text().contains("节目长度")) {
            duraton = Regex(" |节目长度：").replace(element.text(), "")
          }
        }
        Logger.i(TAG, "parseOneMusicDetailsForMHRByResult: $name, $url, $pubDate, $duraton")
        return@withContext Result.success(
          MusicItem(
            title = name,
            date = pubDate,
            duration = duraton,
            url = webSiteInfo.musicUrlHead + url
          ).also { it.index = urlIndexed.index }
        )
      } catch (e: Exception) {
        Logger.e(TAG, "[E]parseOneMusicDetailsForMHRByResult: ${e.message}")
        return@withContext Result.failure(e)
      }
    }

  /**
   * 解析明慧广播官网栏目网页，获取分页信息，和当前页的 MusicItem 列表
   * @param url https://www.mhradio.org/showcategory/139/1.html
   */
  suspend fun parsePageValueUpperLimitForMHR(url: String): Result<Int> =
    withContext(Dispatchers.IO) {
      Logger.i(TAG, "parsePageValueUpperLimitForMHR: url = $url")
      try {
        val doc: Document = makeJsoupConnection(url).get()
        val list = doc.select("#navigationbar a").toList()
        val node = list.find { it.text() == "末页" } ?: list.last()
        val href = node.attr("href")
        val upperLimit =
          href.substring(href.lastIndexOf("/") + 1)
            .replace(".html", "").toIntOrNull() ?: 1
        return@withContext Result.success(upperLimit)
      } catch (e: Exception) {
        Logger.e(TAG, "[E]parsePageValueUpperLimitForMHR: ${e.message}")
        return@withContext Result.success(1) //【20260630】Result.failure(e)
      }
    }

  suspend fun parsePageValueUpperLimitForMHRByDoc(doc: Document): Int =
    withContext(Dispatchers.IO) {
      Logger.i(TAG, "parsePageValueUpperLimitForMHRByDoc...")
      try {
        val node = doc.select("#navigationbar a").toList().find { it.text() == "末页" }
          ?: doc.select("#navigationbar a").toList().last()
        val href = node.attr("href")
        val upperLimit =
          href.substring(href.lastIndexOf("/") + 1).replace(".html", "").toIntOrNull()
            ?: 1
        return@withContext upperLimit
      } catch (e: Exception) {
        Logger.e(TAG, "[E]parsePageValueUpperLimitForMHRByDoc: ${e.message}")
        return@withContext 1
      }

    }

  ////////////////////////////////////////
  // 解析希望之声源
  ////////////////////////////////////////
  /**
   * 解析希望之声源主页，获取栏目、分类、年份信息
   * @param url "webSiteUrl": "https://mp3.soundofhope.org/mhradio/",
   * @return SiteCategory 里 programUrls 为空， pageRange 有效。
   */
  suspend fun parseCategoryInfoForMHRInSOH(
    webSiteInfo: WebSiteInfo,
    url: String
  ): Result<List<WebSiteCategory>> =
    withContext(Dispatchers.IO) {
      Logger.i(TAG, "parseCategoryInfoForMHRInSOH: url = $url")
      try {
        val doc: Document = makeJsoupConnection(url).get()
        val splitter = "<h2><font color=\"green\">"
        val siteCategories = mutableListOf<WebSiteCategory>()
        doc.body().html().split(splitter).toList().filter { it.isNotEmpty() }
          .forEach { font ->
            val doc = Jsoup.parse(splitter + font)
            val sectionName = doc.select("font[color='green']").text()
            doc.select("h4").toList().forEach { h4 ->
              val categoryName = h4.selectFirst("font")?.text() ?: ""
              val h4as = h4.select("a")
              if (h4as.isEmpty()) return@forEach //?
              val years = h4as.mapNotNull { it.text().trim().toIntOrNull() }
              val tmp = h4as[0].attr("href").replace("/mhradio/", "")
              val categoryId = tmp.substring(0, tmp.lastIndexOf("/"))
              siteCategories.add(
                WebSiteCategory(
                  webSiteInfo.webSiteName,
                  sectionName,
                  categoryName,
                  categoryId,
                  pageRange = years,
                )
              )
            }
          }
        return@withContext Result.success(siteCategories)
      } catch (e: Exception) {
        Logger.e(TAG, "[E]parseSiteSectionsForMHRInSOH: ${e.message}")
        return@withContext Result.failure(e)
      }
    }

  /**
   * 解析希望之声【年份】页面，获取 MusicItem 的文件名、连接、音乐时长、发布日期
   * @param url https://mp3.soundofhope.org/mhradio/144/2021.html
   */
  suspend fun fetchMusicItemsForMHRInSOH(
    webSiteInfo: WebSiteInfo,
    url: String,
    onProgress: (Int) -> Unit
  ): Result<List<MusicItem>> = withContext(Dispatchers.IO) {
    Logger.i(TAG, "fetchMusicItemsForMHRInSOH: url = $url")
    try {
      val list = mutableListOf<MusicItem>()
      val doc: Document = makeJsoupConnection(url).get()
      val nodes = doc.select("p").toList()
      if (nodes.count() != 3 || nodes[1].childNodeSize() == 0 || nodes[1].childNodeSize() % 3 != 0) {
        Logger.e(TAG, "fetchMusicItemsForMHRInSOH: error 1")
        return@withContext Result.failure(Exception(resIdToRealString(R.string.failed_to_retrieve_data_for_data_format_error)))
      }
      var downloadUrl = ""
      var fileName = ""
      val urlHistory = mutableSetOf<String>()
      val nodeSize = nodes[1].childNodeSize()
      for (index in 0..<nodeSize step 3) {
        Logger.i(
          TAG,
          "fetchMusicItemsForMHRInSOH: $index -> ${nodes[1].childNode(index).outerHtml()}"
        )
        fileName = nodes[1].childNode(index).nodeValue()
        fileName =
          Regex("【天音净乐】[\\w\\W]*天音净乐第").replace(fileName, "【天音净乐】天音净乐第")
        downloadUrl = nodes[1].childNode(index + 1).attr("href")
        if (fileName.isEmpty() || downloadUrl.isEmpty() || urlHistory.contains(downloadUrl)) {
          continue
        }
        urlHistory.add(downloadUrl)
        fileName = patchTitle(fileName)
        val result = parseSOHTitle(fileName) ///!!!PatchFileExtension(result.second)
        list.add(
          MusicItem(
            title = result.second,
            date = DateUtils.sdfEnToSdfCn(result.first),
            duration = result.third,
            url = patchMp3Url(downloadUrl)
          )
        )
        onProgress((index + 3) * 100 / nodeSize)
      }
      return@withContext Result.success(list)
    } catch (e: Exception) {
      Logger.e(TAG, "[E]fetchMusicItemsForMHRInSOH: ${e.message}")
      return@withContext Result.failure(e)
    }
  }

  val TITLE_PATTERN =
    """^(\d{4}-\w{2}-\d{2})\s*(.*?)\s*节目长度：((\d+分)?(\d+秒)?)""".toRegex(RegexOption.MULTILINE)

  /**
   * @return 发布时间-标题-时长
   */
  fun parseSOHTitle(title: String): Triple<String, String, String> {
    val newTitle = Regex("[\\\\/:*?<>|]").replace(title, "").trim()
    val groups = TITLE_PATTERN.matchEntire(newTitle)
    return Triple(
      groups?.groupValues?.get(1) ?: "",
      groups?.groupValues?.get(2) ?: "",
      groups?.groupValues?.get(3) ?: "",
    )
  }

  fun patchMp3Url(url: String): String {
    return if (url.startsWith("http://"))
      url.replaceFirst("http://", "https://")
    else
      url
  }

  ////////////////////////////////////////
  // 解析
  ////////////////////////////////////////
  fun patchTitle(title: String): String {
    var newTitle = title.trim()
    val pos = newTitle.indexOf("|")
    if (pos > 0) newTitle = newTitle.substring(0, pos)
    newTitle = Regex("[\\\\/:*?<>|]").replace(newTitle, "")
    newTitle =
      newTitle.replace("｜", " ").replace("\n", " ").replace("\r", " ").replace("\"", " ")
        .replace("\r\n", " ")
    return Regex("[. ]{2,}").replace(newTitle, " ").trim()
  }

  val PATTER_PAGE = """\s*(.*?)(\d+)\s*(.*?)""".toRegex()
  fun parseDigital(s: String): String {
    return PATTER_PAGE.find(s)!!.groupValues[2]
  }

}