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

package me.xizzhu.android.joshua.tests.robots

import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.settings.SettingsActivity
import me.xizzhu.android.joshua.tests.action.click

class SettingsActivityRobot(activity: SettingsActivity) : BaseRobot<SettingsActivity, SettingsActivityRobot>(activity) {
    fun toggleKeepScreenOn(): SettingsActivityRobot {
        click(R.id.keep_screen_on)
        return self()
    }

    fun toggleNightMode(): SettingsActivityRobot {
        click(R.id.night_mode_on)
        return self()
    }

    fun toggleSimpleReading(): SettingsActivityRobot {
        click(R.id.simple_reading_mode)
        return self()
    }

    fun toggleHideSearchButton(): SettingsActivityRobot {
        click(R.id.hide_search_button)
        return self()
    }

    fun toggleConsolidateVersesForSharing(): SettingsActivityRobot {
        click(R.id.consolidated_sharing)
        return self()
    }

    fun clickBackup(): SettingsActivityRobot {
        click(R.id.backup)
        return self()
    }
}
