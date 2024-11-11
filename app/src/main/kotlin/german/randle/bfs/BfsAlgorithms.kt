package german.randle.bfs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicIntegerArray

fun bfsSequential(gr: Graph): List<Int> {
    val result = MutableList(gr.n) { INF }
    val q = ArrayDeque<Int>()
    result[0] = 0
    q.add(0)

    while (q.isNotEmpty()) {
        val u = q.removeFirst()
        for (v in gr.getAdjacentNodes(u)) {
            if (result[v] == INF) {
                result[v] = result[u] + 1
                q.add(v)
            }
        }
    }

    return result
}

@OptIn(ExperimentalCoroutinesApi::class)
val scope = CoroutineScope(Dispatchers.Default.limitedParallelism(PROCESSES_COUNT))

val used = AtomicIntegerArray(CUBE_SIDE * CUBE_SIDE * CUBE_SIDE)

suspend fun bfsParallel(gr: Graph, blockSize: Int): List<Int> {
    val result = MutableList(gr.n) { INF }
    var frontier = intArrayOf(0)
    used.set(0, 1)
    result[0] = 0

    suspend fun up(scanTree: IntArray, nodeId: Int, l: Int, r: Int) {
        if (r - l <= blockSize) {
            scanTree[nodeId] = (l..<r).sumOf { gr.getAdjacentNodesCount(frontier[it]) }
        }

        val m = (l + r) / 2
        val leftChild = scope.launch { up(scanTree, nodeId * 2 + 1, l, m) }
        val rightChild = scope.launch { up(scanTree, nodeId * 2 + 2, m, r) }
        leftChild.join()
        scanTree[nodeId] += scanTree[nodeId * 2 + 1]
        rightChild.join()
        scanTree[nodeId] += scanTree[nodeId * 2 + 2]
    }

    suspend fun down(scan: IntArray, scanTree: IntArray, nodeId: Int, l: Int, r: Int, acc: Int) {
        if (r - l <= blockSize) {
            var localAcc = 0
            (l..<r).forEach {
                scan[it] = acc + localAcc
                localAcc += gr.getAdjacentNodesCount(frontier[it])
            }
        }

        val m = (l + r) / 2
        val leftChild = scope.launch { down(scan, scanTree, nodeId * 2 + 1, l, m, acc) }
        val rightChild = scope.launch { down(scan, scanTree, nodeId * 2 + 2, m, r, acc + scanTree[nodeId * 2 + 1]) }
        leftChild.join()
        rightChild.join()
    }

    while (frontier.isNotEmpty()) {
        val scanTree = IntArray(frontier.size * 4)
        val scan = IntArray(frontier.size + 1)
        scope.launch { up(scanTree, 0, 0, frontier.size) }.join()
        scope.launch { down(scan, scanTree, 0, 0, frontier.size, 0) }.join()
        val next = IntArray(scan.last()) { -1 }

        val chunksAmount = (frontier.size + blockSize - 1) / blockSize
        val chunkSize = (frontier.size + chunksAmount - 1) / chunksAmount
        val jobs = (0..<chunksAmount).map { chunk ->
            scope.launch {
                val chunkBegin = chunk * chunkSize
                val chunkEnd = minOf(frontier.size, chunkBegin + chunkSize)
                (chunkBegin..<chunkEnd).forEach { u ->
                    for ((i, v) in gr.getAdjacentNodes(u).withIndex()) {
                        if (used.compareAndSet(v, 0, 1)) {
                            next[scan[v] + i] = v
                        }
                    }
                }
            }
        }
        jobs.forEach { it.join() }

        // filter next

        frontier = next // TODO filtered next!
    }

    return result
}
