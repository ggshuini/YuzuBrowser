/*
 * Copyright (C) 2017 Hazuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.hazuki.yuzubrowser.settings.activity

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.webkit.WebView
import android.widget.Toast
import jp.hazuki.yuzubrowser.BuildConfig
import jp.hazuki.yuzubrowser.R
import jp.hazuki.yuzubrowser.utils.AppUtils
import jp.hazuki.yuzubrowser.utils.FileUtils
import jp.hazuki.yuzubrowser.utils.extensions.setClipboardWithToast
import java.io.File

class AboutFragment : YuzuPreferenceFragment() {

    override fun onCreateYuzuPreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_about)
        val version = findPreference("version")
        version.setOnPreferenceClickListener {
            activity.setClipboardWithToast(AppUtils.getVersionDeviceInfo(activity))
            true
        }

        version.summary = BuildConfig.VERSION_NAME
        findPreference("build").summary = BuildConfig.GIT_HASH
        findPreference("build_time").summary = BuildConfig.BUILD_TIME

        findPreference("osl").setOnPreferenceClickListener {
            OpenSourceLicenseDialog().show(childFragmentManager, "osl")
            true
        }

        findPreference("delete_log").setOnPreferenceClickListener {
            DeleteLogDialog().show(childFragmentManager, "delete")
            true
        }
    }

    class OpenSourceLicenseDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val webView = WebView(activity)
            webView.loadUrl("file:///android_asset/licenses.html")
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.open_source_license)
                    .setView(webView)
            return builder.create()
        }
    }

    class DeleteLogDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.pref_delete_all_logs)
                    .setMessage(R.string.pref_delete_log_mes)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        val file = File(activity.getExternalFilesDir(null), "./error_log/")
                        if (!file.exists()) {
                            Toast.makeText(activity, R.string.succeed, Toast.LENGTH_SHORT).show()
                        } else if (FileUtils.deleteFile(file)) {
                            Toast.makeText(activity, R.string.succeed, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, R.string.failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(android.R.string.no, null)
            return builder.create()
        }
    }
}
