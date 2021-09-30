package tw.idv.fy.okhttp.interceptor.refreshtoken.ui

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import androidx.recyclerview.widget.RecyclerView
import tw.idv.fy.okhttp.interceptor.refreshtoken.R


class EmptyItemDecoration : RecyclerView.ItemDecoration() {

    private lateinit var emptyView: View

    override fun onDraw(c: Canvas, recyclerView: RecyclerView, state: RecyclerView.State) {
        with(recyclerView) {
            when (childCount) {
                0 -> {
                    if (!::emptyView.isInitialized) {
                        emptyView = LayoutInflater.from(context).inflate(R.layout.empty_view, this, false)
                        emptyView.measure(makeMeasureSpec(width, EXACTLY), makeMeasureSpec(height, EXACTLY))
                        emptyView.layout(0, 0, width, height)
                    }
                    emptyView.draw(c)
                }
            }
        }
    }
}