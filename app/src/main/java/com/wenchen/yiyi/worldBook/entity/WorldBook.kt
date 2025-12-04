package com.wenchen.yiyi.worldBook.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorldBook(
    var id: String,
    var worldName: String,
    var worldDesc: String? = null,
    var worldItems: List<WorldBookItem>? = null
)

@JsonClass(generateAdapter = true)
data class WorldBookItem(
    var name: String,
    var desc: String,
)