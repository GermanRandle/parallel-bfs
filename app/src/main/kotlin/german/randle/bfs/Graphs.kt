package german.randle.bfs

sealed interface Graph {
    val n: Int

    fun getAdjacentNodesCount(u: Int): Int

    fun getAdjacentNodes(u: Int): List<Int>
}

class CubicGraph(val side: Int) : Graph {
    override val n = side * side * side

    override fun getAdjacentNodesCount(u: Int): Int {
        val (x, y, z) = u.toCoords()
        return if (x > 0) 1 else 0 +
            if (x < side - 1) 1 else 0 +
            if (y > 0) 1 else 0 +
            if (y < side - 1) 1 else 0 +
            if (z > 0) 1 else 0 +
            if (z < side - 1) 1 else 0
    }

    override fun getAdjacentNodes(u: Int): List<Int> {
        val (x, y, z) = u.toCoords()
        return buildList {
            if (x > 0) add(nodeOf(x - 1, y, z))
            if (x < side - 1) add(nodeOf(x + 1, y, z))
            if (y > 0) add(nodeOf(x, y - 1, z))
            if (y < side - 1) add(nodeOf(x, y + 1, z))
            if (z > 0) add(nodeOf(x, y, z - 1))
            if (z < side - 1) add(nodeOf(x, y, z + 1))
        }
    }

    fun checkBfsResult(bfsResult: List<Int>): Boolean {
        check(bfsResult.size == side * side * side) { "wrong number of nodes in the bfs result..." }
        for (u in 0..<bfsResult.size) {
            if (bfsResult[u] != u.toCoords().let { it.first + it.second + it.third }) {
                return false
            }
        }
        return true
    }

    private fun Int.toCoords() = Triple(this / side / side, this / side % side, this % side)

    private fun nodeOf(x: Int, y: Int, z: Int) = x * side * side + y * side + z
}

class AdjListGraph(override val n: Int, edges: Set<Pair<Int, Int>>) : Graph {
    private val adjList: List<List<Int>>

    init {
        val adjList = List(n) { mutableListOf<Int>() }
        for ((u, v) in edges) {
            check(u != v && (v to u) !in edges) { "No self-loops and repeated edges allowed" }
            adjList[u].add(v)
            adjList[v].add(u)
        }
        this.adjList = adjList
    }

    override fun getAdjacentNodesCount(u: Int): Int {
        return adjList[u].size
    }

    override fun getAdjacentNodes(u: Int): List<Int> {
        return adjList[u]
    }
}
