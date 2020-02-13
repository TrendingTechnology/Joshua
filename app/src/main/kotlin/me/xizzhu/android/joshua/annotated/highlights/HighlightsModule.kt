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

package me.xizzhu.android.joshua.annotated.highlights

import dagger.Module
import dagger.Provides
import me.xizzhu.android.joshua.ActivityScope
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.annotated.AnnotatedVersesInteractor
import me.xizzhu.android.joshua.annotated.AnnotatedVersesViewModel
import me.xizzhu.android.joshua.annotated.BaseAnnotatedVersesPresenter
import me.xizzhu.android.joshua.annotated.highlights.list.HighlightsListPresenter
import me.xizzhu.android.joshua.annotated.toolbar.AnnotatedVersesToolbarInteractor
import me.xizzhu.android.joshua.annotated.toolbar.AnnotatedVersesToolbarPresenter
import me.xizzhu.android.joshua.core.BibleReadingManager
import me.xizzhu.android.joshua.core.Highlight
import me.xizzhu.android.joshua.core.SettingsManager
import me.xizzhu.android.joshua.core.VerseAnnotationManager

@Module
object HighlightsModule {
    @ActivityScope
    @Provides
    fun provideAnnotatedVersesToolbarInteractor(highlightManager: VerseAnnotationManager<Highlight>): AnnotatedVersesToolbarInteractor<Highlight> =
            AnnotatedVersesToolbarInteractor(highlightManager)

    @ActivityScope
    @Provides
    fun provideSortOrderToolbarPresenter(annotatedVersesToolbarInteractor: AnnotatedVersesToolbarInteractor<Highlight>): AnnotatedVersesToolbarPresenter<Highlight> =
            AnnotatedVersesToolbarPresenter(R.string.title_highlights, annotatedVersesToolbarInteractor)

    @ActivityScope
    @Provides
    fun provideHighlightsListInteractor(highlightManager: VerseAnnotationManager<Highlight>,
                                        bibleReadingManager: BibleReadingManager,
                                        settingsManager: SettingsManager): AnnotatedVersesInteractor<Highlight> =
            AnnotatedVersesInteractor(highlightManager, bibleReadingManager, settingsManager)

    @ActivityScope
    @Provides
    fun provideHighlightsListPresenter(highlightsActivity: HighlightsActivity,
                                       navigator: Navigator,
                                       highlightsListInteractor: AnnotatedVersesInteractor<Highlight>): BaseAnnotatedVersesPresenter<Highlight, AnnotatedVersesInteractor<Highlight>> =
            HighlightsListPresenter(highlightsActivity, navigator, highlightsListInteractor)

    @ActivityScope
    @Provides
    fun provideHighlightsViewModel(settingsManager: SettingsManager,
                                   annotatedVersesToolbarInteractor: AnnotatedVersesToolbarInteractor<Highlight>,
                                   highlightsListInteractor: AnnotatedVersesInteractor<Highlight>): AnnotatedVersesViewModel<Highlight> =
            AnnotatedVersesViewModel(settingsManager, annotatedVersesToolbarInteractor, highlightsListInteractor)
}
