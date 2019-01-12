package com.github.rothso.mass.extractor

data class Environment(
    val athenaKey: String,
    val athenaSecret: String,
    val practiceId: Int,
    val previewMode: Boolean
)