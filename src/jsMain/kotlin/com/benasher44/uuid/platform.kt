package com.benasher44.uuid

import kotlin.random.Random

internal actual fun getRandomUUIDBytes() = Random.Default.nextBytes(UUID_BYTES)
