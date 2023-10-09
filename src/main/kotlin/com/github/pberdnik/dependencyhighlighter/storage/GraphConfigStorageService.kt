package com.github.pberdnik.dependencyhighlighter.storage

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "GraphConfig", storages = [Storage("graphConfig.xml")])
@Service(Service.Level.PROJECT)
class GraphConfigStorageService(val project: Project) : PersistentStateComponent<GraphConfigState> {

    private val state = GraphConfigState()

    override fun getState() = state

    override fun loadState(state: GraphConfigState) = XmlSerializerUtil.copyBean<GraphConfigState>(state, this.state)
}