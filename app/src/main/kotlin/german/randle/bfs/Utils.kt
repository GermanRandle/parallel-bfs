package german.randle.bfs

fun buildCubicGraph(): List<List<Pair<Int, Int>>> {
    return List(CUBE_SIDE * CUBE_SIDE * CUBE_SIDE) {
        val (x, y, z) = vertexToCoords(it)
        buildList {
            if (x > 0) add(coordsToVertex(x - 1, y, z) to 1)
            if (x < CUBE_SIDE - 1) add(coordsToVertex(x + 1, y, z) to 1)
            if (y > 0) add(coordsToVertex(x, y - 1, z) to 1)
            if (y < CUBE_SIDE - 1) add(coordsToVertex(x, y + 1, z) to 1)
            if (z > 0) add(coordsToVertex(x, y, z - 1) to 1)
            if (z < CUBE_SIDE - 1) add(coordsToVertex(x, y, z + 1) to 1)
        }
    }
}

fun vertexToCoords(u: Int) =
    Triple(u / CUBE_SIDE / CUBE_SIDE, u / CUBE_SIDE % CUBE_SIDE, u % CUBE_SIDE)

fun coordsToVertex(x: Int, y: Int, z: Int) =
    x * CUBE_SIDE * CUBE_SIDE + y * CUBE_SIDE + z

fun isBfsCorrect(result: List<Int>): Boolean {
    return false
}
