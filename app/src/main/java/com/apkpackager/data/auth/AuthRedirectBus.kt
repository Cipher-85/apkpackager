package com.apkpackager.data.auth

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRedirectBus @Inject constructor() {
    private val _redirects = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val redirects: SharedFlow<Uri> = _redirects

    fun publish(uri: Uri) {
        _redirects.tryEmit(uri)
    }
}
