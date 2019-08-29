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

package me.xizzhu.android.joshua.core.repository.local.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.runBlocking
import me.xizzhu.android.joshua.core.VerseIndex
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidReadingStorageTest : BaseSqliteTest() {
    private lateinit var androidReadingStorage: AndroidReadingStorage

    @Before
    override fun setup() {
        super.setup()
        androidReadingStorage = AndroidReadingStorage(androidDatabase)
    }

    @Test
    fun testReadDefaultCurrentVerseIndex() {
        runBlocking {
            assertEquals(VerseIndex(0, 0, 0), androidReadingStorage.readCurrentVerseIndex())
        }
    }

    @Test
    fun testSaveThenReadCurrentVerseIndex() {
        runBlocking {
            val expected = VerseIndex(1, 2, 3)
            androidReadingStorage.saveCurrentVerseIndex(expected)
            assertEquals(expected, androidReadingStorage.readCurrentVerseIndex())
        }
    }

    @Test
    fun testSaveOverrideThenReadCurrentVerseIndex() {
        runBlocking {
            val expected = VerseIndex(1, 2, 3)
            androidReadingStorage.saveCurrentVerseIndex(VerseIndex(9, 8, 7))
            androidReadingStorage.saveCurrentVerseIndex(expected)
            assertEquals(expected, androidReadingStorage.readCurrentVerseIndex())
        }
    }

    @Test
    fun testReadDefaultCurrentTranslation() {
        runBlocking {
            assertEquals("", androidReadingStorage.readCurrentTranslation())
        }
    }

    @Test
    fun testSaveThenReadCurrentTranslation() {
        runBlocking {
            val expected = "KJV"
            androidReadingStorage.saveCurrentTranslation(expected)
            assertEquals(expected, androidReadingStorage.readCurrentTranslation())
        }
    }

    @Test
    fun testSaveOverrideThenReadCurrentTranslation() {
        runBlocking {
            val expected = "KJV"
            androidReadingStorage.saveCurrentTranslation("random")
            androidReadingStorage.saveCurrentTranslation(expected)
            assertEquals(expected, androidReadingStorage.readCurrentTranslation())
        }
    }
}
