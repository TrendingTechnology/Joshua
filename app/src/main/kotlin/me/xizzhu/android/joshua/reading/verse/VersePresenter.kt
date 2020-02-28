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

package me.xizzhu.android.joshua.reading.verse

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.*
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.*
import me.xizzhu.android.joshua.infra.activity.BaseSettingsPresenter
import me.xizzhu.android.joshua.infra.arch.ViewHolder
import me.xizzhu.android.joshua.infra.arch.filterOnSuccess
import me.xizzhu.android.joshua.infra.arch.onEach
import me.xizzhu.android.joshua.infra.arch.onEachSuccess
import me.xizzhu.android.joshua.reading.ReadingActivity
import me.xizzhu.android.joshua.reading.ReadingViewModel
import me.xizzhu.android.joshua.reading.VerseDetailRequest
import me.xizzhu.android.joshua.reading.VersesViewData
import me.xizzhu.android.joshua.ui.dialog
import me.xizzhu.android.joshua.ui.recyclerview.BaseItem
import me.xizzhu.android.joshua.ui.toast
import me.xizzhu.android.joshua.utils.chooserForSharing
import me.xizzhu.android.logger.Log
import kotlin.math.max

data class VerseViewHolder(val versePager: ViewPager2) : ViewHolder

class VersePresenter(
        readingViewModel: ReadingViewModel, readingActivity: ReadingActivity,
        coroutineScope: CoroutineScope = readingActivity.lifecycleScope
) : BaseSettingsPresenter<VerseViewHolder, ReadingViewModel, ReadingActivity>(readingViewModel, readingActivity, coroutineScope) {
    private val selectedVerses: MutableSet<Verse> = mutableSetOf()
    private var actionMode: ActionMode? = null
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_verse_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = when (item.itemId) {
            R.id.action_copy -> {
                copyToClipBoard()
                true
            }
            R.id.action_share -> {
                share()
                true
            }
            else -> false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            selectedVerses.forEach { verse -> adapter.deselectVerse(verse.verseIndex) }
            selectedVerses.clear()
            actionMode = null
        }
    }

    private fun copyToClipBoard() {
        coroutineScope.launch {
            try {
                if (selectedVerses.isNotEmpty()) {
                    val verse = selectedVerses.first()
                    val bookName = viewModel.readBookNames(verse.text.translationShortName)[verse.verseIndex.bookIndex]
                    // On older devices, this only works on the threads with loopers.
                    (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText(verse.text.translationShortName + " " + bookName,
                                    selectedVerses.toStringForSharing(bookName)))
                    activity.toast(R.string.toast_verses_copied)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to copy", e)
                activity.toast(R.string.toast_unknown_error)
            }
            actionMode?.finish()
        }
    }

    private fun share() {
        coroutineScope.launch {
            try {
                if (selectedVerses.isNotEmpty()) {
                    val verse = selectedVerses.first()
                    val bookName = viewModel.readBookNames(verse.text.translationShortName)[verse.verseIndex.bookIndex]

                    activity.chooserForSharing(activity.getString(R.string.text_share_with), selectedVerses.toStringForSharing(bookName))
                            ?.let { activity.startActivity(it) }
                            ?: throw RuntimeException("Failed to create chooser for sharing")
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to share", e)
                activity.toast(R.string.toast_unknown_error)
            }
            actionMode?.finish()
        }
    }

    private val adapter: VersePagerAdapter = VersePagerAdapter(readingActivity,
            { bookIndex, chapterIndex -> loadVerses(bookIndex, chapterIndex) },
            { verseIndex -> updateCurrentVerse(verseIndex) })

    private var currentVerseIndex: VerseIndex = VerseIndex.INVALID

    @UiThread
    override fun onBind() {
        super.onBind()

        viewHolder.versePager.offscreenPageLimit = 1
        viewHolder.versePager.adapter = adapter
        viewHolder.versePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateCurrentChapter(position.toBookIndex(), position.toChapterIndex())
            }
        })
    }

    private fun updateCurrentChapter(bookIndex: Int, chapterIndex: Int) {
        if (currentVerseIndex.bookIndex == bookIndex && currentVerseIndex.chapterIndex == chapterIndex) return

        coroutineScope.launch {
            try {
                viewModel.saveCurrentVerseIndex(VerseIndex(bookIndex, chapterIndex, 0))
            } catch (e: Exception) {
                Log.e(tag, "Failed to current chapter", e)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        viewModel.settings().onEachSuccess { adapter.settings = it }.launchIn(coroutineScope)

        combine(
                viewModel.currentVerseIndex().filterOnSuccess(),
                viewModel.currentTranslation().filterOnSuccess(),
                viewModel.parallelTranslations().filterOnSuccess()
        ) { newVerseIndex, newTranslation, newParallelTranslations ->
            if (actionMode != null) {
                if (currentVerseIndex.bookIndex != newVerseIndex.bookIndex
                        || currentVerseIndex.chapterIndex != newVerseIndex.chapterIndex) {
                    actionMode?.finish()
                }
            }

            currentVerseIndex = newVerseIndex

            adapter.setCurrent(newVerseIndex, newTranslation, newParallelTranslations)
            viewHolder.versePager.setCurrentItem(newVerseIndex.toPagePosition(), false)
        }.launchIn(coroutineScope)

        viewModel.verseUpdates().onEach { adapter.notifyVerseUpdate(it) }.launchIn(coroutineScope)
    }

    private fun updateCurrentVerse(verseIndex: VerseIndex) {
        if (currentVerseIndex == verseIndex) return

        coroutineScope.launch {
            try {
                viewModel.saveCurrentVerseIndex(verseIndex)
            } catch (e: Exception) {
                Log.e(tag, "Failed to update current verse", e)
            }
        }
    }

    private fun loadVerses(bookIndex: Int, chapterIndex: Int) {
        viewModel.verses(bookIndex, chapterIndex).onEach(
                onLoading = {},
                onSuccess = { adapter.setVerses(bookIndex, chapterIndex, it.toItems()) },
                onError = { _, e ->
                    Log.e(tag, "Failed to load verses", e!!)
                    activity.dialog(true, R.string.dialog_verse_load_error,
                            DialogInterface.OnClickListener { _, _ -> loadVerses(bookIndex, chapterIndex) })
                }
        ).launchIn(coroutineScope)
    }

    private fun VersesViewData.toItems(): List<BaseItem> = if (simpleReadingModeOn) {
        verses.toSimpleVerseItems(highlights, this@VersePresenter::onVerseClicked, this@VersePresenter::onVerseLongClicked)
    } else {
        verses.toVerseItems(bookmarks, highlights, notes, this@VersePresenter::onVerseClicked,
                this@VersePresenter::onVerseLongClicked, this@VersePresenter::onBookmarkClicked,
                this@VersePresenter::onHighlightClicked, this@VersePresenter::onNoteClicked)
    }

    @VisibleForTesting
    fun onVerseClicked(verse: Verse) {
        if (actionMode == null) {
            showVerseDetail(verse.verseIndex, VerseDetailRequest.VERSES)
            return
        }

        if (selectedVerses.contains(verse)) {
            // de-select the verse
            selectedVerses.remove(verse)
            if (selectedVerses.isEmpty()) {
                actionMode?.finish()
            }

            adapter.deselectVerse(verse.verseIndex)
        } else {
            // select the verse
            selectedVerses.add(verse)
            adapter.selectVerse(verse.verseIndex)
        }
    }

    private fun showVerseDetail(verseIndex: VerseIndex, @VerseDetailRequest.Companion.Content content: Int) {
        viewModel.requestVerseDetail(VerseDetailRequest(verseIndex, content))
        adapter.selectVerse(verseIndex)
    }

    @VisibleForTesting
    fun onVerseLongClicked(verse: Verse) {
        if (actionMode == null) {
            actionMode = activity.startSupportActionMode(actionModeCallback)
        }
        onVerseClicked(verse)
    }

    @VisibleForTesting
    fun onBookmarkClicked(verseIndex: VerseIndex, hasBookmark: Boolean) {
        coroutineScope.launch {
            try {
                viewModel.saveBookmark(verseIndex, hasBookmark)
            } catch (e: Exception) {
                Log.e(tag, "Failed to update bookmark", e)
                // TODO
            }
        }
    }

    @VisibleForTesting
    fun onHighlightClicked(verseIndex: VerseIndex, @Highlight.Companion.AvailableColor currentHighlightColor: Int) {
        activity.dialog(R.string.text_pick_highlight_color,
                activity.resources.getStringArray(R.array.text_colors),
                max(0, Highlight.AVAILABLE_COLORS.indexOf(currentHighlightColor)),
                DialogInterface.OnClickListener { dialog, which ->
                    updateHighlight(verseIndex, Highlight.AVAILABLE_COLORS[which])

                    dialog.dismiss()
                })
    }

    @VisibleForTesting
    fun updateHighlight(verseIndex: VerseIndex, @Highlight.Companion.AvailableColor highlightColor: Int) {
        coroutineScope.launch {
            try {
                viewModel.saveHighlight(verseIndex, highlightColor)
            } catch (e: Exception) {
                Log.e(tag, "Failed to update highlight", e)
                // TODO
            }
        }
    }

    @VisibleForTesting
    fun onNoteClicked(verseIndex: VerseIndex) {
        showVerseDetail(verseIndex, VerseDetailRequest.NOTE)
    }
}
