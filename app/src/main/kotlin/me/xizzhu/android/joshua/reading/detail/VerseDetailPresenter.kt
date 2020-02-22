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

package me.xizzhu.android.joshua.reading.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Highlight
import me.xizzhu.android.joshua.core.StrongNumber
import me.xizzhu.android.joshua.core.Verse
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.infra.activity.BaseSettingsPresenter
import me.xizzhu.android.joshua.infra.arch.ViewData
import me.xizzhu.android.joshua.infra.arch.ViewHolder
import me.xizzhu.android.joshua.infra.arch.onEach
import me.xizzhu.android.joshua.infra.arch.onEachSuccess
import me.xizzhu.android.joshua.reading.ReadingActivity
import me.xizzhu.android.joshua.reading.ReadingViewModel
import me.xizzhu.android.joshua.reading.verse.toStringForSharing
import me.xizzhu.android.joshua.strongnumber.StrongNumberListActivity
import me.xizzhu.android.joshua.ui.*
import me.xizzhu.android.logger.Log
import java.lang.StringBuilder
import kotlin.math.max

data class VerseDetailViewHolder(val verseDetailViewLayout: VerseDetailViewLayout) : ViewHolder

class VerseDetailPresenter(
        private val readingActivity: ReadingActivity, private val navigator: Navigator,
        readingViewModel: ReadingViewModel, lifecycle: Lifecycle,
        lifecycleCoroutineScope: LifecycleCoroutineScope = lifecycle.coroutineScope
) : BaseSettingsPresenter<VerseDetailViewHolder, ReadingViewModel>(readingViewModel, lifecycle, lifecycleCoroutineScope) {
    private val translationComparator = TranslationInfoComparator(
            TranslationInfoComparator.SORT_ORDER_LANGUAGE_THEN_SHORT_NAME)

    @VisibleForTesting
    var verseDetail: VerseDetail? = null
    private var updateBookmarkJob: Job? = null
    private var updateHighlightJob: Job? = null
    private var updateNoteJob: Job? = null

    private var downloadStrongNumberJob: Job? = null
    private var downloadStrongNumberDialog: ProgressDialog? = null

    @UiThread
    override fun onBind() {
        super.onBind()

        viewHolder.verseDetailViewLayout.setListeners(
                onClicked = { close() }, onBookmarkClicked = { updateBookmark() },
                onHighlightClicked = { updateHighlight() }, onNoteUpdated = { updateNote(it) },
                onNoStrongNumberClicked = { downloadStrongNumber() }
        )
        viewHolder.verseDetailViewLayout.post {
            viewHolder.verseDetailViewLayout.hide()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        viewModel.settings().onEachSuccess { viewHolder.verseDetailViewLayout.setSettings(it) }.launchIn(lifecycleScope)
        viewModel.verseDetailRequest().onEach {
            loadVerseDetail(it.verseIndex)
            viewHolder.verseDetailViewLayout.show(it.content)
        }.launchIn(lifecycleScope)
        viewModel.currentVerseIndex().onEachSuccess { close() }.launchIn(lifecycleScope)
    }

    private fun updateBookmark() {
        updateBookmarkJob?.cancel()
        updateBookmarkJob = lifecycleScope.launch {
            verseDetail?.let { detail ->
                if (detail.bookmarked) {
                    viewModel.removeBookmark(detail.verseIndex)
                    verseDetail = detail.copy(bookmarked = false)
                } else {
                    viewModel.addBookmark(detail.verseIndex)
                    verseDetail = detail.copy(bookmarked = true)
                }
                viewHolder.verseDetailViewLayout.setVerseDetail(verseDetail!!)
            }

            updateBookmarkJob = null
        }
    }

    private fun updateHighlight() {
        readingActivity.dialog(R.string.text_pick_highlight_color,
                readingActivity.resources.getStringArray(R.array.text_colors),
                max(0, Highlight.AVAILABLE_COLORS.indexOf(verseDetail?.highlightColor
                        ?: Highlight.COLOR_NONE)),
                DialogInterface.OnClickListener { dialog, which ->
                    updateHighlight(Highlight.AVAILABLE_COLORS[which])

                    dialog.dismiss()
                })
    }

    private fun updateHighlight(@Highlight.Companion.AvailableColor highlightColor: Int) {
        updateHighlightJob?.cancel()
        updateHighlightJob = lifecycleScope.launch {
            verseDetail?.let { detail ->
                if (highlightColor == Highlight.COLOR_NONE) {
                    viewModel.removeHighlight(detail.verseIndex)
                } else {
                    viewModel.saveHighlight(detail.verseIndex, highlightColor)
                }
                verseDetail = detail.copy(highlightColor = highlightColor)
                viewHolder.verseDetailViewLayout.setVerseDetail(verseDetail!!)
            }

            updateHighlightJob = null
        }
    }

    private fun updateNote(note: String) {
        updateNoteJob?.cancel()
        updateNoteJob = lifecycleScope.launch {
            verseDetail?.let { detail ->
                if (note.isEmpty()) {
                    viewModel.removeNote(detail.verseIndex)
                } else {
                    viewModel.saveNote(detail.verseIndex, note)
                }
                verseDetail = detail.copy(note = note)
            }

            updateNoteJob = null
        }
    }

    private fun downloadStrongNumber() {
        if (downloadStrongNumberJob != null || downloadStrongNumberDialog != null) {
            // just in case the user clicks too fast
            return
        }

        downloadStrongNumberDialog = readingActivity.progressDialog(
                R.string.dialog_downloading, 100) { downloadStrongNumberJob?.cancel() }
        downloadStrongNumberJob = viewModel.downloadStrongNumber()
                .onEach(
                        onLoading = {
                            it?.let { progress ->
                                downloadStrongNumberDialog?.run {
                                    if (progress < 100) {
                                        setProgress(progress)
                                    } else {
                                        setTitle(R.string.dialog_installing)
                                        setIsIndeterminate(true)
                                    }
                                }
                            }
                                    ?: throw IllegalStateException("Missing progress data when downloading")
                        },
                        onSuccess = {
                            readingActivity.toast(R.string.toast_downloaded)

                            verseDetail?.let {
                                verseDetail = it.copy(strongNumberItems = viewModel.readStrongNumber(it.verseIndex).toStrongNumberItems())
                                viewHolder.verseDetailViewLayout.setVerseDetail(verseDetail!!)
                            }
                        },
                        onError = { _, _ ->
                            readingActivity.dialog(true, R.string.dialog_download_error,
                                    DialogInterface.OnClickListener { _, _ -> downloadStrongNumber() })
                        }
                ).onCompletion {
                    dismissDownloadStrongNumberDialog()
                    downloadStrongNumberJob = null
                }.launchIn(lifecycleScope)
    }

    private fun dismissDownloadStrongNumberDialog() {
        downloadStrongNumberDialog?.dismiss()
        downloadStrongNumberDialog = null
    }

    private fun loadVerseDetail(verseIndex: VerseIndex) {
        lifecycleScope.launch {
            try {
                viewHolder.verseDetailViewLayout.setVerseDetail(VerseDetail.INVALID)
                verseDetail = coroutineScope {
                    val bookmarkAsync = async { viewModel.readBookmark(verseIndex) }
                    val highlightAsync = async { viewModel.readHighlight(verseIndex) }
                    val noteAsync = async { viewModel.readNote(verseIndex) }
                    val strongNumberAsync = async { viewModel.readStrongNumber(verseIndex) }
                    return@coroutineScope VerseDetail(verseIndex, buildVerseTextItems(verseIndex),
                            bookmarkAsync.await().isValid(), highlightAsync.await().color,
                            noteAsync.await().note, strongNumberAsync.await().toStrongNumberItems())
                }
                viewHolder.verseDetailViewLayout.setVerseDetail(verseDetail!!)
            } catch (e: Exception) {
                Log.e(tag, "Failed to load verse detail", e)
                readingActivity.dialog(true, R.string.dialog_load_verse_detail_error,
                        DialogInterface.OnClickListener { _, _ -> loadVerseDetail(verseIndex) })
            }
        }
    }

    @VisibleForTesting
    suspend fun buildVerseTextItems(verseIndex: VerseIndex): List<VerseTextItem> {
        val currentTranslation = viewModel.currentTranslation().first { ViewData.STATUS_SUCCESS == it.status }.data!!
        val parallelTranslations = viewModel.downloadedTranslations().first { ViewData.STATUS_SUCCESS == it.status }.data!!
                .sortedWith(translationComparator)
                .filter { it.shortName != currentTranslation }
                .map { it.shortName }
        val verses = viewModel.readVerses(currentTranslation, parallelTranslations,
                verseIndex.bookIndex, verseIndex.chapterIndex)

        // 1. finds the verse
        var start: VerseIndex? = null
        for (verse in verses) {
            if (verse.text.text.isNotEmpty()) start = verse.verseIndex // we need to consider the empty verses
            if (verse.verseIndex.verseIndex >= verseIndex.verseIndex) break
        }

        val verseIterator = verses.iterator()
        var verse: Verse? = null
        while (verseIterator.hasNext()) {
            val v = verseIterator.next()
            if (v.verseIndex == start) {
                verse = v
                break
            }
        }
        if (verse == null) throw IllegalStateException("Failed to find target verse")

        // 2. builds the parallel
        val parallel = Array(parallelTranslations.size) { StringBuilder() }
        val parallelBuilder: (index: Int, Verse.Text) -> Unit = { index, text ->
            with(parallel[index]) {
                if (isNotEmpty()) append(' ')
                append(text.text)
            }
        }
        verse.parallel.forEachIndexed(parallelBuilder)

        var followingEmptyVerseCount = 0
        while (verseIterator.hasNext()) {
            val v = verseIterator.next()
            if (v.text.text.isNotEmpty()) break
            v.parallel.forEachIndexed(parallelBuilder)
            followingEmptyVerseCount++
        }

        // 3. constructs VerseTextItems
        val verseTextItems = ArrayList<VerseTextItem>(parallelTranslations.size + 1)
        verseTextItems.add(VerseTextItem(verse.verseIndex, followingEmptyVerseCount, verse.text,
                viewModel.readBookNames(verse.text.translationShortName)[verse.verseIndex.bookIndex],
                this@VerseDetailPresenter::onVerseClicked, this@VerseDetailPresenter::onVerseLongClicked))

        parallelTranslations.forEachIndexed { index, translation ->
            verseTextItems.add(VerseTextItem(verse.verseIndex, followingEmptyVerseCount,
                    Verse.Text(translation, parallel[index].toString()),
                    viewModel.readBookNames(translation)[verse.verseIndex.bookIndex],
                    this@VerseDetailPresenter::onVerseClicked, this@VerseDetailPresenter::onVerseLongClicked))
        }

        return verseTextItems
    }

    @VisibleForTesting
    fun onVerseClicked(translation: String) {
        lifecycleScope.launch {
            try {
                if (translation != viewModel.currentTranslation().first { ViewData.STATUS_SUCCESS == it.status }.data) {
                    viewModel.saveCurrentTranslation(translation)
                    close()
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to select translation", e)
                readingActivity.toast(R.string.toast_unknown_error)
            }
        }
    }

    @VisibleForTesting
    fun onVerseLongClicked(verse: Verse) {
        lifecycleScope.launch {
            try {
                val bookName = viewModel.readBookNames(verse.text.translationShortName)[verse.verseIndex.bookIndex]
                // On older devices, this only works on the threads with loopers.
                (readingActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText(verse.text.translationShortName + " " + bookName,
                                verse.toStringForSharing(bookName)))
                readingActivity.toast(R.string.toast_verses_copied)
            } catch (e: Exception) {
                Log.e(tag, "Failed to copy", e)
                readingActivity.toast(R.string.toast_unknown_error)
            }
        }
    }

    private fun List<StrongNumber>.toStrongNumberItems(): List<StrongNumberItem> = map {
        StrongNumberItem(it, this@VerseDetailPresenter::onStrongNumberClicked)
    }

    private fun onStrongNumberClicked(strongNumber: String) {
        try {
            navigator.navigate(readingActivity, Navigator.SCREEN_STRONG_NUMBER,
                    StrongNumberListActivity.bundle(strongNumber))
        } catch (e: Exception) {
            Log.e(tag, "Failed to open Strong's number list activity", e)
            readingActivity.dialog(true, R.string.dialog_navigate_to_strong_number_error,
                    DialogInterface.OnClickListener { _, _ -> onStrongNumberClicked(strongNumber) })
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        dismissDownloadStrongNumberDialog()

        downloadStrongNumberJob?.cancel()
        downloadStrongNumberJob = null
    }

    /**
     * @return true if verse detail view was open, or false otherwise
     * */
    fun close(): Boolean {
        viewHolder.verseDetailViewLayout.hide()

        return verseDetail?.let {
            viewModel.closeVerseDetail(it.verseIndex)
            verseDetail = null
            true
        } ?: false
    }
}
