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

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.elide.uuid

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.errno
import platform.posix.open

internal fun bytesWithURandomFd(fdLambda: (Int, CPointer<ByteVar>) -> Unit): ByteArray {
    return ByteArray(UUID_BYTES).also { bytes ->
        val fd = open("/dev/urandom", O_RDONLY)
        check(fd != -1) { "Failed to access /dev/urandom: $errno" }
        try {
            bytes.usePinned {
                fdLambda(fd, it.addressOf(0))
            }
        } finally {
            close(fd)
        }
    }
}
