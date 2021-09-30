package tw.idv.fy.okhttp.interceptor.refreshtoken.utils

import okhttp3.Dispatcher
import okhttp3.OkHttpClient

inline fun OkHttpClient.Builder.dispatcher(block: Dispatcher.() -> Unit) =
    Dispatcher().apply(block).run(::dispatcher)