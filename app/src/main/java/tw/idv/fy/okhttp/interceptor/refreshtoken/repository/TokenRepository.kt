@file:Suppress("unused", "SpellCheckingInspection", "LocalVariableName")

package tw.idv.fy.okhttp.interceptor.refreshtoken.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.plus
import okhttp3.OkHttpClient
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.HttpResponse
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.TimeApiService.Companion.SerialNoDefault
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.gainTimeApiService
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.dispatcher
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.enqueue

class TokenRepository(
    private val mainCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val ioCoroutineScope: CoroutineScope = mainCoroutineScope + Dispatchers.IO
) {

    private companion object {
        /**
         * OkHttpClient singleton (單個 clients)
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
        gainTimeApiService(OkHttpClientSingleton).getToken().enqueue {
            onResponse { call, response ->
                if (!response.isSuccessful) {
                    trySend(IOErrorToken)
                    close(IOErrorToken.e)
                    return@onResponse
                }
                when (val httpResponse = response.body()) {
                    null -> {
                        trySend(IOErrorToken)
                        close(IOErrorToken.e)
                    }
                    else -> {
                        val serialNo = call.request().url.queryParameter("r1") ?: SerialNoDefault
                        val dateTime = httpResponse.dateTime
                        trySend(ValidToken(serialNo, dateTime))
                        close()
                    }
                }
            }
            onFailure { _, e ->
                trySend(IOErrorToken)
                close(e)
            }
        }
        awaitClose()
    }

    sealed class Token (
        /**
         * 流水號: 3位數, 不足左補零
         */
        serialNo: String,
        /**
         * [dateTime] 應該是 yyyy-MM-ddTHH:mm:ss.SSSSSSS (timeapi.io 的 格式)
         */
        dateTime: String
    ) : TemplateData(serialNo, dateTime)

    class ValidToken internal constructor(
        /**
         * 流水號: 3位數, 不足左補零
         */
        override val serialNo: String,
        /**
         * [dateTime] 應該是 yyyy-MM-ddTHH:mm:ss.SSSSSSS (timeapi.io 的 格式)
         */
        override val dateTime: String
    ) : Token(serialNo, dateTime)

    sealed class ErrorToken(val e: Throwable) : Token(SerialNoDefault, HttpResponse.DEFAULT)

    object IOErrorToken : ErrorToken(Throwable("連線有問題，請稍後再試。"))
    object InvalidToken : ErrorToken(Throwable("錯誤代碼 1002，請洽管理員。"))
    object ExpiredToken : ErrorToken(Throwable("錯誤代碼 13，請洽管理員。"))  // 伺服器回覆過期

}