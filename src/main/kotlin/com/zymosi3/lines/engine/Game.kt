package com.zymosi3.lines.engine

import java.util.ArrayList
import java.util.Random
import java.util.LinkedList
import java.util.HashSet

public enum class Color {
    red
    blue
    green
    gold
    indigo
    aqua
    burgundy
}

public class Ball(val color: Color)

public class Cell(val x: Int, val y: Int) {

    public var ball: Ball? = null
        internal set
}

public enum class Direction {
    left
    right
    top
    bot
}

public class Field(val size: Int) :  Iterable<Cell> {

    internal val cells: List<Cell> = Array(size * size, {i -> Cell(i / size, i % size)}).toList()
    private val cellsMap: Map<Pair<Int, Int>, Cell> = cells.toMap({cell -> Pair(cell.x, cell.y)})

    public fun cell(x: Int, y: Int): Cell? = cellsMap.get(Pair(x, y))

    public fun left(cell: Cell): Cell? = cell(cell.x - 1, cell.y)

    public fun right(cell: Cell): Cell? = cell(cell.x + 1, cell.y)

    public fun top(cell: Cell): Cell? = cell(cell.x, cell.y - 1)

    public fun bot(cell: Cell): Cell? = cell(cell.x, cell.y + 1)

    public fun cell(cell: Cell, dir: Direction): Cell? =
        when (dir) {
            Direction.left -> left(cell)
            Direction.right -> right(cell)
            Direction.top -> top(cell)
            Direction.bot -> bot(cell)
        }

    override fun iterator(): Iterator<Cell> = cells.iterator()
}

public data class MoveResult(
        val success: Boolean,
        val purged: Set<Cell>,
        val gameFinished: Boolean
)

public class Game(size: Int, startBalls: Int, randomSeed: Long? = null) {

    // constants
    private val nextBallsCount = 3
    private val minLineSize = 5

    private val startBalls: Int = startBalls
    public val field: Field = Field(size)
    public var nextBalls: List<Ball> = emptyList()
        private set
    public var currentScore: Int = 0
        private set
    public var movesNum:Int = 0
        private set

    internal val occupied: MutableList<Cell> = LinkedList()
    internal val free: MutableList<Cell> = LinkedList(field.cells)

    private val rand: Random = if (randomSeed != null) Random(randomSeed) else Random()

    public fun start(): MoveResult {
        for (i in 1..startBalls) {
            putRandom(Ball(randColor()))
        }
        nextBalls = defineNextBalls()
        return MoveResult(true, purge(), free.size() <= nextBalls.size())
    }

    public fun move(from: Cell, to: Cell): MoveResult {
        if (isLegal(from, to)) {
            movesNum++
            to.ball = from.ball
            from.ball = null
            occupied.remove(from)
            occupied.add(to)
            free.add(from)
            free.remove(to)
            val purged = purge()
            if (purged.isNotEmpty()) {
                // update score
                currentScore += minLineSize
                if (purged.size() > minLineSize) {
                    for (i in minLineSize + 1..purged.size()) {
                        currentScore += (i - minLineSize) * (i - minLineSize + 1)
                    }
                }
            }
            if (free.size() > nextBalls.size()) {
                if (purged.isEmpty()) {
                    // put new balls
                    for (ball in nextBalls) {
                        putRandom(ball)
                    }
                    val purgedAgain = purge()
                    nextBalls = defineNextBalls()
                    return MoveResult(true, purgedAgain, false)
                } else {
                    return MoveResult(true, purged, false)
                }
            } else {
                // game over
                return MoveResult(true, purged, true)
            }
        } else {
            // illegal move
            return MoveResult(false, emptySet(), free.size() <= nextBalls.size())
        }
    }

