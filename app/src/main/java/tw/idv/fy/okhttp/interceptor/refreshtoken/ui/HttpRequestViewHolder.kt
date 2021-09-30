package tw.idv.fy.okhttp.interceptor.refreshtoken.ui

import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.RecyclerView

class HttpRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    init {
        itemView.findViewById<TextView>(android.R.id.text2).gravity = GravityCompat.END
    }
}