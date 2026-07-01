package org.tpmobile.minghuiradio.util


const val PREF_PROXY_PORT = "pref_proxy_port"
const val PROXY_PORT_HOST = "127.0.0.1"
const val PROXY_PORT_FREEGATE = 8590
const val PROXY_PORT_WUJIE = 19966


const val ANDROID_ASSETS = "file:///android_asset/"

const val PREF_TRY_TIMES = "pref_try_times"
const val PREF_TIME_INTERVAL = "pref_time_interval"
const val DEFAULT_VALUE_TRY_TIMES = 3
const val DEFAULT_VALUE_TIME_INTERVAL = 3

const val TRY_TIMES = 3

const val PREF_RUN_TIMES = "pref_run_times"
const val PREF_RUN_TIMES_FOR_RATIONALE = "pref_run_times_rationale"
const val PREF_PERMISSION_DENIED_PERMANENTLY = "pref_permission_denied_permanently"

const val PREF_WEB_SITE_SELECTED = "pref_web_site_selected"
const val PREF_WEB_SITE_SECTION_SELECTED = "pref_web_site_section_selected"
const val PREF_WEB_SITE_CATEGORY_SELECTED = "pref_web_site_category_selected"
const val PREF_WEB_SITE_PAGE_SELECTED = "pref_web_site_page_selected"

/**
 * 用于处理每个screen都有简单播放器时，通过通知或播放按钮：播放或暂停 时，控制列表item的动画
 * 取消：简化处理。因为动的状态，表示当前正在播放（包括暂停）态的 item
 */
const val PREF_CURRENT_PLAYING = "pref_current_route"

const val RESOURCE_MING_HUI_RADIO = "明慧广播源"
const val RESOURCE_SOUND_OF_HOPE = "希望之声源"

const val RESOURCE_DEFAULT = RESOURCE_MING_HUI_RADIO

const val RESOURCE_MHR_MAIN_SECTION = "主页栏目"
const val RESOURCE_MHR_MAIN_CATEGORY_ZXST = "最新收听"
const val RESOURCE_MHR_MAIN_CATEGORY_RMJM = "热门节目"

/////////////////////////////////////////////////
const val CONNECT_TIMEOUT = 30L //秒
const val READ_TIMEOUT = 30L //秒
const val WRITE_TIMEOUT = 30L //秒
const val DOWNLOAD_TIMEOUT = 10L

/////////////////////////////////////////////////
val COLOR_GOLDEN = "#FFD700"
val COLOR_ORANGE = "#FFA500"
val COLOR_BLUE = "#0000FF"
val COLOR_DARKBLUE = "#FFA500"

/////////////////////////////////////////////////
enum class TimeUnit {
  MSEC,
  SEC,
  MIN,
  HOUR,
  DAY
}

/////////////////////////////////////////////////
/******************** 时间相关常量 (与毫秒的倍数) */
val MSEC = 1
val SEC = 1000
val MIN = 60000
val HOUR = 3600000
val DAY = 86400000

/******************** 存储相关常量(与Byte的倍数)  */
val BYTE = 1
val KB = 1024
val MB = 1048576
val GB = 1073741824
