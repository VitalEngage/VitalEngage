package com.eemphasys.vitalconnect.data.models

import java.io.InputStream

data class MediaMessagePreviewItem(
    val uri : String,
    val inputStream : InputStream,
    val name : String?,
    val mimeType : String?
)
