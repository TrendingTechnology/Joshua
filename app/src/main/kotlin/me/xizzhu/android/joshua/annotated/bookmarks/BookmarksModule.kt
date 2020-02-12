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

package me.xizzhu.android.joshua.annotated.bookmarks

import dagger.Module
import dagger.Provides
import me.xizzhu.android.joshua.ActivityScope
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.annotated.AnnotatedVersesInteractor
import me.xizzhu.android.joshua.annotated.AnnotatedVersesViewModel
import me.xizzhu.android.joshua.annotated.BaseAnnotatedVersesPresenter
import me.xizzhu.android.joshua.annotated.bookmarks.list.BookmarksListPresenter
import me.xizzhu.android.joshua.annotated.toolbar.AnnotatedVersesToolbarInteractor
import me.xizzhu.android.joshua.annotated.toolbar.AnnotatedVersesToolbarPresenter
import me.xizzhu.android.joshua.core.BibleReadingManager
import me.xizzhu.android.joshua.core.Bookmark
import me.xizzhu.android.joshua.core.SettingsManager
import me.xizzhu.android.joshua.core.VerseAnnotationManager

@Module
object BookmarksModule {
    @ActivityScope
    @Provides
    fun provideAnnotatedVersesToolbarInteractor(bookmarkManager: VerseAnnotationManager<Bookmark>): AnnotatedVersesToolbarInteractor<Bookmark> =
            AnnotatedVersesToolbarInteractor(bookmarkManager)

    @ActivityScope
    @Provides
    fun provideSortOrderToolbarPresenter(annotatedVersesToolbarInteractor: AnnotatedVersesToolbarInteractor<Bookmark>): AnnotatedVersesToolbarPresenter<Bookmark> =
            AnnotatedVersesToolbarPresenter(R.string.title_bookmarks, annotatedVersesToolbarInteractor)

    @ActivityScope
    @Provides
    fun provideBookmarksListInteractor(bookmarkManager: VerseAnnotationManager<Bookmark>,
                                       bibleReadingManager: BibleReadingManager,
                                       settingsManager: SettingsManager): AnnotatedVersesInteractor<Bookmark> =
            AnnotatedVersesInteractor(bookmarkManager, bibleReadingManager, settingsManager)

    @ActivityScope
    @Provides
    fun provideBookmarksListPresenter(bookmarksActivity: BookmarksActivity,
                                      navigator: Navigator,
                                      bookmarksListInteractor: AnnotatedVersesInteractor<Bookmark>): BaseAnnotatedVersesPresenter<Bookmark, AnnotatedVersesInteractor<Bookmark>> =
            BookmarksListPresenter(bookmarksActivity, navigator, bookmarksListInteractor)

    @ActivityScope
    @Provides
    fun provideBookmarksViewModel(settingsManager: SettingsManager,
                                  annotatedVersesToolbarInteractor: AnnotatedVersesToolbarInteractor<Bookmark>,
                                  bookmarksListInteractor: AnnotatedVersesInteractor<Bookmark>): AnnotatedVersesViewModel<Bookmark> =
            AnnotatedVersesViewModel(settingsManager, annotatedVersesToolbarInteractor, bookmarksListInteractor)
}
