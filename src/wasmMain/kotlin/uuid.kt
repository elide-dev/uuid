/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:Suppress("MemberVisibilityCanBePrivate", "DuplicatedCode")

package dev.elide.uuid

// Ranges of non-hyphen characters in a UUID string
internal val UUID_CHAR_RANGES: Array<IntRange> = arrayOf(
    IntRange(0, 8),
    IntRange(9, 13),
    IntRange(14, 18),
    IntRange(19, 23),
    IntRange(24, 36),
)

// Indices of the hyphen characters in a UUID string
internal val UUID_HYPHEN_INDICES = arrayOf(8, 13, 18, 23)

// UUID chars arranged from smallest to largest, so they can be indexed by their byte representations
internal val UUID_CHARS = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

/**
 * A RFC4122 UUID
 *
 * @property uuidBytes The underlying UUID bytes
 * @constructor Constructs a new UUID from the given ByteArray
 * @throws IllegalArgumentException, if uuid.count() is not 16
 */
public actual class Uuid @Deprecated("Use `uuidOf` instead.", ReplaceWith("uuidOf(uuid)")) constructor(internal val uuidBytes: ByteArray) : Comparable<Uuid> {

    @Suppress("DEPRECATION")
    public actual constructor(msb: Long, lsb: Long) : this(fromBits(msb, lsb))

    public actual val mostSignificantBits: Long
        get() = uuidBytes.bits(0, 8)

    public actual val leastSignificantBits: Long
        get() = uuidBytes.bits(8, 16)

    private companion object {
        private fun ByteArray.bits(start: Int, end: Int): Long {
            var b = 0L
            var i = start
            do {
                b = (b shl 8) or (get(i).toLong() and 0xff)
            } while (++i < end)
            return b
        }

        private inline fun <T, R> Array<out T>.fold(initial: R, operation: (acc: R, T) -> R): R {
            var accumulator = initial
            for (element in this) accumulator = operation(accumulator, element)
            return accumulator
        }

        /** Creates the [ByteArray] from most and least significant bits */
        private fun fromBits(msb: Long, lsb: Long): ByteArray {
            val bytes = ByteArray(UUID_BYTES)
            arrayOf(7, 6, 5, 4, 3, 2, 1, 0).fold(msb) { x, i ->
                bytes[i] = (x and 0xff).toByte()
                x shr 8
            }
            arrayOf(15, 14, 13, 12, 11, 10, 9, 8).fold(lsb) { x, i ->
                bytes[i] = (x and 0xff).toByte()
                x shr 8
            }
            return bytes
        }

        /** The ranges of sections of UUID bytes, to be separated by hyphens */
        private val uuidByteRanges: Array<IntRange> = arrayOf(
            IntRange(0, 4),
            IntRange(4, 6),
            IntRange(6, 8),
            IntRange(8, 10),
            IntRange(10, 16),
        )
    }

    /**
     * Converts the UUID to a UUID string, per RFC4122
     */
    override fun toString(): String {
        val characters = CharArray(UUID_STRING_LENGTH)
        var charIndex = 0
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
        val str = ""
        for (character in characters) {
            str.plus(character)
        }
        return str
    }

    /**
     * @return true if other is a UUID and its uuid bytes are equal to this one
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Uuid) return false
        for (i in arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)) {
            if (uuidBytes[i] != other.uuidBytes[i]) return false
        }
        return true
    }

    /**
     * @return The hashCode of the uuid bytes
     */
    override fun hashCode(): Int =
        uuidBytes.hashCode() // should be content hash code

    /**
     * @return The result of comparing [uuidBytes] between this and [other]
     */
    override fun compareTo(other: Uuid): Int {
        for (i in arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)) {
            val compareResult = uuidBytes[i].compareTo(other.uuidBytes[i])
            if (compareResult != 0) return compareResult
        }
        return 0
    }
}

public actual val Uuid.bytes: ByteArray
    get() = uuidBytes

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
internal inline fun ByteArray.setVersion(version: Int): ByteArray {
    this[6] = ((this[6].toInt() and 0x0F) or (version shl 4)).toByte()
    this[8] = ((this[8].toInt() and 0x3F) or 0x80).toByte()
    return this
}

@Suppress("DEPRECATION")
public actual fun uuidOf(bytes: ByteArray): Uuid = Uuid(bytes)

/** Returns the Int representation of a given UUID character */
private fun halfByteFromChar(char: Char) = when (char) {
    in '0'..'9' -> char.toInt() - 48
    in 'a'..'f' -> char.toInt() - 87
    in 'A'..'F' -> char.toInt() - 55
    else -> null
}

public actual fun uuidFrom(string: String): Uuid {
    val bytes = ByteArray(UUID_BYTES)
    var byte = 0
    for (range in UUID_CHAR_RANGES) {
        var i = range.first
        while (i < range.last) {
            // Collect each pair of UUID chars and their int representations
            val left = halfByteFromChar(string[i++])
            val right = halfByteFromChar(string[i++])

            // smash them together into a single byte
            bytes[byte++] = (left!!.shl(4) or right!!).toByte()
        }
    }
    @Suppress("DEPRECATION")
    return Uuid(bytes)
}

@Suppress("DEPRECATION", "CAST_NEVER_SUCCEEDS")
public actual fun uuid4(): Uuid =
    Uuid((getRandomUuidBytes() as ByteArray).setVersion(4))

public external interface TypedByteArray

public external fun getRandomUuidBytes(): TypedByteArray
