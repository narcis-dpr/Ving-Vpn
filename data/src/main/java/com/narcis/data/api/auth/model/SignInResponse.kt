package com.narcis.data.api.auth.model

import com.google.gson.annotations.SerializedName

data class SignInResponse(
    @SerializedName("jwt")
    val jwt: String
)