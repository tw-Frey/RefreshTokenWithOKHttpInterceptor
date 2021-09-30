package tw.idv.fy.okhttp.interceptor.refreshtoken.utils

import androidx.annotation.CallSuper
import okhttp3.*
import java.io.IOException

fun OkHttpClient.Builder.dispatcher(block: Dispatcher.() -> Unit) = dispatcher(Dispatcher().apply(block))
fun Call.enqueue(block: OkHttpCallback.() -> Unit) = enqueue(OkHttpCallback().apply(block))

/*open*/ class OkHttpCallback : Callback {

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