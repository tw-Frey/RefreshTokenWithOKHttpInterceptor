package tw.idv.fy.okhttp.interceptor.refreshtoken

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView = findViewById<RecyclerView>(R.id.recycle_view)
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(EmptyItemDecoration())
            adapter = HttpRequestAdapter()
        }
    }
}