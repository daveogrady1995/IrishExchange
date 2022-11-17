package com.davidogrady.irishexchange.holders

import android.view.View
import android.widget.TextView
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.chat_to_row.view.*

class ChatToItem(val text: String, val time: String, val user: User): Item<ViewHolder>() {

    private var timeTextViewIsVisible = false
    private lateinit var textViewHiddenTime: TextView

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_to_row.text = text

        textViewHiddenTime = viewHolder.itemView.textview_to_row_time_hidden
        textViewHiddenTime.text = time


        Picasso.get()
            .load(user.profileImageUrl)
            .error(R.drawable.ic_user_generic)
            .placeholder(R.drawable.ic_user_generic)
            .centerCrop()
            .resize(200,200)
            .into(viewHolder.itemView.imageview_chat_to_row)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
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