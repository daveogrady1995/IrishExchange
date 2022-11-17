package com.davidogrady.irishexchange.holders

import com.davidogrady.irishexchange.R
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.chat_messages_date_grouper_row.view.*

class ChatMessagesDateGrouper(private val dateTimeText: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_messages_group_datetime.text = dateTimeText
    }

    override fun getLayout(): Int {
        return R.layout.chat_messages_date_grouper_row
    }
}