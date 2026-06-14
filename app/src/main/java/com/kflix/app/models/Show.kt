package com.kflix.app.models

import com.kflix.app.adapters.AppAdapter

sealed interface Show : AppAdapter.Item {
    var isFavorite: Boolean
}
