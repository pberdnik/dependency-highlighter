package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
@State(name = "PluginSetting", storages = [Storage("dependency_highlighter_settings.xml")], category = SettingsCategory.UI)
class PluginSetting : PersistentStateComponent<PluginSetting> {
    var baseDir: String? = null
    var highlightForward: Boolean = true
    var highlightBackward: Boolean = true
    override fun getState(): PluginSetting {
        return this
    }

    override fun loadState(state: PluginSetting) {
        XmlSerializerUtil.copyBean(state, this)
    }
}