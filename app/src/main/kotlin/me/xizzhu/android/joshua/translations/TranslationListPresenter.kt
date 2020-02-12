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

package me.xizzhu.android.joshua.translations

import android.content.DialogInterface
import android.view.View
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.TranslationInfo
import me.xizzhu.android.joshua.infra.arch.*
import me.xizzhu.android.joshua.infra.interactors.BaseSettingsAwarePresenter
import me.xizzhu.android.joshua.ui.*
import me.xizzhu.android.joshua.ui.recyclerview.BaseItem
import me.xizzhu.android.joshua.ui.recyclerview.CommonRecyclerView
import me.xizzhu.android.joshua.ui.recyclerview.TitleItem
import me.xizzhu.android.logger.Log
import java.util.ArrayList

data class TranslationListViewHolder(val swipeRefreshLayout: SwipeRefreshLayout,
                                     val translationListView: CommonRecyclerView) : ViewHolder

class TranslationListPresenter(private val translationManagementActivity: TranslationManagementActivity,
                               translationListInteractor: TranslationListInteractor,
                               dispatcher: CoroutineDispatcher = Dispatchers.Main)
    : BaseSettingsAwarePresenter<TranslationListViewHolder, TranslationListInteractor>(translationListInteractor, dispatcher) {
    private val translationComparator = TranslationInfoComparator(TranslationInfoComparator.SORT_ORDER_LANGUAGE_THEN_NAME)

    private var downloadingJob: Job? = null
    private var downloadTranslationDialog: ProgressDialog? = null
    private var removeTranslationDialog: AlertDialog? = null

    @UiThread
    override fun onCreate(viewHolder: TranslationListViewHolder) {
        super.onCreate(viewHolder)

        with(viewHolder.swipeRefreshLayout) {
            setColorSchemeResources(R.color.primary_dark, R.color.primary, R.color.dark_cyan, R.color.dark_lime)
            setOnRefreshListener { interactor.loadTranslationList(true) }
        }

        observeSettings()
        observeTranslationList()
        interactor.loadTranslationList(false)
    }

    private fun observeSettings() {
        interactor.settings().onEachSuccess { viewHolder?.translationListView?.setSettings(it) }.launchIn(coroutineScope)
    }

    private fun observeTranslationList() {
        interactor.translationList()
                .map { viewData ->
                    when (viewData.status) {
                        ViewData.STATUS_SUCCESS -> viewData.data
                                ?.let { translationList ->
                                    val availableTranslations = translationList.availableTranslations.sortedWith(translationComparator)
                                    val downloadedTranslations = translationList.downloadedTranslations.sortedWith(translationComparator)
                                    val items: ArrayList<BaseItem> = ArrayList()
                                    items.addAll(downloadedTranslations.toTranslationItems(
                                            translationList.currentTranslation,
                                            this@TranslationListPresenter::onTranslationClicked,
                                            this@TranslationListPresenter::onTranslationLongClicked))
                                    if (availableTranslations.isNotEmpty()) {
                                        items.add(TitleItem(translationManagementActivity.getString(R.string.header_available_translations), false))
                                        items.addAll(availableTranslations.toTranslationItems(
                                                translationList.currentTranslation,
                                                this@TranslationListPresenter::onTranslationClicked,
                                                this@TranslationListPresenter::onTranslationLongClicked))
                                    }
                                    ViewData.success(items)
                                }
                                ?: throw IllegalStateException("Missing translation list")
                        ViewData.STATUS_ERROR -> ViewData.error(exception = viewData.exception)
                        ViewData.STATUS_LOADING -> ViewData.loading()
                        else -> throw IllegalStateException("Unsupported view data status: ${viewData.status}")
                    }
                }.onEach(
                        onLoading = {
                            viewHolder?.run {
                                swipeRefreshLayout.isRefreshing = true
                                translationListView.visibility = View.GONE
                            }
                        },
                        onSuccess = { items ->
                            viewHolder?.run {
                                swipeRefreshLayout.isRefreshing = false
                                translationListView.setItems(items)
                                translationListView.fadeIn()
                            }
                        },
                        onError = { _, _ ->
                            viewHolder?.swipeRefreshLayout?.isRefreshing = false
                            translationManagementActivity.dialog(false, R.string.dialog_load_translation_list_error,
                                    DialogInterface.OnClickListener { _, _ -> interactor.loadTranslationList(false) },
                                    DialogInterface.OnClickListener { _, _ -> translationManagementActivity.finish() })
                        }
                ).launchIn(coroutineScope)
    }

    @VisibleForTesting
    fun onTranslationClicked(translationInfo: TranslationInfo) {
        if (translationInfo.downloaded) {
            updateCurrentTranslationAndFinishActivity(translationInfo.shortName)
        } else {
            downloadTranslation(translationInfo)
        }
    }

    private fun updateCurrentTranslationAndFinishActivity(translationShortName: String) {
        coroutineScope.launch {
            try {
                interactor.saveCurrentTranslation(translationShortName)
                translationManagementActivity.finish()
            } catch (e: Exception) {
                Log.e(tag, "Failed to select translation and close translation management activity", e)
                translationManagementActivity.dialog(true, R.string.dialog_update_translation_error,
                        DialogInterface.OnClickListener { _, _ -> updateCurrentTranslationAndFinishActivity(translationShortName) })
            }
        }
    }

    private fun downloadTranslation(translationToDownload: TranslationInfo) {
        if (downloadingJob != null || downloadTranslationDialog != null) {
            // just in case the user clicks too fast
            return
        }

        downloadTranslationDialog = translationManagementActivity.progressDialog(
                R.string.dialog_downloading, 100) { downloadingJob?.cancel() }

        downloadingJob = interactor.downloadTranslation(translationToDownload)
                .onEach(
                        onLoading = {
                            it?.let { progress ->
                                downloadTranslationDialog?.run {
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
                            translationManagementActivity.toast(R.string.toast_downloaded)
                        },
                        onError = { _, _ ->
                            translationManagementActivity.dialog(true, R.string.dialog_download_error,
                                    DialogInterface.OnClickListener { _, _ -> downloadTranslation(translationToDownload) })
                        }
                ).onCompletion {
                    dismissDownloadTranslationDialog()
                    downloadingJob = null
                }.launchIn(coroutineScope)
    }

    private fun dismissDownloadTranslationDialog() {
        downloadTranslationDialog?.dismiss()
        downloadTranslationDialog = null
    }

    @VisibleForTesting
    fun onTranslationLongClicked(translationInfo: TranslationInfo, isCurrentTranslation: Boolean) {
        if (translationInfo.downloaded) {
            if (!isCurrentTranslation) {
                translationManagementActivity.dialog(true, R.string.dialog_delete_translation_confirmation,
                        DialogInterface.OnClickListener { _, _ -> removeTranslation(translationInfo) })
            }
        } else {
            downloadTranslation(translationInfo)
        }
    }

    private fun removeTranslation(translationToRemove: TranslationInfo) {
        coroutineScope.launch {
            if (removeTranslationDialog != null) {
                // just in case the user clicks too fast
                return@launch
            }

            try {
                removeTranslationDialog = translationManagementActivity.indeterminateProgressDialog(R.string.dialog_deleting)

                interactor.removeTranslation(translationToRemove)

                dismissRemoveTranslationDialog()
                translationManagementActivity.toast(R.string.toast_deleted)
            } catch (e: Exception) {
                Log.e(tag, "Failed to remove translation", e)
                dismissRemoveTranslationDialog()
                translationManagementActivity.dialog(true, R.string.dialog_delete_error,
                        DialogInterface.OnClickListener { _, _ -> removeTranslation(translationToRemove) })
            }
        }
    }

    private fun dismissRemoveTranslationDialog() {
        removeTranslationDialog?.dismiss()
        removeTranslationDialog = null
    }

    @UiThread
    override fun onStop() {
        dismissDownloadTranslationDialog()
        downloadingJob?.cancel()
        downloadingJob = null

        dismissRemoveTranslationDialog()

        super.onStop()
    }
}
