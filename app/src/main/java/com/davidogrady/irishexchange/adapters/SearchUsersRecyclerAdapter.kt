package com.davidogrady.irishexchange.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.models.User
import com.squareup.picasso.Picasso

class SearchUsersRecyclerAdapter(
        private val userClicked: (user: User) -> Unit) :
    RecyclerView.Adapter<SearchUsersRecyclerAdapter.CardHolder>(), View.OnClickListener {

    private var lastClickTime = System.currentTimeMillis()
    private val clickInterval = 2000

    val users: ArrayList<User> = arrayListOf()

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder {
        val userItem = LayoutInflater.from(parent.context).inflate(R.layout.user_row_new_message, parent, false)
        val holder = CardHolder(userItem)
        userItem.setOnClickListener { view: View? ->
            val now = System.currentTimeMillis()
            if (now - lastClickTime > clickInterval) {
                userClicked(users[holder.adapterPosition])
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: CardHolder, position: Int) {
        val user = users[position]

        holder.usernameTextView.text = user.username

        // load image lazily with picasso
        if (user.profileImageUrl.isNotEmpty())
            Picasso.get()
                .load(user.profileImageUrl)
                .error(R.drawable.ic_user_image_placeholder)
                .centerCrop()
                .resize(100, 100)
                .into(holder.profilePhotoImageView)
    }

    override fun onClick(v: View?) {
        return
    }

    inner class CardHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.username_textview_newmessage)
        val profilePhotoImageView: ImageView = itemView.findViewById(R.id.imageview_new_message)
    }
}