package com.github.pberdnik.dependencyhighlighter.old.graph

import com.github.pberdnik.dependencyhighlighter.old.file.CodeFile

class DependencyGraph {
    val nodes = mutableMapOf<String, Node>()
    private val topSorted = mutableListOf<Node>()

    private val redNodes = mutableMapOf<String, Int>()

    private val nodesInProcess = mutableListOf<Node>()

    private fun add(codeFile: CodeFile): Node {
        return nodes[codeFile.path] ?: run {
            val newNode = Node(codeFile)
            nodes[codeFile.path] = newNode
            newNode
        }
    }

    fun add(codeFile: CodeFile, dependentCodeFile: CodeFile) {
        val node = add(codeFile)
        val dependentNode = add(dependentCodeFile)
        node.dependencies.add(dependentNode)
        dependentNode.backwardDependencies.add(node)
    }

    fun process(config: GraphConfig) {
        runDfs()
        analyzeMobility(config)
        processYellow()
    }

    fun getNodeByClass(name: String): Node {
        return topSorted.find { it.codeFile.className.contains(name) } ?: Node(
            CodeFile(
                "EMPTY"
            )
        )
    }

    fun processYellow() {
        topSorted.forEach { node ->
            if (node.nodeColor == Color.RED) {
                val redDeps = node.dependencies.filter { it.nodeColor == Color.RED }
                if (redDeps.size == 1) {
                    node.onlyRed = redDeps[0]
                }
            }
        }
        topSorted.forEach { node ->
            val onlyRed = node.onlyRed
            if (onlyRed != null) {
                redNodes[onlyRed.id] = (redNodes[onlyRed.id] ?: 0) + node.codeFile.size.toInt()
            }
        }
    }

    private fun runDfs() {
        for (node in nodes.values) {
            node.nodeColor = Color.WHITE
        }
        for (node in nodes.values) {
            if (node.nodeColor == Color.WHITE) {
                dfs(node)
            }
        }
    }

    private fun dfs(node: Node) {
        node.nodeColor = Color.GRAY
        nodesInProcess.add(node)
        for (dependentNode in node.dependencies) {
            if (dependentNode.nodeColor == Color.WHITE) {
                dfs(dependentNode)
            } else if (dependentNode.nodeColor == Color.GRAY) {
                val cycle = Cycle()
                var i = nodesInProcess.size
                do {
                    i--
                    nodesInProcess[i].cycle = cycle
                    cycle.add(nodesInProcess[i])
                } while (nodesInProcess[i] != dependentNode)
            }
        }
        node.nodeColor = Color.BLACK
        nodesInProcess.remove(node)
        topSorted.add(node)
    }

    private fun analyzeMobility(config: GraphConfig) {
        topSorted.forEach { node ->
            node.nodeColor = Color.WHITE

            if (!config.greenModules.contains(node.codeFile.module) || node.cycle != null || config.redClasses.contains(node.codeFile.className)) {
                markAsRed(node)
            } else if (node.dependencies.isEmpty()) {
                node.nodeColor = Color.GREEN
                node.depth = 0
            } else if (node.dependencies.all { it.nodeColor == Color.GREEN }) {
                node.nodeColor = Color.GREEN
                node.depth = node.dependencies.maxByOrNull { it.depth }!!.depth + 1
            } else {
                markAsRed(node)
            }
        }
    }

    private fun markAsRed(node: Node) {
        node.nodeColor = Color.RED
        if (node.dependencies.isEmpty()) {
            node.depth = 0
        } else {
            node.depth =
                node.dependencies.filter { it.nodeColor == Color.RED }.maxByOrNull { it.depth }?.depth?.plus(1) ?: 0
        }
    }
}
