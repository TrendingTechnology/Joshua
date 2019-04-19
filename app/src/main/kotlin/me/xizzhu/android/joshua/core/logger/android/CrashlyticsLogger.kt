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

package me.xizzhu.android.joshua.core.logger.android

import com.crashlytics.android.Crashlytics
import me.xizzhu.android.joshua.core.logger.Log
import me.xizzhu.android.joshua.core.logger.Logger

class CrashlyticsLogger : Logger {
    private val crashlyticsCore = Crashlytics.getInstance().core

    override fun log(@Log.Level level: Int, tag: String, msg: String) {
        // do nothing
    }

    override fun log(@Log.Level level: Int, tag: String, e: Throwable, msg: String) {
        crashlyticsCore.log(level, tag, msg)
        crashlyticsCore.logException(e)
    }
}