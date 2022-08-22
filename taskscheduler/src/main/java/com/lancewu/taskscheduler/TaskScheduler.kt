package com.lancewu.taskscheduler

import com.lancewu.taskscheduler.graph.DirectedAcyclicGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by LanceWu on 2022/8/5
 *
 * 依赖任务调度器
 *
 * 可以实现异步任务按照依赖关系进行调度执行，常见使用场景：
 * - 执行任务一开始就已经准备就绪，依赖关系构造后可以直接调度执行，如启动任务依赖执行
 * - 执行任务一开始未准备就绪，只能先构造依赖关系，执行任务准备完毕后根据依赖关系来决定是否调度还是继续等待
 */
class TaskScheduler(
    val scope: CoroutineScope,
    val taskGraph: DirectedAcyclicGraph<Task>
) {

    /**
     * 任务状态
     */
    private val taskStates: MutableMap<Task, State>

    /**
     * 调度线程
     */
    private val scheduleDispatcher = Dispatchers.Main

    init {
        taskStates = taskGraph.graph.mapValuesTo(mutableMapOf()) {
            State.INIT
        }
    }

    /**
     * 根据任务标签获取任务（如果多个任务标签一致，只获取检索到的第一个）
     * @param taskTag Any
     * @return Task?
     */
    fun getTaskByTag(taskTag: Any): Task? {
        return taskGraph.graph.keys.firstOrNull { it.tag == taskTag }
    }

    /**
     * 获取当前任务状态
     * @param task Task
     * @return State
     */
    fun getTaskState(task: Task): State {
        return taskStates[task]!!
    }

    /**
     * 调度任务
     * @param task Task
     */
    fun schedule(task: Task): TaskScheduler {
        // 统一在主线程调度，防止多线程问题
        scope.launch(scheduleDispatcher) {
            val state = taskStates[task]
            if (state == State.INIT) {
                nextState(task)
                scheduleCurrent(task)
            }
        }
        return this
    }

    private fun scheduleCurrent(task: Task) {
        scope.launch(scheduleDispatcher) {
            val state = taskStates[task]
            // 准备就绪的任务才参与调度
            if (state == State.PREPARED && isIncomingFinished(task)) {
                nextState(task)
                task.doOnScheduled?.invoke(scope)
                nextState(task)
                scheduleNext(task)
            }
        }
    }

    private fun scheduleNext(task: Task) {
        taskGraph.getOutgoingNodes(task)?.forEach {
            scheduleCurrent(it)
        }
    }

    private fun nextState(task: Task) {
        taskStates[task] = getTaskState(task).next()
    }

    private fun isIncomingFinished(task: Task): Boolean {
        return taskGraph.getIncomingNodes(task)?.all { taskStates[it] == State.FINISHED } ?: true
    }

    /**
     * 任务状态，枚举顺序为状态顺序
     */
    enum class State {
        /**
         * 初始状态
         */
        INIT,

        /**
         * 准备就绪
         */
        PREPARED,

        /**
         * 已被调度
         */
        SCHEDULED,

        /**
         * 完成
         */
        FINISHED;

        /**
         * 进入下一个状态
         * @return State
         */
        fun next(): State {
            val values = values()
            val nextOrdinal = (this.ordinal + 1) % values.size
            return values[nextOrdinal]
        }
    }

    /**
     * 任务
     * @param tag 任务标签
     */
    class Task(val tag: Any) {

        /**
         * 调度执行行为，执行结束后会被标记为任务结束，任务在主线程被调度，如果需要执行在异步需要自行切到其它线程
         */
        var doOnScheduled: (suspend (scope: CoroutineScope) -> Unit)? = null

        override fun toString(): String {
            return "Task(tag='$tag')"
        }

    }

}