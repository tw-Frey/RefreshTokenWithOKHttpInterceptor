package tw.idv.fy.okhttp.interceptor.refreshtoken.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.HttpRequestRepository.ResponseObject
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.TokenRepository.Token

class HttpRequestAdapter(private val resultArray: List<Pair<ResponseObject, Token>>) : RecyclerView.Adapter<HttpRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HttpRequestViewHolder =
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
            .run(::HttpRequestViewHolder)

    override fun onBindViewHolder(holder: HttpRequestViewHolder, position: Int) {
        with(holder.itemView) {
            resultArray[position].let {
                findViewById<TextView>(android.R.id.text1).text = String.format(
                    "[%02d] result = %s",
                    position + 1,
                    (it.first.dateTime + "000000000").take(29)
                )
                findViewById<TextView>(android.R.id.text2).text = String.format(
                    "併發流水號 %s\n佇列流水號 %s\nToken = %s/%09d",
                    it.first.serialNo,
                    it.second.serialNo,
                    it.second.date.time,
                    it.second.nanoSecond
                )
            }
        }
    }

    override fun getItemCount(): Int = resultArray.size
}