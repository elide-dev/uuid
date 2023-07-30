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

package dev.elide.uuid

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class JvmUuidTest {

    @Test
    fun `should set correct version and variant bits`() {
        val uuidL = uuid4()
        val platformUuid = java.util.UUID(uuidL.mostSignificantBits, uuidL.leastSignificantBits)

        assertEquals(4, platformUuid.version())
        assertEquals(2, platformUuid.variant())
    }

    @Test
    fun `should match platform UUID string`() {
        val uuidL = uuid4()
        val platformUuid = java.util.UUID(uuidL.mostSignificantBits, uuidL.leastSignificantBits)

        assertEquals(platformUuid.toString(), uuidL.toString())
    }

    @Test
    fun `should match platform UUID bytes`() {
        val uuidL = uuid4()
        val platformUuid = java.util.UUID.fromString(uuidL.toString())

        assertEquals(platformUuid.mostSignificantBits, uuidL.mostSignificantBits)
        assertEquals(platformUuid.leastSignificantBits, uuidL.leastSignificantBits)
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
    for (row in File("$PROJECT_DIR_ROOT/$path").readLines()) {
        if (row.isEmpty()) continue
        val (namespaceStr, name, resultStr) = row.split(",")
        enumerationLambda(uuidFrom(namespaceStr), name, uuidFrom(resultStr))
    }
}
