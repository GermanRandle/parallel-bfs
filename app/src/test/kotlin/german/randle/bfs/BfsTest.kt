package german.randle.bfs

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class BfsTest {
    @ParameterizedTest(name = "side = {0}")
    @ValueSource(ints = [1, 2, 3, 4, 15, 16, 50])
    fun `cubic + seq`(n: Int) {
        val gr = CubicGraph(n)
        val bfsResult = bfsSequential(gr)
        gr.checkBfsResult(bfsResult).shouldBeTrue()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adjListGraphTestcases")
    fun `adj list + seq`(name: String, n: Int, edges: Set<Pair<Int, Int>>, expected: List<Int>) {
        val gr = AdjListGraph(n, edges)
        val bfsResult = bfsSequential(gr)
        bfsResult shouldBe expected
    }

    @ParameterizedTest(name = "side = {0}")
    @ValueSource(ints = [1, 2, 3, 4, 9, 15, 16, 25])
    fun `cubic + par`(n: Int) {
        val gr = CubicGraph(n)
        val bfsResult = runBlocking { bfsParallel(gr, 1) }
        for (i in 0..<gr.n) {
            used.set(i, 0)
        }
        gr.checkBfsResult(bfsResult).shouldBeTrue()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adjListGraphTestcases")
    fun `adj list + par`(name: String, n: Int, edges: Set<Pair<Int, Int>>, expected: List<Int>) {
        val gr = AdjListGraph(n, edges)
        val bfsResult = runBlocking { bfsParallel(gr, 1) }
        for (i in 0..<gr.n) {
            used.set(i, 0)
        }
        bfsResult shouldBe expected
    }

    companion object {
        @JvmStatic
        fun adjListGraphTestcases() = listOf(
            Arguments.of(
                "single node",
                1,
                emptySet<Pair<Int, Int>>(),
                listOf(0),
            ),
            Arguments.of(
                "empty graph",
                6,
                emptySet<Pair<Int, Int>>(),
                listOf(0, INF, INF, INF, INF, INF),
            ),
            Arguments.of(
                "triangle with independent nodes",
                6,
                setOf(0 to 1, 2 to 0, 2 to 1),
                listOf(0, 1, 1, INF, INF, INF),
            ),
            Arguments.of(
                "binary tree",
                7,
                setOf(0 to 1, 0 to 2, 3 to 1, 4 to 1, 5 to 2, 6 to 2),
                listOf(0, 1, 1, 2, 2, 2, 2),
            ),
            Arguments.of(
                "full graph",
                101,
                (0..99).flatMapTo(HashSet()) { i ->
                    ((i + 1)..100).map { j ->
                        i to j
                    }
                },
                listOf(0) + List(100) { 1 },
            ),
            Arguments.of(
                "star",
                777,
                List(776) { 0 to (it + 1) }.toSet(),
                listOf(0) + List(776) {1},
            ),
            Arguments.of(
                "bamboo",
                10, // TODO * 1000
                List(10 - 1) { it to it + 1 }.toSet(),
                List(10) { it },
            ),
        )
    }
}