    private fun isLegal(from: Cell, to: Cell): Boolean {
        // using something like interpretation of Dijkstra algorithm
        fun explore(cur: Cell, explored: Collection<Cell>, toExplore: MutableList<Pair<Cell, Direction>>) {
            for (dir in Direction.values()) {
                val cell = field.cell(cur, dir)
                if (cell != null && !explored.contains(cell))
                    toExplore.add(Pair(cur, dir))
            }
        }

        fun isLegal(from: Cell, to: Cell, explored: MutableCollection<Cell>, toExplore: MutableList<Pair<Cell, Direction>>): Boolean {
            if (from == to)
                return true
            explored.add(from)
            explore(from, explored, toExplore)
            if (toExplore.isEmpty())
                return false
            val next = toExplore.get(0)
            toExplore.remove(0)
            return isLegal(field.cell(next.first, next.second) as Cell, to, explored, toExplore)
        }
        return isLegal(from, to, HashSet(occupied), LinkedList())
    }

    private fun purge(): Set<Cell> {
        val purged: MutableSet<Cell> = HashSet()

        fun purgeLine(line: List<Cell>) {
            if (line.size() >= minLineSize)
                purged.addAll(line)
        }

        fun addToLine(cell: Cell, line: MutableList<Cell>) {
            if (cell.ball != null)
                if (line.isEmpty())
                    // no matter what color, line is empty
                    line.add(0, cell)
                else if (line.get(0).ball?.color == cell.ball?.color)
                    // line is not empty we need the same color
                    line.add(0, cell)
                else {
                    purgeLine(line)
                    // we have another color so clear current line and start new
                    line.clear()
                    line.add(0, cell)
                }
            else
                purgeLine(line)
        }
        // passing row by row
        for (i in 0..field.size - 1) {
            val line: MutableList<Cell> = LinkedList()
            for (j in 0..field.size - 1)
                addToLine(field.cell(j, i) as Cell, line)
            purgeLine(line)
        }
        // passing column by column
        for (i in 0..field.size - 1) {
            val line: MutableList<Cell> = LinkedList()
            for (j in 0..field.size - 1)
                addToLine(field.cell(i, j) as Cell, line)
            purgeLine(line)
        }
        // passing diagonals from left top corner to bottom \
        for (k in 0..field.size - 1) {
            val line: MutableList<Cell> = LinkedList()
            for (l in 0..field.size - 1 - k) {
                val i = l
                val j = k + l
                addToLine(field.cell(i, j) as Cell, line)
            }
            purgeLine(line)
        }
        // passing diagonals from left top corner to right \
        for (k in 1..field.size - 1) {
            val line: MutableList<Cell> = LinkedList()
            for (l in k..field.size - 1) {
                val i = l
                val j = l - k
                addToLine(field.cell(i, j) as Cell, line)
            }
            purgeLine(line)
        }
        // passing diagonals from left top corner to right /
        for (k in 0..field.size - 1) {
            val line: MutableList<Cell> = LinkedList()
            for (l in k downTo 0) {
                val i = l
                val j = k - l
                addToLine(field.cell(i, j) as Cell, line)
            }
            purgeLine(line)
        }
        // passing diagonals from right top corner to bottom /
        for (k in 1..field.size - 1) {
            val line: MutableList<Cell> = LinkedList()
            for (l in field.size - 1 downTo k) {
                val i = l
                val j = field.size - 1 - l + k
                addToLine(field.cell(i, j) as Cell, line)
            }
            purgeLine(line)
        }
        // clear purged cells
        for (cell in purged) {
            cell.ball = null
            free.add(cell)
            occupied.remove(cell)
        }
        return purged
    }

    private fun putRandom(ball: Ball) {
        val index = rand.nextInt(free.size())
        val cell = free.get(index)
        cell.ball = ball
        free.remove(index)
        occupied.add(cell)
    }

    private fun defineNextBalls(): List<Ball> = Array(nextBallsCount, {i -> Ball(randColor())}).toList()

    private fun randColor(): Color = Color.values().get(rand.nextInt(Color.values().size()))
}


