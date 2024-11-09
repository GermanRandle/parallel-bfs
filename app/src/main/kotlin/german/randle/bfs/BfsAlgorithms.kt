package german.randle.bfs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

fun bfsParallel(gr: Graph): List<Int> {
    return emptyList() // TODO
}
