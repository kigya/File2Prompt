package dev.kigya.file2prompt

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "File2PromptSettings",
    storages = [Storage("file2prompt.xml")]
)
@Service(Level.APP)
class SettingsService : PersistentStateComponent<SettingsState> {
    private var state = SettingsState()
    override fun getState() = state
    override fun loadState(state: SettingsState) {
        this.state = state
    }

    companion object {
        @JvmStatic
        fun getInstance(): SettingsService = ApplicationManager
            .getApplication().getService(SettingsService::class.java)
    }
}
