package tw.idv.fy.okhttp.interceptor.refreshtoken

import androidx.annotation.CallSuper
import com.squareup.moshi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.plus
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused")
class HttpRequestRepository(
    private val mainCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val ioCoroutineScope: CoroutineScope = mainCoroutineScope + Dispatchers.IO
) {

    private companion object {
        private val jsonAdapter: JsonAdapter<HttpResponse> = Moshi.Builder().add(DateAdapter()).build().adapter(HttpResponse::class.java)
        private var serialNo = 1
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun httpRequest(delayInMillisecond: Int = 5000): Flow<Pair<Int, String>> = callbackFlow {
        val serialNo = serialNo++
        val request = Request.Builder()
            //.url("https://deelay.me/$delayInMillisecond/https://www.timeapi.io/api/Time/current/zone?timeZone=Asia/Taipei")
            .url("https://www.timeapi.io/api/Time/current/zone?timeZone=Asia/Taipei")
            .build()
        //ioCoroutineScope.launch {
        //    OkHttpClient().newCall(request)
        //        .runCatching {
        //            (execute().body?.string() ?: "{}")
        //                /*.apply {
        //                    android.util.Log.d("Faty", "(流水編號 $serialNo) $this")
        //                }*/.run {
        //                    jsonAdapter.fromJson(this)?.dateTime ?: HttpResponse.DEFAULT
        //                }.let {
        //                    trySend(serialNo to "${it}0000000".take(27))
        //                }
        //        }
        //        .onFailure {
        //            android.util.Log.w("Faty", it)
        //        }
        //}
        OkHttpClient().newCall(request).enqueue {
            onResponse { _, response ->
                (response.body?.string() ?: "{}")
                    /*.apply {
                        android.util.Log.d("Faty", "(流水編號 $serialNo) $this")
                    }*/.run {
                        jsonAdapter.fromJson(this)?.dateTime ?: HttpResponse.DEFAULT
                    }.let {
                        trySend(serialNo to "${it}0000000".take(27))
                    }
            }
        }
        awaitClose()
    }
}

fun Call.enqueue(block: OkHttpCallback.() -> Unit) = enqueue(OkHttpCallback().apply(block))

@Suppress("unused")
open class OkHttpCallback : Callback {

    private var onResponseBlock: ((call: Call, response: Response) -> Unit)? = null
    private var onFailureBlock: ((call: Call, e: IOException) -> Unit)? = null

    fun onResponse(block: (call: Call, response: Response) -> Unit) {
        onResponseBlock = block
    }

    fun onFailure(block: (call: Call, e: IOException) -> Unit) {
        onFailureBlock = block
    }

    @CallSuper
    override fun onResponse(call: Call, response: Response) {
        onResponseBlock?.invoke(call, response)
    }

    @CallSuper
    override fun onFailure(call: Call, e: IOException) {
        onFailureBlock?.invoke(call, e)
    }
}

@JsonClass(generateAdapter = true)
data class HttpResponse(val dateTime: String = DEFAULT) {
    companion object {
        val DEFAULT = DateAdapter.Instance.toJson(DateAdapter.DEFAULT)
    }
}

@Suppress("unused")
class DateAdapter {
    companion object {
        val DEFAULT = Date(0)
        val Instance = DateAdapter()
    }
    @FromJson
    fun fromJson(dateTime: String): Date? = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.TAIWAN).parse(dateTime)
    @ToJson
    fun toJson(dateTime: Date): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.TAIWAN).format(dateTime)
}