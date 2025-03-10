package com.tcgptrading.tcgptrading

import androidx.compose.runtime.Composable

expect fun onApplicationStartPlatformSpecific()

@Composable
expect fun localContext(): Any?

expect val adBannerId: String
