package tw.idv.fy.okhttp.interceptor.refreshtoken.utils

import kotlinx.coroutines.*

fun Throwable.toCancellationException(): CancellationException = this
    .run { localizedMessage?.takeIf(CharSequence::isNotBlank) ?: message ?: javaClass.simpleName } // 解開這條, message 可以更短 (不含原始Throwable型態)
    .toString().run(::CancellationException)