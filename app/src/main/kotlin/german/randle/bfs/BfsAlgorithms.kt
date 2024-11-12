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

    suspend fun up(
        scanTree: IntArray,
        nodeId: Int,
        l: Int,
        r: Int,
        indexToValue: (Int) -> Int,
    ) {
        if (r - l <= blockSize) {
            scanTree[nodeId] = (l..<r).sumOf { indexToValue(it) }
            return
        }

        val m = (l + r) / 2
        val leftChild = scope.launch { up(scanTree, nodeId * 2 + 1, l, m, indexToValue) }
        val rightChild = scope.launch { up(scanTree, nodeId * 2 + 2, m, r, indexToValue) }
        leftChild.join()
        scanTree[nodeId] += scanTree[nodeId * 2 + 1]
        rightChild.join()
        scanTree[nodeId] += scanTree[nodeId * 2 + 2]
    }

    suspend fun down(
        scan: IntArray,
        scanTree: IntArray,
        nodeId: Int,
        l: Int,
        r: Int,
        acc: Int,
        indexToValue: (Int) -> Int,
    ) {
        if (r - l <= blockSize) {
            var localAcc = 0
            (l..<r).forEach {
                localAcc += indexToValue(it)
                scan[it + 1] = acc + localAcc
            }
            return
        }

        val m = (l + r) / 2
        val leftChild = scope.launch { down(scan, scanTree, nodeId * 2 + 1, l, m, acc, indexToValue) }
        val rightChild = scope.launch { down(scan, scanTree, nodeId * 2 + 2, m, r, acc + scanTree[nodeId * 2 + 1], indexToValue) }
        leftChild.join()
        rightChild.join()
    }
    
    val adjNodesFunction = { u: Int -> gr.getAdjacentNodesCount(frontier[u]) }

    var layer = 0
    while (frontier.isNotEmpty()) {
        layer++
        val scanTree = IntArray(frontier.size * 4)
        val scan = IntArray(frontier.size + 1)
        up(scanTree, 0, 0, frontier.size, adjNodesFunction)
        down(scan, scanTree, 0, 0, frontier.size, 0, adjNodesFunction)
        val neighbors = scan.last()
        if (neighbors == 0) {
            break
        }
        val next = IntArray(scan.last()) { -1 }

        val chunksAmount = (frontier.size + blockSize - 1) / blockSize
        val chunkSize = (frontier.size + chunksAmount - 1) / chunksAmount
        val goJobs = (0..<chunksAmount).map { chunk ->
            scope.launch {
                val chunkBegin = chunk * chunkSize
                val chunkEnd = minOf(frontier.size, chunkBegin + chunkSize)
                (chunkBegin..<chunkEnd).forEach { fi ->
                    val u = frontier[fi]
                    for ((i, v) in gr.getAdjacentNodes(u).withIndex()) {
                        if (used.compareAndSet(v, 0, 1)) {
                            next[scan[fi] + i] = v
                        }
                    }
                }
            }
        }
        goJobs.forEach { it.join() }

        val nextScanTree = IntArray(next.size * 4)
        val nextScan = IntArray(next.size + 1)
        val isNodePresentFunction = { i: Int -> if (next[i] != -1) 1 else 0 }
        up(nextScanTree, 0, 0, next.size, isNodePresentFunction)
        down(nextScan, nextScanTree, 0, 0, next.size, 0, isNodePresentFunction)

        val nextFrontier = IntArray(nextScan.last())
        val copyChunksAmount = (next.size + blockSize - 1) / blockSize
        val copyChunkSize = (next.size + copyChunksAmount - 1) / copyChunksAmount
        
        val copyJobs = (0..<copyChunksAmount).map { chunk ->
            scope.launch {
                val chunkBegin = chunk * copyChunkSize
                val chunkEnd = minOf(next.size, chunkBegin + copyChunkSize)
                (chunkBegin..<chunkEnd).forEach { ni ->
                    if (nextScan[ni] != nextScan[ni + 1]) {
                        nextFrontier[nextScan[ni]] = next[ni]
                        result[next[ni]] = layer
                    }
                }
            }
        }
        copyJobs.forEach { it.join() }

        frontier = nextFrontier
    }

    return result
}
