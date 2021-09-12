package tw.idv.fy.okhttp.interceptor.refreshtoken

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private companion object {
        private const val COUNT = 15
    }

    val resultLiveData: LiveData<Pair<Int, String>> by lazy {
        MediatorLiveData<Pair<Int, String>>().apply { httpRequestAll() }
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

    private fun LiveData<Pair<Int, String>>.httpRequestAll() {
        val httpRequestRepository = HttpRequestRepository(viewModelScope)
        repeat(COUNT) {
            val liveData = httpRequestRepository.httpRequest(it * 100)
            (this as MediatorLiveData).addSource(liveData, ::setValue)
        }
    }
}