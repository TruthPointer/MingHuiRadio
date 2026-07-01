package org.tpmobile.minghuiradio.util


import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * G: Era 标志符
 * w: 年中的周数
 * W: 月份中的周数
 * d: 月份中的天数
 * D: 年中的天数
 * F: 月份中的星期
 * E: 星期中的天数
 * a: Am/pm 标记
 * k: 一天中的小时数（0-23）
 * K: am/pm 中的小时数（0-11）
 * h: am/pm 中的小时数（1-12）
 * H: 一天中的小时数（1-24）
 * z: 时区
 * Z: RFC 822 时区
 * s: 秒
 * S: 毫秒
 * HH:mm    15:44
 * h:mm a    3:44 下午
 * HH:mm z    15:44 CST
 * HH:mm Z    15:44 +0800
 * HH:mm zzzz    15:44 中国标准时间
 * HH:mm:ss    15:44:40
 * yyyy-MM-dd    2016-08-12
 * yyyy-MM-dd HH:mm    2016-08-12 15:44
 * yyyy-MM-dd HH:mm:ss    2016-08-12 15:44:40
 * yyyy-MM-dd HH:mm:ss zzzz    2016-08-12 15:44:40 中国标准时间
 * EEEE yyyy-MM-dd HH:mm:ss zzzz    星期五 2016-08-12 15:44:40 中国标准时间
 * yyyy-MM-dd HH:mm:ss.SSSZ    2016-08-12 15:44:40.461+0800
 * yyyy-MM-dd'T'HH:mm:ss.SSSZ    2016-08-12T15:44:40.461+0800
 * yyyy.MM.dd G 'at' HH:mm:ss z    2016.08.12 公元 at 15:44:40 CST
 * K:mm a    3:44 下午
 * EEE, MMM d, ''yy    星期五, 八月 12, '16
 * hh 'o''clock' a, zzzz    03 o'clock 下午, 中国标准时间
 * yyyyy.MMMMM.dd GGG hh:mm aaa    02016.八月.12 公元 03:44 下午
 * EEE, d MMM yyyy HH:mm:ss Z    星期五, 12 八月 2016 15:44:40 +0800
 * yyMMddHHmmssZ    160812154440+0800
 * yyyy-MM-dd'T'HH:mm:ss.SSSZ    2016-08-12T15:44:40.461+0800
 * EEEE 'DATE('yyyy-MM-dd')' 'TIME('HH:mm:ss')' zzzz    星期五 DATE(2016-08-12) TIME(15:44:40) 中国标准时间
 *
 */
object DateUtils {

  /////////////////////////
  val SDF_CN = SimpleDateFormat("yyyy年M月d日", Locale.US)
  val SDF_EN = SimpleDateFormat("yyyy-MM-dd", Locale.US)

  fun sdfEnToSdfCn(dateString: String): String {
    if (!dateString.matches("\\d{4}-\\d{2}-\\d{2}".toRegex()))
      return dateString
    return try {
      val date = SDF_EN.parse(dateString) ?: return dateString
      SDF_CN.format(date)
    } catch (e: ParseException) {
      e.printStackTrace()
      ""
    }
  }

}
