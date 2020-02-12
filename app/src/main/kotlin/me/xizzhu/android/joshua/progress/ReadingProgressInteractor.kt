/*
 * Copyright (C) 2020 Xizhi Zhu
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

package me.xizzhu.android.joshua.progress

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import me.xizzhu.android.joshua.core.*
import me.xizzhu.android.joshua.infra.interactors.BaseSettingsAwareInteractor

class ReadingProgressInteractor(private val readingProgressManager: ReadingProgressManager,
                                private val bibleReadingManager: BibleReadingManager,
                                settingsManager: SettingsManager,
                                dispatcher: CoroutineDispatcher = Dispatchers.Default)
    : BaseSettingsAwareInteractor(settingsManager, dispatcher) {
    suspend fun bookNames(): List<String> = bibleReadingManager.currentTranslation()
            .filter { it.isNotEmpty() }.first()
            .let { bibleReadingManager.readBookNames(it) }

    suspend fun readingProgress(): ReadingProgress = readingProgressManager.read()

    suspend fun saveCurrentVerseIndex(verseIndex: VerseIndex) = bibleReadingManager.saveCurrentVerseIndex(verseIndex)
}
