package german.randle.bfs

import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

const val CUBE_SIDE = 500
const val LAUNCHES_COUNT = 5
const val PROCESSES_COUNT = 4
const val INF = 1_000_000_000

val testGraph = CubicGraph(CUBE_SIDE)

/**
 * If the size of array is less than or equal to this number, then we "switch to sequential mode".
 * Should be set up manually, also it depends on [PROCESSES_COUNT].
 */
private const val BLOCK_SIZE = 10000

fun main() = runBlocking {
    val seqToParTimes = List(LAUNCHES_COUNT) {
        println("LAUNCH #${it + 1}")

        cleanupForBfsSequential()
        val sequentialTime = measureTimeMillis {
            bfsSequential(testGraph)
        }.also {
            println("SEQUENTIAL TIME: $it ms")
        }

        cleanupForBfsParallel()
        val parallelTime = measureTimeMillis {
            bfsParallel(testGraph, BLOCK_SIZE)
        }.also {
            println("PARALLEL TIME: $it ms")
        }

        check(testGraph.checkSeqBfsResult()) {
            "SEQUENTIAL BFS FAILED"
        }
        check(testGraph.checkParBfsResult()) {
            "PARALLEL BFS FAILED"
        }

        println("RATIO: ${sequentialTime.toDouble() / parallelTime}")

        sequentialTime to parallelTime
    }

    val avgSeqTime = seqToParTimes.map { it.first }.average()
    val avgParTime = seqToParTimes.map { it.second }.average()

    println()
    println("FINAL RESULTS:")
    println("SEQUENTIAL TIME: $avgSeqTime ms")
    println("PARALLEL TIME: $avgParTime ms")
    println("RATIO: ${avgSeqTime / avgParTime}")
}
