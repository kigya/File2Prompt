package dev.kigya.file2prompt

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection
import java.nio.file.Files
import java.nio.file.Path

class ConvertFilesToAIPromptAction : AnAction("Convert Files to AI Prompt"), DumbAware {

    override fun update(e: AnActionEvent) {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabled = files?.isNotEmpty() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val base = project.basePath ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Generating AI Prompt", true) {
                private var prompt: String = ""

                override fun run(indicator: ProgressIndicator) {
                    val allFiles = mutableListOf<VirtualFile>()
                    files.forEachIndexed { idx, vf ->
                        indicator.checkCanceled()
                        indicator.text = "Scanning ${vf.name} (${idx + 1}/${files.size})..."
                        if (vf.isDirectory) {
                            VfsUtilCore.iterateChildrenRecursively(
                                vf,
                                { it.isDirectory || !it.fileType.isBinary },
                                { child ->
                                    indicator.checkCanceled()
                                    if (!child.isDirectory && !child.fileType.isBinary) {
                                        allFiles += child
                                    }
                                    true
                                }
                            )
                        } else if (!vf.fileType.isBinary) {
                            allFiles += vf
                        }
                    }

                    val sb = StringBuilder()
                    allFiles.forEachIndexed { idx, file ->
                        indicator.checkCanceled()
                        indicator.text2 = "Processing ${file.name} (${idx + 1}/${allFiles.size})..."
                        val rel = file.path.removePrefix("$base/")
                        sb.append("#### $rel\n\n")
                        file.refresh(false, false)
                        sb.append(VfsUtil.loadText(file))
                        sb.append("\n\n")
                        if (sb.length > MAX_PROMPT_SIZE) {
                            throw ProcessCanceledException()
                        }
                    }
                    prompt = sb.toString()
                }

                override fun onCancel() {
                    Notifications.Bus.notify(
                        Notification(
                            "File2Prompt",
                            "File2Prompt",
                            "Operation cancelled or prompt too large.",
                            NotificationType.WARNING
                        ), project
                    )
                }

                override fun onSuccess() {
                    CopyPasteManager.getInstance().setContents(StringSelection(prompt))

                    val openAfter = SettingsService.getInstance().state.openFileAfterCopy
                    if (openAfter) {
                        val tempFile: Path = Files.createTempFile("file2prompt-", ".txt")
                        tempFile.toFile().deleteOnExit()
                        Files.writeString(tempFile, prompt)
                        LocalFileSystem.getInstance()
                            .refreshAndFindFileByNioFile(tempFile)
                            ?.let { vf ->
                                FileEditorManager.getInstance(project).openFile(vf, true)
                            }
                    }

                    Notifications.Bus.notify(
                        Notification(
                            "File2Prompt",
                            "File2Prompt",
                            if (openAfter)
                                "Prompt copied and file opened."
                            else
                                "Prompt copied to clipboard.",
                            NotificationType.INFORMATION
                        ), project
                    )
                }
            })
    }

    private companion object {
        const val MAX_PROMPT_SIZE = 200_000
    }
}
