package com.btl.tinder.data

import coil3.Image

data class UserData(
    var userId: String? = "",
    var name: String? = "",
    var username: String? = "",
    var imageUrl: String? = "",
    var bio: String? = ""
) {
    fun toMap() = mapOf(
        "userId" to userId,
        "name" to name,
        "username" to username,
        "imageUrl" to imageUrl,
        "bio" to bio
    )
}