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
import java.util.*

class TokenRepository(
    private val mainCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val ioCoroutineScope: CoroutineScope = mainCoroutineScope + Dispatchers.IO
) {

    private companion object {
        var SerialNo = 1
        /**
         * OkHttpClient singleton
         */
        val OkHttpClientSingleton = OkHttpClient
            .Builder()
            .dispatcher {
                maxRequests = 1
                maxRequestsPerHost = 1
            }
            .build()
    }

    var isTokenExpires: Boolean = true
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchTokenRequest(): Flow<Token> = callbackFlow {
        val serialNo = SerialNo++
        val request  = with(timeApiRequest) {
            url.newBuilder()
               .addQueryParameter("fetchToken", "$serialNo." + System.currentTimeMillis())
               .build()
               .let{
                    newBuilder()
                        .url(it)
                        .build()
               }
        }
        OkHttpClientSingleton.newCall(request).enqueue {
            onResponse { _, response ->
                (response.body?.string() ?: "{}")
                    .apply {
                        android.util.Log.d("Faty", "fetchTokenRequest = (流水編號 $serialNo) $this")
                    }.run {
                        jsonAdapter.fromJson(this)?.dateTime ?: HttpResponse.DEFAULT
                    }.let { dateTime ->
                        trySend(Token(serialNo, dateTime))
                    }
                close()
            }
            onFailure { _, e ->
                close(e)
            }
        }
        awaitClose()
    }

    data class Token(
        val serialNo: Int,
        /**
         * 應該是 yyyy-MM-ddTHH:mm:ss.SSSSSSS (timeapi.io 的 格式)
         */
        val dateTime: String,
        val date: Date = DateAdapter.Instance.fromJson(dateTime) ?: DateAdapter.DEFAULT,
        val nanoSecond: Int = "${dateTime}000000000".takeLast(9).toInt()
    )
}