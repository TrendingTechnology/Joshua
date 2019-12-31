/*
 * Copyright (C) 2019 Xizhi Zhu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xizzhu.android.joshua.core.repository.remote

import kotlinx.coroutines.channels.SendChannel
import me.xizzhu.android.joshua.core.VerseIndex

data class RemoteStrongNumberVerses(val verses: Map<VerseIndex, List<Int>>)

data class RemoteStrongNumberWords(val hebrew: Map<Int, String>, val greek: Map<Int, String>)

interface RemoteStrongNumberStorage {
    suspend fun fetchVerses(channel: SendChannel<Int>): RemoteStrongNumberVerses

    suspend fun fetchWords(channel: SendChannel<Int>): RemoteStrongNumberWords
}
