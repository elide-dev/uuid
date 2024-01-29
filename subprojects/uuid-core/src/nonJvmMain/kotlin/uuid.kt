/*
 * Copyright (c) 2023-2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package dev.elide.uuid

import kotlin.jvm.JvmInline

/**
 * A RFC4122 UUID
 *
 * @constructor Constructs a new UUID from the given ByteArray
 * @throws IllegalArgumentException, if uuid.count() is not 16
 */
@JvmInline
public actual value class Uuid public actual constructor (public val pair: Pair<Long, Long>): Comparable<Uuid> {
  @Suppress("DEPRECATION")
  public actual constructor(msb: Long, lsb: Long) : this(msb to lsb)

  @Deprecated("Use `uuidOf` instead.", ReplaceWith("uuidOf(uuid)"))
  public constructor(uuidBytes: ByteArray) : this(uuidBytes.bits(0, 8) to uuidBytes.bits(8, 16))

  public actual inline val mostSignificantBits: Long get() = pair.first
  public actual inline val leastSignificantBits: Long get() = pair.second

  public companion object {
    private fun ByteArray.bits(start: Int, end: Int): Long {
      var b = 0L
      var i = start
      do {
        b = (b shl 8) or (get(i).toLong() and 0xff)
      } while (++i < end)
      return b
    }

    /** Creates the [ByteArray] from most and least significant bits */
    public fun fromBits(msb: Long, lsb: Long): ByteArray = ByteArray(UUID_BYTES).also { bytes ->
      require(bytes.count() == UUID_BYTES) {
        "Invalid UUID bytes. Expected $UUID_BYTES bytes; found ${bytes.count()}"
      }
      (7 downTo 0).fold(msb) { x, i ->
        bytes[i] = (x and 0xff).toByte()
        x shr 8
      }
      (15 downTo 8).fold(lsb) { x, i ->
        bytes[i] = (x and 0xff).toByte()
        x shr 8
      }
    }

    /** The ranges of sections of UUID bytes, to be separated by hyphens */
    private val uuidByteRanges: List<IntRange> = listOf(
      0 until 4,
      4 until 6,
      6 until 8,
      8 until 10,
      10 until 16,
    )
  }

  /**
   * Converts the UUID to a UUID string, per RFC4122
   */
  override fun toString(): String {
    val characters = CharArray(UUID_STRING_LENGTH)
    var charIndex = 0
    val uuidBytes = fromBits(mostSignificantBits, leastSignificantBits)

    for (range in uuidByteRanges) {
      for (i in range) {
        val octetPair = uuidBytes[i]
        // convert the octet pair in this byte into 2 characters
        val left = octetPair.toInt().shr(4) and 0b00001111
        val right = octetPair.toInt() and 0b00001111
        characters[charIndex++] = UUID_CHARS[left]
        characters[charIndex++] = UUID_CHARS[right]
      }
      if (charIndex < UUID_STRING_LENGTH) {
        characters[charIndex++] = '-'
      }
    }
    return characters.concatToString()
  }

  /**
   * @return The result of comparing [uuidBytes] between this and [other]
   */
  override actual fun compareTo(other: Uuid): Int {
    val uuidBytes = fromBits(mostSignificantBits, leastSignificantBits)
    val otherBytes = fromBits(other.mostSignificantBits, other.leastSignificantBits)

    for (i in (0 until UUID_BYTES)) {
      val compareResult = uuidBytes[i].compareTo(otherBytes[i])
      if (compareResult != 0) return compareResult
    }
    return 0
  }
}

public actual val Uuid.bytes: ByteArray
  get() = Uuid.fromBits(mostSignificantBits, leastSignificantBits)

public actual val Uuid.variant: Int
  get() = (leastSignificantBits.ushr((64 - (leastSignificantBits ushr 62)).toInt()) and (leastSignificantBits shr 63)).toInt()

public actual val Uuid.version: Int
  get() = ((mostSignificantBits shr 12) and 0x0f).toInt()

/**
 * Set the [Uuid.version] on this big-endian [ByteArray]. The [Uuid.variant] is
 * always set to the RFC 4122 one since this is the only variant supported by
 * the [Uuid] implementation.
 *
 * @return Itself after setting the [Uuid.variant] and [Uuid.version].
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.setVersion(version: Int) = apply {
  this[6] = ((this[6].toInt() and 0x0F) or (version shl 4)).toByte()
  this[8] = ((this[8].toInt() and 0x3F) or 0x80).toByte()
}

public actual fun uuidOf(bytes: ByteArray): Uuid {
  require(bytes.count() == UUID_BYTES) {
    "Invalid UUID bytes. Expected $UUID_BYTES bytes; found ${bytes.count()}"
  }
  @Suppress("DEPRECATION")
  return Uuid(bytes)
}

/** Returns the Int representation of a given UUID character */
private fun halfByteFromChar(char: Char) = when (char) {
  in '0'..'9' -> char.code - 48
  in 'a'..'f' -> char.code - 87
  in 'A'..'F' -> char.code - 55
  else -> null
}

public actual fun uuidFrom(string: String): Uuid {
  require(string.length == UUID_STRING_LENGTH) {
    "Uuid string has invalid length: $string"
  }
  require(UUID_HYPHEN_INDICES.all { string[it] == '-' }) {
    "Uuid string has invalid format: $string"
  }

  val bytes = ByteArray(UUID_BYTES)
  var byte = 0
  for (range in UUID_CHAR_RANGES) {
    var i = range.first
    while (i < range.last) {
      // Collect each pair of UUID chars and their int representations
      val left = halfByteFromChar(string[i++])
      val right = halfByteFromChar(string[i++])
      require(left != null && right != null) {
        "Uuid string has invalid characters: $string"
      }

      // smash them together into a single byte
      bytes[byte++] = (left.shl(4) or right).toByte()
    }
  }

  @Suppress("DEPRECATION")
  return Uuid(bytes)
}

@Suppress("DEPRECATION")
public actual fun uuid4(): Uuid =
  Uuid(getRandomUuidBytes().setVersion(4))
