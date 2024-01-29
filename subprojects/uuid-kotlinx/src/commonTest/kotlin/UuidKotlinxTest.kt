/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package dev.elide.uuid

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UuidKotlinxTest {
  @Serializable data class SampleDataClass(
    val id: Int,
    @Serializable(with = UUIDSerializer::class) val uuid: Uuid,
  )

  @Test fun uuid_expectations() {
    val uuid = uuid4()
    assertNotNull(
      uuid,
      "should not get `null` from generating a uuid via `uuid4`",
    )
    assertNotEquals(
      "",
      uuid.toString(),
      "should not get empty string from generating a uuid via `uuid4`",
    )
  }

  @Test fun serialize_uuid_json() {
    val uuid = uuid4()
    val uuidString = uuid.toString()
    val dataclass = SampleDataClass(id = 123, uuid = uuid)
    val serialized = Json.encodeToString(SampleDataClass.serializer(), dataclass)
    assertNotNull(serialized, "should not get `null` from JSON serialization test")
    assertNotEquals("", serialized, "should not get empty string from JSON serialization test")
    assertTrue(serialized.contains(uuidString), "should find uuid string in serialized JSON")
  }

  @Test fun deserialize_uuid_json() {
    val uuid = uuid4()
    val uuidString = uuid.toString()
    val dataclass = SampleDataClass(id = 123, uuid = uuid)
    val serialized = Json.encodeToString(SampleDataClass.serializer(), dataclass)
    assertNotNull(serialized, "should not get `null` from JSON serialization test")
    assertNotEquals("", serialized, "should not get empty string from JSON serialization test")
    assertTrue(serialized.contains(uuidString), "should find uuid string in serialized JSON")

    // now, deserialize and check
    val deserialized = Json.decodeFromString(SampleDataClass.serializer(), serialized)
    assertNotNull(deserialized, "should not get `null` from JSON de-serialization test")
    assertTrue(deserialized.uuid == uuid, "should find uuid string in serialized JSON")
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Test fun serialize_uuid_proto() {
    val uuid = uuid4()
    val dataclass = SampleDataClass(id = 123, uuid = uuid)
    val serialized = ProtoBuf.encodeToByteArray(SampleDataClass.serializer(), dataclass)
    assertNotNull(serialized, "should not get `null` from JSON serialization test")
    assertTrue(serialized.isNotEmpty(), "serialized byte array for proto should not be empty")
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Test fun deserialize_uuid_proto() {
    val uuid = uuid4()
    val dataclass = SampleDataClass(id = 123, uuid = uuid)
    val serialized = ProtoBuf.encodeToByteArray(SampleDataClass.serializer(), dataclass)
    assertNotNull(serialized, "should not get `null` from JSON serialization test")
    assertTrue(serialized.isNotEmpty(), "serialized byte array for proto should not be empty")

    // now, deserialize and check
    val deserialized = ProtoBuf.decodeFromByteArray(SampleDataClass.serializer(), serialized)
    assertNotNull(deserialized, "should not get `null` from JSON de-serialization test")
    assertTrue(deserialized.uuid == uuid, "should find uuid string in serialized proto")
  }
}
