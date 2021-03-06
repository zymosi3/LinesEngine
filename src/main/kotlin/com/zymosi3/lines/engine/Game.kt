package com.zymosi3.lines.engine

import java.util.ArrayList
import java.util.Random
import java.util.LinkedList
import java.util.HashSet
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

public enum class Color(public val index: Int) {
    red(0),
    blue(1),
    green(2),
    gold(3),
    indigo(4),
    aqua(5),
    burgundy(6)
}

public class Ball(val color: Color)

public class Cell(val x: Int, val y: Int) {

    public var ball: Ball? = null
        internal set
}

public enum class Direction {
    left,
    right,
    top,
    bot,
}

public class Field(val size: Int) :  Iterable<Cell> {

    internal val cells: List<Cell> = Array(size * size, {i -> Cell(i / size, i % size)}).toList()
    private val cellsMap: Map<Pair<Int, Int>, Cell> = cells.toMap({cell -> Pair(cell.x, cell.y)})

    public fun cell(x: Int, y: Int): Cell? = cellsMap[Pair(x, y)]

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

public fun restore(snapshot: ByteArray): Game {
    val stream = ByteArrayInputStream(snapshot)
    stream.use {
        val reader = stream.reader()
        reader.use {
            val size = reader.read()
            val game = Game(size)
            val occupiedSize = reader.read()
            for (i in 1..occupiedSize) {
                val x = reader.read()
                val y = reader.read()
                val color = Color.values()[reader.read()]
                val cell = game.field.cell(x, y) as Cell
                cell.ball = Ball(color)
                game.occupied.add(cell)
                game.free.remove(cell)
            }
            val nextBallsSize = reader.read()
            val nextBalls = ArrayList<Ball>(nextBallsSize)
            for (i in 1.. nextBallsSize) {
                val color = Color.values()[reader.read()]
                nextBalls.add(Ball(color))
            }
            game.nextBalls = nextBalls
            game.score = reader.read()
            game.movesNum = reader.read()
            return game
        }
    }
}

public class Game(size: Int, randomSeed: Long? = null) {

    // constants
    private val nextBallsCount = 3
    private val minLineSize = 5

    public val field: Field = Field(size)
    public var nextBalls: List<Ball> = emptyList()
        internal set
    public var score: Int = 0
        internal set
    public var movesNum:Int = 0
        internal set

    internal val occupied: MutableList<Cell> = LinkedList()
    internal val free: MutableList<Cell> = LinkedList(field.cells)

    private val rand: Random = if (randomSeed != null) Random(randomSeed) else Random()

    public fun start(): MoveResult {
        for (i in 1..nextBallsCount) {
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
                score += minLineSize
                if (purged.size() > minLineSize) {
                    for (i in minLineSize + 1..purged.size()) {
                        score += (i - minLineSize) * (i - minLineSize + 1)
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

    public fun snapshot(): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.use {
            val writer = stream.writer()
            writer.use {
                writer.write(field.size)
                writer.write(occupied.size())
                for (cell in occupied) {
                    writer.write(cell.x)
                    writer.write(cell.y)
                    writer.write(cell.ball?.color?.index as Int)
                }
                writer.write(nextBalls.size())
                for (ball in nextBalls) {
                    writer.write(ball.color.index)
                }
                writer.write(score)
                writer.write(movesNum)
            }
            return stream.toByteArray()
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

        var cur = from
        val explored = HashSet(occupied)
        val toExplore = LinkedList<Pair<Cell, Direction>>()

        while (true) {
            if (cur == to)
                return true
            explored.add(cur)
            explore(cur, explored, toExplore)
            if (toExplore.isEmpty())
                return false
            val next = toExplore[0]
            toExplore.remove(0)
            cur = field.cell(next.first, next.second) as Cell
        }
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
                else if (line[0].ball?.color == cell.ball?.color)
                    // line is not empty we need the same color
                    line.add(0, cell)
                else {
                    purgeLine(line)
                    // we have another color so clear current line and start new
                    line.clear()
                    line.add(0, cell)
                }
            else {
                purgeLine(line)
                line.clear()
            }
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
        val cell = free[index]
        cell.ball = ball
        free.remove(index)
        occupied.add(cell)
    }

    private fun defineNextBalls(): List<Ball> = Array(nextBallsCount, {i -> Ball(randColor())}).toList()

    private fun randColor(): Color = Color.values()[rand.nextInt(Color.values().size())]
}


