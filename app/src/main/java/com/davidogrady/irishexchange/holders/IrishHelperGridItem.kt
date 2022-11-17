package com.davidogrady.irishexchange.holders

import com.davidogrady.irishexchange.R
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.irish_helper_grid_item.view.*

class IrishHelperGridItem(val irishText: String, private val englishText: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_irishhelper_irish_text.text = irishText
        viewHolder.itemView.textview_irishhelper_english_text.text = englishText
    }

    override fun getLayout(): Int {
        return R.layout.irish_helper_grid_item
    }
}