package tw.idv.fy.okhttp.interceptor.refreshtoken.utils

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

inline fun <reified T> gainRetrofitService(
    okHttpClient: OkHttpClient = OkHttpClient(),
    retrofit: Retrofit = Retrofit.Builder().build()
) : T = retrofit.newBuilder().client(okHttpClient).build().create(T::class.java)

inline fun <reified T> Call<T>.enqueue(block: Retrofit2CallbackBuilder<T>.() -> Unit) =
    Retrofit2CallbackBuilder<T>().apply(block).run(::enqueue)

/*open*/ class Retrofit2CallbackBuilder<T> : Callback<T> {

    private var onResponseBlock: ((call: Call<T>, response: Response<T>) -> Unit)? = null
    private var onFailureBlock : ((call: Call<T>, t: Throwable) -> Unit)? = null


    fun onResponse(block: ((call: Call<T>, response: Response<T>) -> Unit)?) {
        onResponseBlock = block
    }

    fun onFailure(block: ((call: Call<T>, t: Throwable) -> Unit)?) {
        onFailureBlock = block
    }

    override fun onResponse(call: Call<T>, response: Response<T>) {
        onResponseBlock?.invoke(call, response)
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        onFailureBlock?.invoke(call, t)
    }

}