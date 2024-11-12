package german.randle.bfs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicIntegerArray

@OptIn(ExperimentalCoroutinesApi::class)
val scope = CoroutineScope(Dispatchers.Default.limitedParallelism(PROCESSES_COUNT))

// Optimizing memory
const val MAX_FRONTIER = CUBE_SIDE * CUBE_SIDE
const val MAX_DEGREE = 6
val seqResult = MutableList(CUBE_SIDE * CUBE_SIDE * CUBE_SIDE) { INF }
val parResult = AtomicIntegerArray(CUBE_SIDE * CUBE_SIDE * CUBE_SIDE)
val frontier = IntArray(MAX_FRONTIER) { -1 }
val next = IntArray(MAX_FRONTIER * MAX_DEGREE) { -1 }
val nextScanTree = IntArray(MAX_FRONTIER * MAX_DEGREE * 4)
val nextScan = IntArray(MAX_FRONTIER * MAX_DEGREE)
val scanTree = IntArray(MAX_FRONTIER * 4)
val scan = IntArray(MAX_FRONTIER)

fun cleanupForBfsSequential() {
    for (i in 0..<seqResult.size) {
        seqResult[i] = INF
    }
}

/**
 * WARNING: call [cleanupForBfsSequential] before
 */
fun bfsSequential(gr: Graph) {
    val q = ArrayDeque<Int>()
    seqResult[0] = 0
    q.add(0)

    while (q.isNotEmpty()) {
        val u = q.removeFirst()
        for (v in gr.getAdjacentNodes(u)) {
            if (seqResult[v] == INF) {
                seqResult[v] = seqResult[u] + 1
                q.add(v)
            }
        }
    }
}

fun cleanupForBfsParallel() {
    for (i in 0..<parResult.length()) {
        parResult.set(i, INF)
    }
    for (i in 0..<frontier.size) {
        frontier[i] = -1
    }
    for (i in 0..<next.size) {
        next[i] = -1
    }
}

/**
 * WARNING: call [cleanupForBfsParallel] before
 */
suspend fun bfsParallel(gr: Graph, blockSize: Int) {
    frontier[0] = 0
    var frontierSize = 1
    parResult.set(0, 0)

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
        scanTree[nodeId] = scanTree[nodeId * 2 + 1]
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
    ): Int {
        if (r - l <= blockSize) {
            var localAcc = 0
            (l..<r).forEach {
                localAcc += indexToValue(it)
                scan[it + 1] = acc + localAcc
            }
            return localAcc
        }

        val m = (l + r) / 2
        val leftChild = scope.async { down(scan, scanTree, nodeId * 2 + 1, l, m, acc, indexToValue) }
        val rightChild = scope.async { down(scan, scanTree, nodeId * 2 + 2, m, r, acc + scanTree[nodeId * 2 + 1], indexToValue) }
        return leftChild.await() + rightChild.await()
    }
    
    val adjNodesFunction = { u: Int -> gr.getAdjacentNodesCount(frontier[u]) }

    var layer = 0
    while (frontierSize > 0) {
        layer++
        up(scanTree, 0, 0, frontierSize, adjNodesFunction)
        val nextSize = down(scan, scanTree, 0, 0, frontierSize, 0, adjNodesFunction)

        val chunksAmount = (frontierSize + blockSize - 1) / blockSize
        val chunkSize = (frontierSize + chunksAmount - 1) / chunksAmount
        val goJobs = (0..<chunksAmount).map { chunk ->
            scope.launch {
                val chunkBegin = chunk * chunkSize
                val chunkEnd = minOf(frontierSize, chunkBegin + chunkSize)
                (chunkBegin..<chunkEnd).forEach { frontierIndex ->
                    val u = frontier[frontierIndex]
                    for ((i, v) in gr.getAdjacentNodes(u).withIndex()) {
                        if (parResult.compareAndSet(v, INF, layer)) {
                            next[scan[frontierIndex] + i] = v
                        }
                    }
                }
            }
        }
        goJobs.forEach { it.join() }

        val isNodePresentFunction = { i: Int -> if (next[i] != -1) 1 else 0 }
        up(nextScanTree, 0, 0, nextSize, isNodePresentFunction)
        val nextFrontierSize = down(nextScan, nextScanTree, 0, 0, nextSize, 0, isNodePresentFunction)
        if (nextFrontierSize == 0) {
            break
        }

        val copyChunksAmount = (nextSize + blockSize - 1) / blockSize
        val copyChunkSize = (nextSize + copyChunksAmount - 1) / copyChunksAmount
        
        val copyJobs = (0..<copyChunksAmount).map { chunk ->
            scope.launch {
                val chunkBegin = chunk * copyChunkSize
                val chunkEnd = minOf(nextSize, chunkBegin + copyChunkSize)
                (chunkBegin..<chunkEnd).forEach {
                    if (nextScan[it] != nextScan[it + 1]) {
                        frontier[nextScan[it]] = next[it]
                        parResult[next[it]] = layer
                        next[it] = -1
                    }
                }
            }
        }
        copyJobs.forEach { it.join() }

        // We don't expect many elements here, so I see no reason to parallelize it.
        for (i in nextFrontierSize..<frontierSize) {
            frontier[i] = -1
        }

        frontierSize = nextFrontierSize
    }
}
