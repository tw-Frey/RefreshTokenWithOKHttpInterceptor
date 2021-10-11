package tw.idv.fy.okhttp.interceptor.refreshtoken.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.HttpRequestRepository.ResponseObject
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.TokenRepository.Token

class HttpRequestAdapter(private val resultArray: List<Pair<ResponseObject, Token>>) : RecyclerView.Adapter<HttpRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HttpRequestViewHolder =
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
            .run(::HttpRequestViewHolder)

    override fun onBindViewHolder(holder: HttpRequestViewHolder, position: Int) {
        val (responseObject, token) = resultArray[position]
        holder.bind(responseObject, token)
    }

    override fun getItemCount(): Int = resultArray.size
}