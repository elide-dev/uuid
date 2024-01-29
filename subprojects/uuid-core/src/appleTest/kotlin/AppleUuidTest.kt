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

package dev.elide.uuid

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CocoaUuidTest {
    @Test
    fun `UUID toString matches NSUUID`() {
        val uuidL = uuid4()
        val nativeUuidString = uuidL.bytes.usePinned {
            NSUUID(it.addressOf(0).reinterpret()).UUIDString
        }.lowercase()
        assertEquals(uuidL.toString(), nativeUuidString)
    }

    @Test
    fun `UUID bytes match NSUUID`() {
        val uuidL = uuid4()
        val nativeUuid = NSUUID(uuidL.toString())
        val nativeBytes = ByteArray(UUID_BYTES)
        nativeBytes.usePinned {
            nativeUuid.getUUIDBytes(it.addressOf(0).reinterpret())
        }
        assertTrue(uuidL.bytes.contentEquals(nativeBytes))
    }

    @Test
    fun `test uuid5`() {
        enumerateUuid5Data { namespace, name, result ->
            assertEquals(result, uuid5Of(namespace, name))
        }
    }

    @Test
    fun `test uuid3`() {
        enumerateUuid3Data { namespace, name, result ->
            assertEquals(result, uuid3Of(namespace, name))
        }
    }
}

private fun enumerateUuid3Data(enumerationLambda: (namespace: Uuid, name: String, result: Uuid) -> Unit) {
    enumerateData("src/commonTest/data/uuid3.txt", enumerationLambda)
}

private fun enumerateUuid5Data(enumerationLambda: (namespace: Uuid, name: String, result: Uuid) -> Unit) {
    enumerateData("src/commonTest/data/uuid5.txt", enumerationLambda)
}

private fun enumerateData(path: String, enumerationLambda: (namespace: Uuid, name: String, result: Uuid) -> Unit) {
    val data = NSData.dataWithContentsOfFile("$PROJECT_DIR_ROOT/$path")!!
    val str = memScoped {
        data.bytes!!.getPointer(this)
            .reinterpret<ByteVar>()
            .readBytes(data.length.toInt())
            .decodeToString()
    }
    for (row in str.split("\n")) {
        if (row.isEmpty()) continue
        val (namespaceStr, name, resultStr) = row.split(",")
        enumerationLambda(uuidFrom(namespaceStr), name, uuidFrom(resultStr))
    }
}
