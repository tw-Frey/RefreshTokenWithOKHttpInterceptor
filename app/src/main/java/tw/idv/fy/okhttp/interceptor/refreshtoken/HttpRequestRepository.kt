@file:Suppress("unused", "SpellCheckingInspection")

package tw.idv.fy.okhttp.interceptor.refreshtoken

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.plus
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

class HttpRequestRepository(
    private val mainCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val ioCoroutineScope: CoroutineScope = mainCoroutineScope + Dispatchers.IO
) {

    private companion object {
        var serialNo = 1
        /**
         * 因為沒有 getter 去 new OkHttpClient, 所以每次 obtainOkHttpClient 都是一樣 (單個 clients)
         */
        private val obtainOkHttpClient: OkHttpClient /*get()*/ = OkHttpClient.Builder()
            .addNetworkInterceptor {
                with(it) {
                    /*proceed(
                        request()
                            .newBuilder()
                            .addHeader("start_time", Date().toString())
                            .build()
                    )*/
                    proceed(request())
                        .newBuilder()
                        .addHeader("token_stamp", DateAdapter.Instance.toJson(Date()))
                        .build()
                }
            }
            .dispatcher {
                maxRequests = 1
                maxRequestsPerHost = 1
            }
            .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun httpRequest(delayInMillisecond: Int = 1000): Flow<Pair<Int, String>> = callbackFlow {
        val serialNo = serialNo++
        val request = Request.Builder()
            //.url("https://deelay.me/$delayInMillisecond/https://www.timeapi.io/api/Time/current/zone?timeZone=Asia/Taipei")
            //.url("https://www.google.com.tw?r=$serialNo." + System.currentTimeMillis())
            .url("https://www.timeapi.io/api/Time/current/zone?timeZone=Asia/Taipei&r=$serialNo." + System.currentTimeMillis())
            //.addHeader("serialNo", serialNo.toString())
            //.addHeader("insert_time", Date().time.toString().takeLast(7))
            .build()
        //ioCoroutineScope.launch {
        //    obtainOkHttpClient.newCall(request)
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
        obtainOkHttpClient.newCall(request).enqueue {
            onResponse { _, response ->
                //(response.body?.string() ?: "{}")
                //(response.use { it.headers }["date"] ?: "Thu, 01 Jan 1970 00:00:00 GMT")
                (response.use{ it.headers }["token_stamp"] ?: HttpResponse.DEFAULT)  // 使用 response.use{} 原因係最後會 response.close() 如此才能立即釋放供下一個 request 使用, PS: body() 實質上 有執行 close()
                    /*.apply {
                        android.util.Log.d("Faty", "(流水編號 $serialNo) $this")
                    }*/.run {
                        //jsonAdapter.fromJson(this)?.dateTime ?: HttpResponse.DEFAULT
                        //DateAdapter.Instance.fromJson(this, DateAdapter.HttpDatePattern) ?: DateAdapter.DEFAULT
                        this
                    }.let {
                        //trySend(serialNo to "${it}000000000".take(29))
                        //trySend(serialNo to DateAdapter.Instance.toJson(it))
                        trySend(serialNo to it)
                    }
                close()
            }
            onFailure { _, e ->
                close(e)
            }
        }
        awaitClose()
    }
}