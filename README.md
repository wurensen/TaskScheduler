# TaskScheduler

## 简介

在Android开发场景中，经常会碰到多个耗时任务之间有依赖关系。

为了快速实现任务可以按照依赖关系调度执行，本库基于Kotlin的协程来实现线程的切换，以及基于图论的DAG（DirectedAcyclicGraph：有向无环图）来实现任务的依赖调度。

## 主要功能

### 依赖任务调度

可以实现异步任务按照依赖关系进行调度执行，常见使用场景：

- 执行任务一开始就已经准备就绪，依赖关系构造后可以直接调度执行，如启动任务依赖执行

- 执行任务一开始未准备就绪，只能先构造依赖关系，执行任务准备完毕后根据依赖关系来决定是否调度还是继续等待

任务调度`doOnScheduled`为`suspend`函数对象，执行在主线程，具体任务执行由开发者自行决定执行在什么线程，`doOnScheduled`调度执行结束表示任务结束，将进行后续的调度。

### 循环任务检查

当依赖任务之间出现循环依赖时，在构造任务图的时候将抛出异常。

## 使用方式

`build.gradle`添加依赖：

```groovy
// 确保仓库已添加：mavenCentral()
allprojects {
    repositories {
        ...
        mavenCentral()
    }
}

// 添加依赖
dependencies {
	compile 'io.github.wurensen:taskscheduler:<version>' 
}
```

> `<version>`请替换为对应的版本号。

构造依赖图并执行调度：

```kotlin
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
```

更多使用例子，请参考：[测试用例](taskscheduler/src/androidTest/java/com/lancewu/taskscheduler/TaskSchedulerTest.kt)

## Change Log

[Change Log](CHANGELOG.md)

## License

```txt
Copyright 2022 LanceWu

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
