package com.zymosi3.lines.engine

import java.util.ArrayList
import java.util.Random
import java.util.LinkedList

public enum class Color {
    red
    blue
    green
    yellow
    purple
    aqua
    burgundy
}

public class Ball(color: Color) {
    public val color: Color = color
}

public class Cell(x: Int, y: Int) {

    public val x: Int = x
    public val y: Int = y
    public var ball: Ball? = null
        internal set
}

public class Field(size: Int) :  Iterable<Cell> {

    public val size: Int = size
    internal val cells: List<Cell> = Array(size * size, {i -> Cell(i / size, i % size)}).toList()
    private val cellsMap: Map<Pair<Int, Int>, Cell> = cells.toMap({cell -> Pair(cell.x, cell.y)})

    public fun cell(x: Int, y: Int): Cell = cellsMap.get(Pair(x, y)) ?: throw IllegalArgumentException("Can't find cell {$x, $y}")

    override fun iterator(): Iterator<Cell> = cells.iterator()
}

public class Game(size: Int, startBalls: Int) {

    private val startBalls: Int = startBalls
    public val field: Field = Field(size)
    public var nextBalls: List<Ball> = emptyList()
        private set
    public var currentScore: Int = 0
        private set

    private val occupied: MutableList<Cell> = LinkedList()
    private val free: MutableList<Cell> = LinkedList(field.cells)

    private val rand: Random = Random()

    public fun start() {
        for (i in 1..startBalls) {
            val index = rand.nextInt(free.size())
            val cell = free.get(index)
            cell.ball = Ball(randColor())
            free.remove(index)
            occupied.add(cell)
        }
        // TODO what if we have 5 in row now?
    }

    private fun randColor(): Color = Color.values().get(rand.nextInt(Color.values().size()))
}


