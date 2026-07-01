package org.tpmobile.minghuiradio.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tpmobile.minghuiradio.MyApp
import org.tpmobile.minghuiradio.data.WebSiteCategory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.random.Random

object FileUtil {
  private val TAG = "FileUtil"

  suspend fun clearFiles(
    files: List<File>, onProgress: ((Int, String) -> Unit)? = null
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    onProgress?.invoke(0, "开始清理历史存档...")
    var index = 0
    try {
      files.filter { it.exists() && it.isDirectory }.forEach { file ->
        file.deleteRecursively()
        index++
        onProgress?.invoke(index * 100 / files.size, "正在清理历史存档...")
      }
      onProgress?.invoke(100, "成功清理历史存档")
      return@withContext Result.success(true)
    } catch (e: Exception) {
      Logger.e(TAG, "clearFiles error: ${e.message}")
      return@withContext Result.failure(e)
    }
  }

  suspend fun clearAllFiles(): Boolean = withContext(Dispatchers.IO) {
    try {
      MyApp.appContext.filesDir.listFiles { it.isDirectory }?.forEach { file ->
        file.deleteRecursively()
      }
      return@withContext true
    } catch (e: Exception) {
      Logger.e(TAG, "clearFiles error: ${e.message}")
      return@withContext false
    }
  }

  suspend fun createSavedPath(context: Context, webSiteCategory: WebSiteCategory): Boolean =
    withContext(Dispatchers.IO) {
      try {
        val filePath = "${context.filesDir}${File.separator}" +
          "${webSiteCategory.webSiteName}${File.separator}" +
          "${webSiteCategory.sectionName}${File.separator}" +
          "${webSiteCategory.categoryName}${File.separator}"
        return@withContext File(filePath).mkdirs()
      } catch (e: Exception) {
        Logger.e(TAG, "clearFiles error: ${e.message}")
        return@withContext false
      }

    }

  fun readFileInAssets(fileName: String): String {
    var content = ""
    BufferedReader(InputStreamReader(MyApp.appContext.assets.open("settings.json"))).use { bufferedReader ->
      content = bufferedReader.readLines().joinToString("\n")
    }
    return content
  }


  /**
   * eg: assets/a/b/c.png filePath = "a/b/c.png"
   */
  fun assetFileToUri(filePath: String): Uri {
    return "file:///android_asset/$filePath".toUri()
  }

  fun getOneUriForArtwork(): Uri {
    return "file:///android_asset/lotus_${Random.nextInt(1, 4)}.png".toUri()
  }
}