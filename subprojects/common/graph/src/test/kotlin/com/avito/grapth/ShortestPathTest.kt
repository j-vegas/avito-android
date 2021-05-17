package com.avito.grapth

import com.avito.graph.Operation
import com.avito.graph.ShortestPath
import com.avito.graph.SimpleOperation
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ShortestPathTest {


    fun `zero operations - empty path`() {
        val path = shortestPath(emptySet())

        assertThat(path.operations).isEmpty()
    }


    fun `one operation - path with it`() {
        val operation = SimpleOperation(id = "id", duration = 1.0)

        val path = shortestPath(setOf(operation))

        assertThat(path.operations).containsExactly(operation)
    }


    fun `consecutive operations - all of them`() {
        val a1 = SimpleOperation(id = "a1", duration = 1.0)
        val a2 = SimpleOperation(id = "a2", duration = 1.0, predecessors = setOf("a1"))
        val a3 = SimpleOperation(id = "a3", duration = 1.0, predecessors = setOf("a2"))

        val path = shortestPath(setOf(a3, a2, a1))

        assertThat(path.operations).containsExactly(a1, a2, a3)
    }


    fun `independent chains - the shortest chain`() {
        val a1 = SimpleOperation(id = "a1", duration = 2.0)
        val a2 = SimpleOperation(id = "a2", duration = 3.0, predecessors = setOf("a1"))

        val b1 = SimpleOperation(id = "b1", duration = 3.0)
        val b2 = SimpleOperation(id = "b2", duration = 3.0, predecessors = setOf("b1"))

        val path = shortestPath(setOf(a1, a2, b1, b2))

        assertThat(path.operations).containsExactly(a1, a2)
    }


    fun `alternative parallel routes - the shortest route`() {
        val start = SimpleOperation(id = "start", duration = 1.0)
        val intermediate1 = SimpleOperation(id = "intermediate1", duration = 1.0, predecessors = setOf("start"))
        val intermediate2 = SimpleOperation(id = "intermediate2", duration = 2.0, predecessors = setOf("start"))
        val intermediate3 = SimpleOperation(id = "intermediate3", duration = 3.0, predecessors = setOf("start"))
        val end = SimpleOperation(
            id = "end",
            duration = 1.0,
            predecessors = setOf("intermediate1", "intermediate2", "intermediate3")
        )

        val path = shortestPath(setOf(start, intermediate1, intermediate2, intermediate3, end))

        assertThat(path.operations).containsExactly(start, intermediate1, end)
    }

    private fun shortestPath(operations: Set<Operation>) =
        ShortestPath(operations).find()
}
