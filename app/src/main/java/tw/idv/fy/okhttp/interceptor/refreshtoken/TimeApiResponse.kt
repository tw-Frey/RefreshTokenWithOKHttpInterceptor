@file:Suppress("unused", "SpellCheckingInspection", "MemberVisibilityCanBePrivate")

package tw.idv.fy.okhttp.interceptor.refreshtoken

import com.squareup.moshi.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 因為 https://www.timeapi.io/api/Time/current/zone?timeZone=Asia/Taipei
 * 返回 dateTime 的 microsecond 是 7 位數: yyyy-MM-ddTHH:mm:ss.SSSSSSS
 * 為了精準性, 儲存時 保留 dateTime 原始時間字串
 */

val jsonAdapter: JsonAdapter<HttpResponse> = Moshi.Builder().add(DateAdapter()).build().adapter(HttpResponse::class.java)

@JsonClass(generateAdapter = true)
data class HttpResponse(val dateTime: String = DEFAULT) {
    companion object {
        val DEFAULT = DateAdapter.Instance.toJson(DateAdapter.DEFAULT)
    }
}

class DateAdapter {
    companion object {
        val DEFAULT = Date(0)
        val Instance = DateAdapter()
        const val ISODatePattern  = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS"
        const val HttpDatePattern = "EEE, dd MMM yyyy HH:mm:ss zzz"
    }
    @FromJson
    fun fromJson(dateTime: String): Date? = fromJson(dateTime, ISODatePattern)
    /*
     * 沒有用 @JvmOverloads 合併, 係因為 Moshi.JsonAdapter 會發生衝突
     */
    internal fun fromJson(dateTime: String, pattern: String = HttpDatePattern, locale: Locale = Locale.US): Date? = SimpleDateFormat(pattern, locale).parse(dateTime)

    @ToJson
    fun toJson(dateTime: Date): String = toJson(dateTime, ISODatePattern)
    /*
     * 沒有用 @JvmOverloads 合併, 係因為 Moshi.JsonAdapter 會發生衝突
     */
    internal fun toJson(dateTime: Date, pattern: String = HttpDatePattern, locale: Locale = Locale.US): String = (SimpleDateFormat(pattern, locale).format(dateTime) + "000000000").take(29)
}