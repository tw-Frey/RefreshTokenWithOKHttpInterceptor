package tw.idv.fy.okhttp.interceptor.refreshtoken.ui

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.RecyclerView
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.HttpRequestRepository.ResponseObject
import tw.idv.fy.okhttp.interceptor.refreshtoken.repository.TokenRepository.Token

class HttpRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val text1 = itemView.findViewById<TextView>(android.R.id.text1)
    private val text2 = itemView.findViewById<TextView>(android.R.id.text2)

    init {
        text1.setShadowLayer(1F, 1F, 1F, Color.BLACK)
        text2.setShadowLayer(1F, 1F, 1F, Color.BLACK)
        text2.gravity = GravityCompat.END
    }

    internal fun bind(responseObject: ResponseObject, token: Token) {
        tokenToTextColor(token)
        text1.text = String.format(
            "[%02d] result = %s",
            bindingAdapterPosition + 1,
            (responseObject.dateTime + "000000000").take(29)
        )
        text2.text = String.format(
            "併發流水號 %s\n佇列流水號 %s\nToken = %s/%09d",
            responseObject.serialNo,
            token.serialNo,
            token.date.time,
            token.nanoSecond
        )
    }

    private fun tokenToTextColor(token: Token) {
        val c = token.dateTime.hashCode().toColorLong()
        text1.setTextColor(c)
        text2.setTextColor(c)
    }

    private companion object {
        fun Int.toColorLong() = toLong().run {
            val r = shr(8).and(0xF).run {
                shl(4).or(this).shl(16)
            }
            val g = shr(4).and(0xF).run {
                shl(4).or(this).shl(8)
            }
            val b = shr(0).and(0xF).run {
                shl(4).or(this).shl(0)
            }
            0xFF000000 or r or g or b
        }.toInt()
    }
}