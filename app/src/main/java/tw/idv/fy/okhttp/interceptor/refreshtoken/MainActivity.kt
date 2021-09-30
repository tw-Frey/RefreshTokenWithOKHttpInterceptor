@file:Suppress("unused")

package tw.idv.fy.okhttp.interceptor.refreshtoken

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import tw.idv.fy.okhttp.interceptor.refreshtoken.databinding.ActivityMainBinding
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.TokenRepository.Token
import tw.idv.fy.okhttp.interceptor.refreshtoken.ui.EmptyItemDecoration
import tw.idv.fy.okhttp.interceptor.refreshtoken.ui.HttpRequestAdapter
import tw.idv.fy.okhttp.interceptor.refreshtoken.viewmodel.MainViewModel
import java.util.*

class MainActivity : AppCompatActivity() {

    private val comparator = Comparator<Pair<String, Token>> { a, b ->
        a.second.dateTime.compareTo(b.second.dateTime)
    }
    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
            .apply {
                lifecycleOwner = this@MainActivity
                viewmodel = mainViewModel
            }.apply {
                val resultArray = mutableListOf<Pair<String, Token>>()
                val resultAdapter = HttpRequestAdapter(resultArray)
                with(recycleView) {
                    addItemDecoration(EmptyItemDecoration())
                    layoutManager = LinearLayoutManager(context)
                    adapter = resultAdapter
                }
                with(mainViewModel) {
                    resultLiveData.observe(this@MainActivity) {
                        resultArray.add(it)
                        //resultArray.sortWith(comparator)
                        resultAdapter.notifyDataSetChanged()
                    }
                    refreshButtonOnClick {
                        resultArray.clear()
                        resultAdapter.notifyDataSetChanged()
                    }
                }
            }
    }
}