/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

@file:kotlin.jvm.JvmName("UuidUtil")
@file:Suppress("MemberVisibilityCanBePrivate")

package dev.elide.uuid

import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.native.concurrent.SharedImmutable

// Ranges of non-hyphen characters in a UUID string
@SharedImmutable
internal val UUID_CHAR_RANGES: List<IntRange> = listOf(
    0 until 8,
    9 until 13,
    14 until 18,
    19 until 23,
    24 until 36
)

// Indices of the hyphen characters in a UUID string
@SharedImmutable
internal val UUID_HYPHEN_INDICES = listOf(8, 13, 18, 23)

// UUID chars arranged from smallest to largest, so they can be indexed by their byte representations
@SharedImmutable
internal val UUID_CHARS = ('0'..'9') + ('a'..'f')

/**
 * Constructs a "Name-Based" version 3 or 5 [UUID][Uuid].
 *
 * Version 3 and 5 UUIDs are created by combining a name and
 * a namespace using a hash function. This library may provide
 * such hash functions in the future, but it adds a significant
 * maintenance burden to support for native, JS, and JVM. Until then:
 *
 * - Provide a MD5 [UuidHasher] to get a v3 UUID
 * - Provide a SHA-1 [UuidHasher] to get a v5 UUID
 *
 * @param namespace for the "Name-Based" UUID
 * @param name withing the namespace for the "Name-Based" UUID
 * @param hasher interface that implements a hashing algorithm
 * @return New version 3 or 5 [UUID][Uuid].
 * @sample dev.elide.uuid.uuid5Of
 * @see <a href="https://tools.ietf.org/html/rfc4122#section-4.3">RFC 4122: Section 4.3</a>
 */
public fun nameBasedUuidOf(namespace: Uuid, name: String, hasher: UuidHasher): Uuid {
    hasher.update(namespace.bytes)
    hasher.update(name.encodeToByteArray())
    val hashedBytes = hasher.digest()
    hashedBytes[6] = hashedBytes[6]
        .and(0b00001111) // clear the 4 most sig bits
        .or(hasher.version.shl(4).toByte())
    hashedBytes[8] = hashedBytes[8]
        .and(0b00111111) // clear the 2 most sig bits
        .or(-0b10000000) // set 2 most sig to 10
    return uuidOf(hashedBytes.copyOf(UUID_BYTES))
}
