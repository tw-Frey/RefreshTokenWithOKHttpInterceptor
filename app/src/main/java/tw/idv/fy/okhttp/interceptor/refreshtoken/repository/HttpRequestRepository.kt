@file:Suppress("unused", "SpellCheckingInspection", "LocalVariableName")

package tw.idv.fy.okhttp.interceptor.refreshtoken.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.HttpResponse
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.TimeApiService.Companion.SerialNoDefault
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.gainTimeApiService
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.TokenRepository.Token
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.enqueue

class HttpRequestRepository(
    private val mainCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val ioCoroutineScope: CoroutineScope = mainCoroutineScope + Dispatchers.IO,
    private val tokenRepository: TokenRepository = TokenRepository(mainCoroutineScope)
) {

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
        .build()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun httpRequest(): Flow<Pair<ResponseObject, Token>> = callbackFlow {
        gainTimeApiService(obtainOkHttpClient).getData().enqueue {
            onResponse { call, response ->
                if (!response.isSuccessful) {
                    onFailure(call, Exception(response.errorBody()?.string()))
                    return@onResponse
                }
                when (val httpResponse = response.body()) {
                    null -> onFailure(call, Exception("httpResponse is null"))
                    else -> {
                        val serialNo = call.request().url.queryParameter("r1") ?: SerialNoDefault
                        val responseObject = ResponseObject(
                            serialNo = serialNo,
                            dateTime = httpResponse.dateTime
                        )
                        val token = response.raw().headers.run {
                            Token(
                                serialNo = this["token_serialNo"] ?: SerialNoDefault,
                                dateTime = this["token_dateTime"] ?: HttpResponse.DEFAULT
                            )
                        }
                        trySend(responseObject to token)
                        close()
                    }
                }
            }
            onFailure { _, e ->
                close(e)
            }
        }
        awaitClose()
    }

    data class ResponseObject (
        /**
         * 流水號: 3位數, 不足左補零
         */
        override val serialNo: String,
        /**
         * [dateTime] 應該是 yyyy-MM-ddTHH:mm:ss.SSSSSSS (timeapi.io 的 格式)
         */
        override val dateTime: String
    ) : TemplateData(serialNo, dateTime)
}