package german.randle.bfs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

fun bfsSequential(gr: Graph): List<Int> {
    return emptyList() // TODO
}

@OptIn(ExperimentalCoroutinesApi::class)
val scope = CoroutineScope(Dispatchers.Default.limitedParallelism(PROCESSES_COUNT))

fun bfsParallel(gr: Graph): List<Int> {
    return emptyList() // TODO
}
