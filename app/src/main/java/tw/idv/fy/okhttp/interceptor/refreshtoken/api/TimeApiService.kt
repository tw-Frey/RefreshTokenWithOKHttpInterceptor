package tw.idv.fy.okhttp.interceptor.refreshtoken.api

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.gainRetrofitService

interface TimeApiService {

    companion object {
        const val SerialNoDefault = "000"
        private var serialNoForGetData : Int = SerialNoDefault.toInt()
        internal var serialNoForGetToken: Int = SerialNoDefault.toInt()
    }

    @GET("zone")
    suspend fun getToken(
        @Header("User-Agent") http_agent: String? = System.getProperty("http.agent"),
        @Query("timeZone", encoded = true) timeZone: String = "Asia/Taipei",
        @Query("api", encoded = true) api: String = "TimeApiService/getToken",
        @Query("r1") serialNo: String, // 必填
        @Query("r2") currentTimeMillis: Long // 必填
    ) : HttpResponse

    @GET("coordinate")
    fun getData(
        @Header("User-Agent") http_agent: String? = System.getProperty("http.agent"),
        @Query("api", encoded = true) api: String = "TimeApiService/getData",
        @Query("r1") serialNo: String = "000${++serialNoForGetData}".takeLast(3),
        @Query("r2") currentTimeMillis: Long = System.currentTimeMillis(),
        @Query("latitude") latitude: Double = 24.992444717237387,
        @Query("longitude") longitude: Double = 121.51614369598786
    ): Call<HttpResponse>

}

fun gainTimeApiService(okHttpClient: OkHttpClient = OkHttpClient()) =
    gainRetrofitService<TimeApiService>(okHttpClient, retrofit)

val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("https://www.timeapi.io/api/Time/current/")
    .addConverterFactory(MoshiConverterFactory.create(timeApiMoshi))
    .build()