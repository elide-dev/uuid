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

import dev.elide.uuid.Uuid
import dev.elide.uuid.bytes
import dev.elide.uuid.uuidFrom
import dev.elide.uuid.uuidOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * # UUID Serializer
 *
 * Provides integration between [Uuid] and KotlinX Serialization; this serializer should be detected automatically by
 * the compiler plugin, and does not need to be explicitly registered. The serializer offers several internal operating
 * modes, which can be adopted via custom serializer contexts.
 *
 * &nbsp;
 *
 * ## Operating Modes
 *
 * Each operating mode is documented within the [SerializationMode] enum, and defines a way to emit UUIDs to serializer
 * streams. The default mode is [SerializationMode.STRING], which emits UUIDs as well-formed strings with no additional
 * transformation.
 *
 * &nbsp;
 *
 * ## Serialization
 *
 * Serialization behavior is straightforward and simply follows configured settings. This allows easy serialization of
 * UUIDs without additional configuration.
 *
 * ## Deserialization
 *
 * Deserialization is customized to identify (and allow) all possible formats for UUIDs supported as internal operating
 * modes by this codec; this allows easy transition from one mode to another without breakage.
 *
 * &nbsp;
 *
 * ## Customizing Behavior
 *
 * Coming soon.
 */
public object UUIDSerializer : KSerializer<Uuid> {
  /** Default operating mode. */
  private val defaultOperatingMode = SerializationMode.STRING

  /** Active operating mode. */
  private var activeOperatingMode = defaultOperatingMode

  /**
   * ## UUID Codec
   *
   * Interface for UUID operating modes which must know how to serialize and deserialize UUIDs from serialization
   * streams. User code should not need to interact with this interface directly.
   */
  public sealed interface UUIDCodec {
    /**
     * Serialize a UUID.
     *
     * @param encoder The encoder to use.
     * @param uuid The UUID to serialize.
     */
    public fun serialize(encoder: Encoder, uuid: Uuid)

    /**
     * Deserialize a UUID.
     *
     * @param decoder The decoder to use.
     * @return The deserialized UUID.
     */
    public fun deserialize(decoder: Decoder): Uuid

    /** Customized type descriptor for this codec. */
    public val descriptor: SerialDescriptor get() = String.serializer().descriptor
  }

  /**
   * ## Serialization Mode
   *
   * Describes supported serialization modes for UUIDs when used with KotlinX Serialization; each mode is described
   * below. The default mode is [STRING].
   *
   * &nbsp;
   *
   * ### Available Modes
   *
   * - [STRING]: Serialize UUIDs as well-formed strings; in this mode, the UUID is expressed in standard string form,
   *   for example, "ad881322-ae14-48ea-b971-d03a836363c5".
   *
   * - [STRING_UPPERCASE]: Serialize UUIDs as with [STRING], but emit uppercase letters only; this matches behavior of
   *   certain platforms, such as macOS (for example, "1214BD91-2327-4C23-A212-6C3E7C092354").
   *
   * - [BINARY]: Serialize UUIDs as raw binary values; in this mode, the UUID is expressed as a 128-bit array of bytes.
   *   In JSON and other text-based formats, this is encoded as a Base64 string. In length-prefixed binary formats (such
   *   as Protobuf), this is encoded as a 16-byte array.
   */
  public enum class SerializationMode : UUIDCodec {
    /** Serialize UUIDs as well-formed strings. */
    STRING {
      override fun serialize(encoder: Encoder, uuid: Uuid) {
        encoder.encodeInline(descriptor).encodeString(uuid.toString())
      }

      override fun deserialize(decoder: Decoder): Uuid {
        return uuidFrom(decoder.decodeInline(descriptor).decodeString())
      }
    },

    /** Serialize UUIDs as well-formed strings, but with uppercase letters. */
    STRING_UPPERCASE {
      override fun serialize(encoder: Encoder, uuid: Uuid) {
        encoder.encodeInline(descriptor).encodeString(uuid.toString().uppercase())
      }

      override fun deserialize(decoder: Decoder): Uuid {
        return uuidFrom(decoder.decodeInline(descriptor).decodeString())
      }
    },

    /** Serialize UUIDs as raw binary values. */
    BINARY {
      @OptIn(ExperimentalEncodingApi::class)
      override fun serialize(encoder: Encoder, uuid: Uuid) {
        encoder.encodeInline(descriptor).encodeString(Base64.encode(uuid.bytes))
      }

      @OptIn(ExperimentalEncodingApi::class)
      override fun deserialize(decoder: Decoder): Uuid {
        return uuidOf(Base64.decode(decoder.decodeInline(descriptor).decodeString()))
      }
    },
  }

  override val descriptor: SerialDescriptor get() = activeOperatingMode.descriptor
  override fun deserialize(decoder: Decoder): Uuid = activeOperatingMode.deserialize(decoder)
  override fun serialize(encoder: Encoder, value: Uuid): Unit = activeOperatingMode.serialize(
    encoder,
    value,
  )
}
