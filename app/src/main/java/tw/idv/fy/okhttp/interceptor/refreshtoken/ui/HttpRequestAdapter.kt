package tw.idv.fy.okhttp.interceptor.refreshtoken.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.TokenRepository.Token

class HttpRequestAdapter(private val resultArray: List<Pair<String, Token>>) : RecyclerView.Adapter<HttpRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HttpRequestViewHolder =
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
            .run(::HttpRequestViewHolder)

    override fun onBindViewHolder(holder: HttpRequestViewHolder, position: Int) {
        with(holder.itemView) {
            resultArray[position].let {
                findViewById<TextView>(android.R.id.text1).text = String.format(
                    "[%02d] Token = %s/%09d",
                    position + 1,
                    it.second.date.time,
                    it.second.nanoSecond
                )
                findViewById<TextView>(android.R.id.text2).text = String.format(
                    "併發流水號 %s\n佇列流水號 %s",
                    it.first,
                    it.second.serialNo
                )
            }
        }
    }

    override fun getItemCount(): Int = resultArray.size
}