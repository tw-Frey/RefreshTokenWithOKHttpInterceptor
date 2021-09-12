package tw.idv.fy.okhttp.interceptor.refreshtoken

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import tw.idv.fy.okhttp.interceptor.refreshtoken.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private val comparator = Comparator<Pair<Int, String>> { a, b -> a.second.compareTo(b.second) }
    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
            .apply {
                lifecycleOwner = this@MainActivity
                viewmodel = mainViewModel
            }.apply {
                val resultArray = mutableListOf<Pair<Int, String>>()
                val resultAdapter = HttpRequestAdapter(resultArray)
                with(recycleView) {
                    addItemDecoration(EmptyItemDecoration())
                    layoutManager = LinearLayoutManager(context)
                    adapter = resultAdapter
                }
                with(mainViewModel) {
                    resultLiveData.observe(this@MainActivity) {
                        resultArray.add(it)
                        resultArray.sortWith(comparator)
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