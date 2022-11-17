package com.davidogrady.irishexchange.models

data class IrishHelperItem(
    val irishPhrase: String,
    val englishPhrase: String) {
    constructor() : this(
        "",
        "")
}