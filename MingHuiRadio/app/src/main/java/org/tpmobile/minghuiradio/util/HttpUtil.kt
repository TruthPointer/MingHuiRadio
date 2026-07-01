package org.tpmobile.minghuiradio.util


import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.tpmobile.minghuiradio.MyApp
import org.tpmobile.minghuiradio.util.ktx.toHumanReadableSize
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object HttpUtil {
  private const val TAG = "HttpUtil"

  //////////////////////////
  suspend fun downloadFileWithProgress(
    context: Context, urlString: String, onProgress: ((Int, String) -> Unit)? = null
  ): Result<String> = withContext(Dispatchers.IO) {
    // 打开连接
    try {
      onProgress?.invoke(0, "开始下载...")

      val connection = if (MyApp.USE_PROXY)
        URL(urlString).openConnection(MyApp.proxy) as HttpURLConnection
      else
        URL(urlString).openConnection() as HttpURLConnection
      connection.connectTimeout = 15000
      connection.readTimeout = 15000
      connection.requestMethod = "GET"
      connection.connect()

      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        // 转到 catch{} 部分处理了，onProgress?.invoke(-1, "下载失败，详情：服务器响应异常")
        throw kotlin.Exception(Exception("服务器响应错误: ${connection.responseCode} ${connection.responseMessage}"))
      }

      // 准备文件路径
      val fileLength = connection.contentLength
      val fileName = File(urlString).name//"download_${System.currentTimeMillis()}.dat"
      val file = File(context.filesDir, fileName)
      Logger.i(TAG, "fileName = $fileName, fileLength = $fileLength")

      // 流操作
      connection.inputStream.use { input ->
        FileOutputStream(file).use { output ->
          val data = ByteArray(8192) // 8KB buffer
          var total: Long = 0
          var count: Int

          while (input.read(data).also { count = it } != -1 && isActive) {
            total += count
            output.write(data, 0, count)

            // 计算进度并回调
            if (fileLength > 0) {
              val progress = (total * 100 / fileLength).toInt()
              val detail =
                "正在下载[${total.toHumanReadableSize()}/${fileLength.toHumanReadableSize()}]中..."
              onProgress?.invoke(progress, detail)
            } else {
              // 如果服务器没返回长度，只显示已下载大小
              onProgress?.invoke(-2, "已下载：${total.toHumanReadableSize()}")
            }
          }
        }
      }
      onProgress?.invoke(100, "下载成功")
      return@withContext Result.success(file.absolutePath)
    } catch (e: Exception) {
      Logger.e(TAG, "下载文件失败，详情：${e.message}")
      return@withContext Result.failure(e)
    }
  }

  suspend fun downloadFile(
    context: Context, urlString: String
  ): Result<String> = withContext(Dispatchers.IO) {
    // 打开连接
    try {
      val connection = if (MyApp.USE_PROXY)
        URL(urlString).openConnection(MyApp.proxy) as HttpURLConnection
      else
        URL(urlString).openConnection() as HttpURLConnection
      connection.connectTimeout = 15000
      connection.readTimeout = 15000
      connection.requestMethod = "GET"
      connection.connect()

      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        // 转到 catch{} 部分处理了，onProgress?.invoke(-1, "下载失败，详情：服务器响应异常")
        throw kotlin.Exception(Exception("服务器响应错误: ${connection.responseCode} ${connection.responseMessage}"))
      }

      // 准备文件路径
      val fileLength = connection.contentLength
      val fileName = File(urlString).name//"download_${System.currentTimeMillis()}.dat"
      val file = File(context.filesDir, fileName)
      Logger.i(TAG, "fileName = $fileName, fileLength = $fileLength")

      // 流操作
      connection.inputStream.use { input ->
        FileOutputStream(file).use { output ->
          val data = ByteArray(8192) // 8KB buffer
          var count: Int

          while (input.read(data).also { count = it } != -1 && isActive) {
            output.write(data, 0, count)
          }
        }
      }
      return@withContext Result.success(file.absolutePath)
    } catch (e: Exception) {
      Logger.e(TAG, "下载文件失败，详情：${e.message}")
      return@withContext Result.failure(e)
    }
  }

}