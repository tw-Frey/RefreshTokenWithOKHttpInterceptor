package tw.idv.fy.okhttp.interceptor.refreshtoken.repository

import tw.idv.fy.okhttp.interceptor.refreshtoken.api.DateAdapter
import java.util.*

sealed class DataTime(
    /**
     * 流水號: 3位數, 不足左補零
     */
    open val serialNo: String,
    /**
     * [dateTime] 應該是 yyyy-MM-ddTHH:mm:ss.SSSSSSS (timeapi.io 的 格式)
     */
    open val dateTime: String,
    val date: Date = DateAdapter.Instance.fromJson(dateTime) ?: DateAdapter.DEFAULT,
    val nanoSecond: Int = "${dateTime}000000000".take(29).takeLast(9).toInt()
)