package tw.idv.fy.okhttp.interceptor.refreshtoken.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.view.View
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.HttpRequestRepository
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.HttpRequestRepository.ResponseObject
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.TokenRepository.Token

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private companion object {
        private const val COUNT = 10
    }

    val resultLiveData: LiveData<Pair<ResponseObject, Token>> by lazy {
        MutableLiveData<Pair<ResponseObject, Token>>().apply { httpRequestAll() }
    }

    private var refreshButtonOnClickBlock: ((v: View?) -> Unit)? = null

    @JvmOverloads
    fun refreshButtonOnClick(v: View? = null, block: ((v: View?) -> Unit)? = null) {
        when (block) {
            // 來自 layout 層 -- viewmodel::refreshButtonOnClick
            null -> {
                refreshButtonOnClickBlock?.invoke(v)
                resultLiveData.httpRequestAll()
            }
            // 來自 MainActivity -- refreshButtonOnClick {...}
            else -> refreshButtonOnClickBlock = block
        }
    }

    private fun LiveData<Pair<ResponseObject, Token>>.httpRequestAll() {
        var toast: Toast? = null
        val httpRequestRepository = HttpRequestRepository(viewModelScope)
        repeat(COUNT) {
            viewModelScope.launch {
                httpRequestRepository.httpRequest().collect {
                    (this@httpRequestAll as MutableLiveData<Pair<ResponseObject, Token>>).value = it
                }
            }.invokeOnCompletion {
                @SuppressLint("ShowToast")
                if (it != null) {
                    android.util.Log.w("Faty", it)
                    toast?.cancel()
                    toast = Toast.makeText(
                        app,
                        it.run { localizedMessage ?: message ?: "連線有問題，請稍後再試。" },
                        Toast.LENGTH_SHORT
                    ).apply(Toast::show)
                }
            }
        }
    }
}