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

package me.xizzhu.android.joshua.search.result

import android.view.View
import android.widget.ProgressBar
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.infra.arch.ViewData
import me.xizzhu.android.joshua.search.SearchActivity
import me.xizzhu.android.joshua.tests.BaseUnitTest
import me.xizzhu.android.joshua.tests.MockContents
import me.xizzhu.android.joshua.ui.fadeIn
import me.xizzhu.android.joshua.ui.recyclerview.CommonRecyclerView
import me.xizzhu.android.joshua.ui.recyclerview.TitleItem
import org.mockito.Mock
import org.mockito.Mockito.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchResultListPresenterTest : BaseUnitTest() {
    @Mock
    private lateinit var searchActivity: SearchActivity
    @Mock
    private lateinit var navigator: Navigator
    @Mock
    private lateinit var searchResultInteractor: SearchResultInteractor
    @Mock
    private lateinit var loadingSpinner: ProgressBar
    @Mock
    private lateinit var searchResultListView: CommonRecyclerView

    private lateinit var searchResultViewHolder: SearchResultViewHolder
    private lateinit var searchResultListPresenter: SearchResultListPresenter

    @BeforeTest
    override fun setup() {
        super.setup()

        `when`(searchResultInteractor.settings()).thenReturn(emptyFlow())
        `when`(searchResultInteractor.query()).thenReturn(emptyFlow())

        searchResultViewHolder = SearchResultViewHolder(loadingSpinner, searchResultListView)
        searchResultListPresenter = SearchResultListPresenter(searchActivity, navigator, searchResultInteractor, testDispatcher)
    }

    @Test
    fun testObserveSettings() = testDispatcher.runBlockingTest {
        val settings = listOf(
                ViewData.error(),
                ViewData.loading(),
                ViewData.success(Settings(false, true, 1, true, true))
        )
        `when`(searchResultInteractor.settings()).thenReturn(flow { settings.forEach { emit(it) } })

        searchResultListPresenter.create(searchResultViewHolder)
        with(inOrder(searchResultViewHolder.searchResultListView)) {
            settings.forEach {
                if (ViewData.STATUS_SUCCESS == it.status) {
                    verify(searchResultViewHolder.searchResultListView, times(1)).setSettings(it.data!!)
                }
            }
        }

        searchResultListPresenter.destroy()
    }

    @Test
    fun testObserveQueryWithError() = testDispatcher.runBlockingTest {
        `when`(searchResultInteractor.query()).thenReturn(flowOf(ViewData.error()))

        searchResultListPresenter.create(searchResultViewHolder)

        verify(searchResultListView, never()).setItems(any())

        searchResultListPresenter.destroy()
    }

    @Test
    fun testInstantSearch() = testDispatcher.runBlockingTest {
        val query = "query"
        val verses = MockContents.kjvVerses
        `when`(searchResultInteractor.query()).thenReturn(flowOf(ViewData.loading(query)))
        `when`(searchResultInteractor.search(query)).thenReturn(ViewData.success(verses))
        `when`(searchResultInteractor.bookNames()).thenReturn(ViewData.success(MockContents.kjvBookNames))
        `when`(searchResultInteractor.bookShortNames()).thenReturn(ViewData.success(MockContents.kjvBookShortNames))
        `when`(searchActivity.getString(R.string.toast_verses_searched, verses.size)).thenReturn("")

        searchResultListPresenter.create(searchResultViewHolder)

        with(inOrder(searchResultListView)) {
            verify(searchResultListView, times(1)).setItems(any())
            verify(searchResultListView, times(1)).scrollToPosition(0)
            verify(searchResultListView, times(1)).visibility = View.VISIBLE
        }

        searchResultListPresenter.destroy()
    }

    @Test
    fun testInstantSearchWithException() = testDispatcher.runBlockingTest {
        val query = "query"
        val exception = RuntimeException("Random exception")
        `when`(searchResultInteractor.query()).thenReturn(flowOf(ViewData.loading(query)))
        `when`(searchResultInteractor.search(query)).thenThrow(exception)

        searchResultListPresenter.create(searchResultViewHolder)

        verify(searchResultListView, times(1)).visibility = View.GONE
        verify(searchResultListView, never()).setItems(any())

        searchResultListPresenter.destroy()
    }

    @Test
    fun testSearch() = testDispatcher.runBlockingTest {
        val query = "query"
        val verses = MockContents.kjvVerses
        `when`(searchResultInteractor.query()).thenReturn(flowOf(ViewData.success(query)))
        `when`(searchResultInteractor.search(query)).thenReturn(ViewData.success(verses))
        `when`(searchResultInteractor.bookNames()).thenReturn(ViewData.success(MockContents.kjvBookNames))
        `when`(searchResultInteractor.bookShortNames()).thenReturn(ViewData.success(MockContents.kjvBookShortNames))
        `when`(searchActivity.getString(R.string.toast_verses_searched, verses.size)).thenReturn("")

        searchResultListPresenter.create(searchResultViewHolder)

        with(inOrder(loadingSpinner, searchResultListView)) {
            verify(loadingSpinner, times(1)).fadeIn()
            verify(searchResultListView, times(1)).setItems(any())
            verify(loadingSpinner, times(1)).visibility = View.GONE
        }

        searchResultListPresenter.destroy()
    }

    @Test
    fun testSearchWithException() = testDispatcher.runBlockingTest {
        val query = "query"
        val exception = RuntimeException("Random exception")
        `when`(searchResultInteractor.query()).thenReturn(flowOf(ViewData.success(query)))
        `when`(searchResultInteractor.search(query)).thenThrow(exception)

        searchResultListPresenter.create(searchResultViewHolder)

        with(inOrder(loadingSpinner, searchResultListView)) {
            verify(loadingSpinner, times(1)).fadeIn()
            verify(loadingSpinner, times(1)).visibility = View.GONE
        }
        verify(searchResultListView, never()).setItems(any())

        searchResultListPresenter.destroy()
    }

    @Test
    fun testToSearchItems() = testDispatcher.runBlockingTest {
        `when`(searchResultInteractor.bookNames()).thenReturn(ViewData.success(MockContents.kjvBookNames))
        `when`(searchResultInteractor.bookShortNames()).thenReturn(ViewData.success(MockContents.kjvBookShortNames))

        with(searchResultListPresenter) {
            val query = "query"
            assertEquals(
                    listOf(
                            TitleItem(MockContents.kjvBookNames[0], false),
                            SearchItem(MockContents.kjvVerses[0].verseIndex,
                                    MockContents.kjvBookShortNames[0], MockContents.kjvVerses[0].text.text, query, this::selectVerse),
                            SearchItem(MockContents.kjvVerses[1].verseIndex,
                                    MockContents.kjvBookShortNames[0], MockContents.kjvVerses[1].text.text, query, this::selectVerse)
                    ),
                    listOf(MockContents.kjvVerses[0], MockContents.kjvVerses[1]).toSearchItems(query)
            )
        }
    }
}
