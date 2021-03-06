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

package jp.hazuki.yuzubrowser.theme

import android.content.Context
import android.net.Uri
import jp.hazuki.yuzubrowser.BrowserApplication
import jp.hazuki.yuzubrowser.R
import jp.hazuki.yuzubrowser.utils.Deferred
import jp.hazuki.yuzubrowser.utils.ErrorReport
import jp.hazuki.yuzubrowser.utils.FileUtils
import jp.hazuki.yuzubrowser.utils.async
import jp.hazuki.yuzubrowser.utils.extensions.forEach
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

internal suspend fun importTheme(context: Context, uri: Uri): Deferred<Result> = async {
    val root = File(BrowserApplication.getExternalUserDirectory(), "theme")
    val tmpFolder = File(root, System.currentTimeMillis().toString())

    try {
        ZipInputStream(context.contentResolver.openInputStream(uri)).use { zis ->
            val buffer = ByteArray(8192)

            zis.forEach { entry ->
                val file = File(tmpFolder, entry.name)
                if (entry.isDirectory) {
                    if (file.exists()) {
                        FileUtils.deleteFile(file)
                    }
                    if (!file.mkdirs()) {
                        FileUtils.deleteFile(tmpFolder)
                        return@async Result(false, context.getString(R.string.cant_create_folder))
                    }
                } else {
                    if (file.exists()) {
                        FileUtils.deleteFile(file)
                    } else if (!file.parentFile.exists()) {
                        if (!file.parentFile.mkdirs()) {
                            FileUtils.deleteFile(tmpFolder)
                            return@async Result(false, context.getString(R.string.cant_create_folder))
                        }
                    }
                    FileOutputStream(file).use { os ->
                        var len = 0
                        while (zis.read(buffer).also { len = it } > 0) {
                            os.write(buffer, 0, len)
                        }
                    }
                }
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        FileUtils.deleteFile(tmpFolder)
        return@async Result(false, context.getString(R.string.theme_unknown_error))
    }

    val manifestFile = File(tmpFolder, ThemeManifest.MANIFEST)
    val manifest: ThemeManifest?

    try {
        manifest = ThemeManifest.decodeManifest(manifestFile)
        if (manifest == null) {
            FileUtils.deleteFile(tmpFolder)
            return@async Result(false, context.getString(R.string.theme_manifest_not_found))
        }
    } catch (e: ThemeManifest.IllegalManifestException) {
        FileUtils.deleteFile(tmpFolder)
        val text = when (e.errorType) {
            0 -> R.string.theme_unknown_error
            1 -> R.string.theme_broken_manifest
            2 -> R.string.theme_unknown_version
            else -> R.string.theme_unknown_error
        }
        return@async Result(false, context.getString(text))
    }


    val name = removeFileProhibitionWord(manifest.name)
    if (name.isEmpty()) {
        FileUtils.deleteFile(tmpFolder)
        return@async Result(false, context.getString(R.string.theme_broken_manifest))
    }

    val theme = File(root, name)

    if (theme.exists()) {
        if (theme.isDirectory) {
            val destManifest = File(theme, ThemeManifest.MANIFEST)
            if (destManifest.exists()) {
                try {
                    val dest = ThemeManifest.decodeManifest(destManifest)
                    if (dest != null) {
                        if (dest.id == manifest.id) {
                            if (dest.version == manifest.version) {
                                FileUtils.deleteFile(tmpFolder)
                                return@async Result(false, context.getString(R.string.theme_installed_version))
                            }
                        } else {
                            FileUtils.deleteFile(tmpFolder)
                            return@async Result(false, context.getString(R.string.theme_same_name, manifest.name))
                        }
                    }
                } catch (e: ThemeManifest.IllegalManifestException) {
                    ErrorReport.printAndWriteLog(e)
                }
            }
        }
        FileUtils.deleteFile(theme)
    }

    if (tmpFolder.renameTo(theme)) {
        return@async Result(true, manifest.name)
    }

    FileUtils.deleteFile(tmpFolder)
    return@async Result(false, context.getString(R.string.theme_unknown_error))
}

private fun removeFileProhibitionWord(name: String): String {
    return name.replace("\\", "")
            .replace("/", "")
            .replace(":", "")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("<", "")
            .replace(">", "")
            .replace("|", "")
            .trim()
}

class Result constructor(val isSuccess: Boolean, val message: String)