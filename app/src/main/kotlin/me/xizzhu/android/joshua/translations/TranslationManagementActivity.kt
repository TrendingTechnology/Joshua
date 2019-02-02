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

package me.xizzhu.android.joshua.translations

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjector
import me.xizzhu.android.joshua.utils.BaseActivity

@Module
class TranslationManagementModule

@Subcomponent(modules = [(TranslationManagementModule::class)])
interface TranslationManagementComponent : AndroidInjector<TranslationManagementActivity> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<TranslationManagementActivity>()
}

class TranslationManagementActivity : BaseActivity() {
    companion object {
        fun newStartIntent(context: Context) = Intent(context, TranslationManagementActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
