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

import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import platform.posix.read

@OptIn(UnsafeNumber::class)
internal actual fun getRandomUuidBytes(): ByteArray {
    return bytesWithURandomFd { fd, bytePtr ->
        read(fd, bytePtr, UUID_BYTES.convert())
    }
}