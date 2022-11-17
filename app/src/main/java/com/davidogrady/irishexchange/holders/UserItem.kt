package com.davidogrady.irishexchange.holders

import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.user_row_new_message.view.*

class UserItem(val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.username_textview_newmessage.text = user.username
        viewHolder.itemView.irish_level_textview_newmessage.text =
            user.userProfile?.irishLevel ?: "Beginner"
        viewHolder.itemView.location_textview_newmessage.text =
            user.userProfile?.location ?: "Location Not Provided"
        viewHolder.itemView.online_status_imaageview_newmessage.apply {
            if (user.userOnline!!)
                setBackgroundResource(R.drawable.ic_green_online_icon)
            else
                setBackgroundResource(R.drawable.ic_red_offline_icon)

        }

        if (user.profileImageUrl.isNotEmpty()) {
            Picasso.get()
                .load(user.profileImageUrl)
                .error(R.drawable.ic_user_generic)
                .placeholder(R.drawable.ic_user_generic)
                .centerCrop()
                .resize(400,400)
                .into(viewHolder.itemView.imageview_new_message)
        }
    }

    override fun getLayout(): Int {
        return R.layout.user_row_new_message
    }
}