package com.example.drinkcourt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Produc (
    @SerialName("id")
    var id: Int? = null,
    @SerialName("uid")
    var uid: String? = null,
    @SerialName("MenuId")
    var menuId: String? = null,
    @SerialName("MenuName")
    var nama: String? = null,
    @SerialName("Qt")
    var quanty: Int? = null,
    @SerialName("Price")
    var harga: Int? = null,
    @SerialName("Note")
    var catatan: String? = null,
    @SerialName("Total")
    var total: Int? = null
)