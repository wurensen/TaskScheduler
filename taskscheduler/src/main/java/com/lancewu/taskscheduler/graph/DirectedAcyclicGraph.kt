package com.lancewu.taskscheduler.graph

import com.lancewu.taskscheduler.graph.DirectedAcyclicGraph.Builder
import java.util.*

/**
 * DAG图（有向无环图）
 * @param Node 节点类型
 * @property graph Map<Node, List<Node>?> 图，value表示入度节点
 * @constructor 通过[Builder.build]创建实例
 */
class DirectedAcyclicGraph<Node> private constructor(val graph: Map<Node, List<Node>?>) {

    init {
        checkCyclicDependencies()
    }

    private fun checkCyclicDependencies() {
        // 已经访问过的节点
        val visited = mutableListOf<Node>()
        val stack = Stack<Node>()
        // 先找到入度为0的节点，添加到栈中
        for (key in graph.keys) {
            if (getIncomingNodes(key).isNullOrEmpty()) {
                stack.push(key)
            }
        }
        var count = 0
        while (stack.isNotEmpty()) {
            val node = stack.pop()
            visited.add(node)
            count++
            // 当前顶点所有指向顶点入度-1，如果计算后为0，则加入栈中
            getOutgoingNodes(node)?.forEach { outgoing ->
                // outgoing的已经无入度节点，入栈
                val incomingVisited = getIncomingNodes(outgoing)?.let {
                    visited.containsAll(it)
                } ?: true
                if (incomingVisited) {
                    stack.push(outgoing)
                }
            }
        }
        if (count < graph.size) {
            throw RuntimeException("This graph contains cyclic dependencies")
        }
    }

    /**
     * 获取[node]的所有入度节点
     * @param node Node
     * @return List<Node>?
     */
    fun getIncomingNodes(node: Node): List<Node>? {
        return graph[node]
    }

    /**
     * 获取[node]的所有出度节点
     * @param node Node
     * @return List<Node>?
     */
    fun getOutgoingNodes(node: Node): List<Node>? {
        var outgoing: MutableList<Node>? = null
        for ((item, incomingEdges) in graph) {
            if (incomingEdges?.contains(node) == true) {
                if (outgoing == null) {
                    outgoing = mutableListOf()
                }
                outgoing.add(item)
            }
        }
        return outgoing
    }

    /**
     * 构造器
     * @param Node 节点
     * @property graph MutableMap<Node, MutableList<Node>?>
     */
    class Builder<Node> {

        // DAG图（key-节点，value-入度节点）
        private val graph = mutableMapOf<Node, MutableList<Node>?>()

        /***
         * 添加节点
         * @param node Node
         */
        fun addNode(node: Node): Builder<Node> {
            if (!graph.containsKey(node)) {
                graph[node] = null
            }
            return this
        }

        /**
         * 添加边：[nodeU]->[nodeV]
         * @param nodeU Node
         * @param nodeV Node
         */
        fun addEdge(nodeU: Node, nodeV: Node): Builder<Node> {
            addNode(nodeU)
            addNode(nodeV)
            val incoming = graph[nodeV] ?: mutableListOf()
            if (!incoming.contains(nodeU)) {
                incoming.add(nodeU)
            }
            graph[nodeV] = incoming
            return this
        }

        /**
         * 创建DAG图，如果有环将抛出异常
         * @return DirectedAcyclicGraph<Node>
         */
        fun build(): DirectedAcyclicGraph<Node> {
            return DirectedAcyclicGraph(graph)
        }
    }

}