package tw.idv.fy.okhttp.interceptor.refreshtoken

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HttpRequestAdapter : RecyclerView.Adapter<HttpRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HttpRequestViewHolder =
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
            .run(::HttpRequestViewHolder)

    override fun onBindViewHolder(holder: HttpRequestViewHolder, position: Int) {
        (holder.itemView as TextView).text = position.toString()
    }

    override fun getItemCount(): Int = 0
}