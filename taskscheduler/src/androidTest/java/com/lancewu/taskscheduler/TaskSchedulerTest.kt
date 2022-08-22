package com.lancewu.taskscheduler

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lancewu.taskscheduler.graph.DirectedAcyclicGraph
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.RuntimeException

/**
 * Created by LanceWu on 2022/8/10.<br></br>
 * 测试
 */
@RunWith(AndroidJUnit4::class)
class TaskSchedulerTest {

    companion object {
        const val TAG = "TaskSchedulerTest"
    }

    @Test
    fun test_TaskScheduler() {
        runBlocking {
            val task1 = TaskScheduler.Task("1").apply {
                doOnScheduled = {
                    delay(1000)
                    log("任务执行完成：$this")
                }
            }
            val task2 = TaskScheduler.Task("2").apply {
                doOnScheduled = {
                    delay(1000)
                    log("任务执行完成：$this")
                }
            }
            val task3 = TaskScheduler.Task("3").apply {
                doOnScheduled = {
                    delay(2000)
                    log("任务执行完成：$this")
                }
            }
            val task4 = TaskScheduler.Task("4").apply {
                doOnScheduled = {
                    delay(1000)
                    log("任务执行完成：$this")
                }
            }
            val task5 = TaskScheduler.Task("5").apply {
                doOnScheduled = {
                    delay(1000)
                    log("任务执行完成：$this")
                }
            }
            val graph = DirectedAcyclicGraph.Builder<TaskScheduler.Task>()
                .addEdge(task1, task2)
                .addEdge(task1, task3)
                .addEdge(task2, task4)
                .addEdge(task3, task4)
                .addNode(task5)
                .build()
            val taskScheduler = TaskScheduler(MainScope(), graph)
            taskScheduler.schedule(task1)
                .schedule(task2)
                .schedule(task3)
                .schedule(task4)
                .schedule(task5)
            delay(5000)
        }

    }

    @Test
    fun test_CyclicDependencies() {
        // 有环，抛出异常
        var exception: Exception? = null
        try {
            DirectedAcyclicGraph.Builder<String>()
                .addEdge("1", "2")
                .addEdge("1", "3")
                .addEdge("2", "4")
                .addEdge("3", "4")
                .addEdge("4", "1")
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            exception = e
        }
        assert(exception is RuntimeException)
    }

    private fun log(message: String) {
        Log.e(TAG, message)
    }
}