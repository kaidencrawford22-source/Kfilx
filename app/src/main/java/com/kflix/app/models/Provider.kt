package com.kflix.app.models

import com.kflix.app.adapters.AppAdapter

open class Provider(
    val name: String,
    val logo: String,
    val language: String,

    val provider: com.kflix.app.providers.Provider,
) : AppAdapter.Item {


    override lateinit var itemType: AppAdapter.Type
}