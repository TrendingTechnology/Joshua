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

package me.xizzhu.android.joshua.reading

import kotlinx.coroutines.channels.first
import kotlinx.coroutines.runBlocking
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.core.BibleReadingManager
import me.xizzhu.android.joshua.core.ReadingProgressManager
import me.xizzhu.android.joshua.core.TranslationManager
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.tests.BaseUnitTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ReadingInteractorTest : BaseUnitTest() {
    @Mock
    private lateinit var readingActivity: ReadingActivity
    @Mock
    private lateinit var navigator: Navigator
    @Mock
    private lateinit var bibleReadingManager: BibleReadingManager
    @Mock
    private lateinit var readingProgressManager: ReadingProgressManager
    @Mock
    private lateinit var translationManager: TranslationManager

    private lateinit var readingInteractor: ReadingInteractor

    @Before
    override fun setup() {
        super.setup()
        readingInteractor = ReadingInteractor(readingActivity, navigator, bibleReadingManager, readingProgressManager, translationManager)
    }

    @Test
    fun testCloseVerseDetailWithDefaultState() {
        runBlocking { assertFalse(readingInteractor.closeVerseDetail()) }
    }

    @Test
    fun testOpenThenCloseVerseDetail() {
        runBlocking {
            val verseIndex = VerseIndex(1, 2, 3)
            readingInteractor.openVerseDetail(verseIndex)

            assertEquals(verseIndex, readingInteractor.observeVerseDetailOpenState().first())
            assertTrue(readingInteractor.closeVerseDetail())
        }
    }
}