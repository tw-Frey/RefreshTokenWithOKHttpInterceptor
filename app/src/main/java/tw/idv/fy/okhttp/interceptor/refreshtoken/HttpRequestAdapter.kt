package tw.idv.fy.okhttp.interceptor.refreshtoken

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HttpRequestAdapter(private val resultArray: List<Pair<Int, String>>) : RecyclerView.Adapter<HttpRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HttpRequestViewHolder =
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
            .run(::HttpRequestViewHolder)

    override fun onBindViewHolder(holder: HttpRequestViewHolder, position: Int) {
        with(holder.itemView) {
            resultArray[position].let {
                findViewById<TextView>(android.R.id.text1).text = String.format(
                    "[%02d] Token = %s+%s",
                    position + 1,
                    (DateAdapter.Instance.fromJson(it.second) ?: DateAdapter.DEFAULT).time,
                    it.second.takeLast(4)
                )
                findViewById<TextView>(android.R.id.text2).text = String.format(
                    "流水編號 %03d (%s)",
                    it.first,
                    it.second
                )
            }
        }
    }

    override fun getItemCount(): Int = resultArray.size
}