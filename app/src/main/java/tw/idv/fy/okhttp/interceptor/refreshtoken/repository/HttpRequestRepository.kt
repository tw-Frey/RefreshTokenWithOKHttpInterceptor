@file:Suppress("unused", "SpellCheckingInspection", "LocalVariableName")

package tw.idv.fy.okhttp.interceptor.refreshtoken.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.HttpResponse
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.TimeApiService.Companion.SerialNoDefault
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.gainTimeApiService
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.TokenRepository.*
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.enqueue
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.toCancellationException

class HttpRequestRepository(
    private val mainCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val ioCoroutineScope: CoroutineScope = mainCoroutineScope + Dispatchers.IO,
    private val tokenRepository: TokenRepository = TokenRepository(mainCoroutineScope)
) {

    /**
     * 因為有 getter 去 new OkHttpClient, 所以每次 obtainOkHttpClient 都不一樣 (多個 clients)
     */
    private val obtainOkHttpClient: OkHttpClient /**/get() = OkHttpClient.Builder()
        .addInterceptor {
            val request = it.request()
            android.util.Log.v("Faty", "addNetworkInterceptor: r1=${request.url.queryParameter("r1")}=&r2=${request.url.queryParameter("r2")}")
            val token: Token = runBlocking(ioCoroutineScope.coroutineContext) {
                tokenRepository
                    .fetchTokenRequest()
                    .first()
            }
            with(it) {
                when (token) {
                    is ErrorToken -> {
                        val msg = token.e.run { localizedMessage ?: message ?: javaClass.simpleName }
                        Response.Builder()
                            .request(request())
                            .protocol(Protocol.HTTP_2)
                            .code(600)
                            .message(msg)
                            .body(msg.toResponseBody())
                            .build()
                    }
                    else -> {
                        proceed(request())
                            .newBuilder()
                            .addHeader("token_serialNo", token.serialNo)
                            .addHeader("token_dateTime", token.dateTime)
                            .build()
                    }
                }
            }
        }
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            android.util.Log.d("Faty", "addNetworkInterceptor: r1=${request.url.queryParameter("r1")}=&r2=${request.url.queryParameter("r2")}")
            chain.proceed(request)
        }
        .build()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun httpRequest(): Flow<Pair<ResponseObject, Token>> = callbackFlow {
        val call = gainTimeApiService(obtainOkHttpClient).getData()
        android.util.Log.v("Faty", "fetchTokenRequest: r1=${call.request().url.queryParameter("r1")}=&r2=${call.request().url.queryParameter("r2")}")
        call.enqueue {
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
                            ValidToken(
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
                close(e.toCancellationException())
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