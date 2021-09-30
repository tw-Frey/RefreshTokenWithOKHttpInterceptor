@file:Suppress("unused", "SpellCheckingInspection", "LocalVariableName")

package tw.idv.fy.okhttp.interceptor.refreshtoken.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.HttpResponse
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.TokenRepository.Token
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.enqueue

class HttpRequestRepository(
    private val mainCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val ioCoroutineScope: CoroutineScope = mainCoroutineScope + Dispatchers.IO,
    private val tokenRepository: TokenRepository = TokenRepository(mainCoroutineScope)
) {

    private companion object {
        const val SerialNoDefault = 0
        var SerialNo = SerialNoDefault
    }

    /**
     * 因為有 getter 去 new OkHttpClient, 所以每次 obtainOkHttpClient 都不一樣 (多個 clients)
     */
    private val obtainOkHttpClient: OkHttpClient /**/get() = OkHttpClient.Builder()
        .addNetworkInterceptor {
            val token: Token = runBlocking(ioCoroutineScope.coroutineContext) {
                tokenRepository
                    .fetchTokenRequest()
                    .first()
            }
            with(it) {
                //proceed(
                //    request()
                //        .newBuilder()
                //        .addHeader("start_time", Date().toString())
                //        .build()
                //)
                proceed(request())
                    .newBuilder()
                    .addHeader("token_serialNo", token.serialNo)
                    .addHeader("token_dateTime", token.dateTime)
                    .build()
            }
        }
        //.dispatcher {
        //    maxRequests = 1
        //    maxRequestsPerHost = 1
        //}
        .build()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun httpRequest(): Flow<Pair<String, Token>> = callbackFlow {
        val serialNo = "000${++SerialNo}".takeLast(3)
        val request  = Request.Builder()
            .url("https://www.google.com.tw?r=$serialNo." + System.currentTimeMillis())
            .build()
        obtainOkHttpClient.newCall(request).enqueue {
            onResponse { _, response ->
                response.use { it.headers } // 使用 response.use{} 原因係最後會 response.close() 如此才能立即釋放供下一個 request 使用, PS: body() 實質上 有執行 close()
                    .run {
                        Token(
                            this["token_serialNo"] ?: SerialNoDefault.toString(),
                            this["token_dateTime"] ?: HttpResponse.DEFAULT
                        )
                    }.let {
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