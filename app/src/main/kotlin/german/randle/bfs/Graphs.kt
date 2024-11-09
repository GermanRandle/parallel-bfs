package german.randle.bfs

sealed interface Graph {
    fun getAdjacentNodes(u: Int): List<Pair<Int, Int>>
}

class CubicGraph(val side: Int) : Graph {
    override fun getAdjacentNodes(u: Int): List<Pair<Int, Int>> {
        val (x, y, z) = u.toCoords()
        return buildList {
            if (x > 0) add(nodeOf(x - 1, y, z) to 1)
            if (x < side - 1) add(nodeOf(x + 1, y, z) to 1)
            if (y > 0) add(nodeOf(x, y - 1, z) to 1)
            if (y < side - 1) add(nodeOf(x, y + 1, z) to 1)
            if (z > 0) add(nodeOf(x, y, z - 1) to 1)
            if (z < side - 1) add(nodeOf(x, y, z + 1) to 1)
        }
    }

    fun checkBfsResult(bfsResult: List<Int>): Boolean {
        return false // TODO
    }

    private fun Int.toCoords() = Triple(this / side / side, this / side % side, this % side)

    private fun nodeOf(x: Int, y: Int, z: Int) = x * side * side + y * side + z
}

class AdjListGraph(val adjList: List<List<Pair<Int, Int>>>) : Graph {
    override fun getAdjacentNodes(u: Int): List<Pair<Int, Int>> {
        return adjList[u]
    }
}
