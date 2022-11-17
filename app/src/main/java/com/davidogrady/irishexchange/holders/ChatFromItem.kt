package com.davidogrady.irishexchange.holders

import android.view.View
import android.widget.TextView
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.models.User
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.chat_from_row.view.*

class ChatFromItem(val text: String, private val time: String, val user: User): Item<ViewHolder>() {

    private var timeTextViewIsVisible = false
    private lateinit var textViewHiddenTime: TextView

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_from_row.text = text

        textViewHiddenTime = viewHolder.itemView.textview_to_row_time_hidden
        textViewHiddenTime.text = time

       /* Picasso.get()
            .load(user.profileImageUrl)
            .error(R.drawable.ic_user_image_placeholder)
            .centerCrop()
            .fit()
            .into(viewHolder.itemView.imageview_chat_from_row)*/
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }

    fun toggleTimeTextView() {
        if (!timeTextViewIsVisible) {
            textViewHiddenTime.visibility = View.VISIBLE
            timeTextViewIsVisible = true
        }
        else {
            textViewHiddenTime.visibility = View.GONE
            timeTextViewIsVisible = false
        }
    }
}