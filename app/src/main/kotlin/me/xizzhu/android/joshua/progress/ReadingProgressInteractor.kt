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

package me.xizzhu.android.joshua.progress

import kotlinx.coroutines.flow.first
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.core.*
import me.xizzhu.android.joshua.ui.BaseLoadingAwareInteractor

class ReadingProgressInteractor(private val readingProgressActivity: ReadingProgressActivity,
                                private val readingProgressManager: ReadingProgressManager,
                                private val bibleReadingManager: BibleReadingManager,
                                private val navigator: Navigator,
                                settingsManager: SettingsManager) : BaseLoadingAwareInteractor(settingsManager, IS_LOADING) {
    suspend fun readCurrentTranslation(): String = bibleReadingManager.observeCurrentTranslation().first()

    suspend fun readBookNames(translationShortName: String): List<String> = bibleReadingManager.readBookNames(translationShortName)

    suspend fun readReadingProgress(): ReadingProgress = readingProgressManager.readReadingProgress()

    suspend fun openChapter(verseIndex: VerseIndex) {
        bibleReadingManager.saveCurrentVerseIndex(verseIndex)
        navigator.navigate(readingProgressActivity, Navigator.SCREEN_READING)
    }
}
