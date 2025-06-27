package dev.kigya.file2prompt

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection

class GeneratePromptFromChangesAction : AnAction(
    "Convert Selected Changes to AI Prompt",
    "Generate an AI prompt from the contents of the selected changes or unversioned files",
    AllIcons.Actions.SwapPanels
), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val changes: Array<Change>? = e.getData(VcsDataKeys.CHANGES)
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        val hasChanges = !changes.isNullOrEmpty()
        val hasFiles = !files.isNullOrEmpty()
        e.presentation.isEnabled = hasChanges || hasFiles
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val changes: List<Change> =
            e.getData(VcsDataKeys.CHANGES)
                ?.toList()
                .orEmpty()

        val selectedFiles: List<VirtualFile> =
            e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
                ?.toList()
                .orEmpty()
        val filesFromChanges = changes.mapNotNull { change ->
            (change.afterRevision as? CurrentContentRevision)?.file?.virtualFile
        }

        val allFiles = (filesFromChanges + selectedFiles).distinct()

        if (allFiles.isEmpty()) {
            Notifications.Bus.notify(
                Notification(
                    "File2Prompt",
                    "File2Prompt",
                    "Nothing selected to convert",
                    NotificationType.WARNING
                ), project
            )
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating AI Prompt", true) {
            private var prompt = StringBuilder()

            override fun run(indicator: ProgressIndicator) {
                allFiles.forEachIndexed { idx, file ->
                    indicator.checkCanceled()
                    indicator.text = "Processing ${file.name} (${idx + 1}/${allFiles.size})…"
                    val rel = VfsUtil.getRelativeLocation(file, project.baseDir) ?: file.name
                    prompt.append("#### $rel\n\n")
                    prompt.append(VfsUtil.loadText(file)).append("\n\n")
                }
            }

            override fun onSuccess() {
                CopyPasteManager.getInstance().setContents(StringSelection(prompt.toString()))
                Notifications.Bus.notify(
                    Notification(
                        "File2Prompt",
                        "File2Prompt",
                        "Prompt copied to clipboard",
                        NotificationType.INFORMATION
                    ),
                    project
                )
            }
        })
    }
}
