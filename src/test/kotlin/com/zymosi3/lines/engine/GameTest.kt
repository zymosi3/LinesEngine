package com.zymosi3.lines.engine

import org.junit.Test as test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.util.Random
import kotlin.test.assertNull
import kotlin.test.assertNotNull

public class GameTest {

    val size = 9
    val startBalls = 3
    val seed = 42L

    test fun createGameTest() {
        val game = Game(size, startBalls, seed)
        assertEquals(size, game.field.size)
        assertTrue(game.nextBalls.isEmpty())
        assertEquals(0, game.currentScore)
        assertTrue(game.occupied.isEmpty())
        assertEquals(size * size, game.free.size())
    }

    test fun startGameTest() {
        val game = Game(size, startBalls, seed)
        val res = game.start()
        assertTrue(res.success)
        assertTrue(res.purged.isEmpty())
        assertFalse(res.gameFinished)
        assertEquals(3, game.nextBalls.size())
        assertEquals(0, game.currentScore)
        assertEquals(startBalls, game.occupied.size())
        assertEquals(size * size - startBalls, game.free.size())
    }

    test fun finishGameTest() {
        val game = Game(size, startBalls, seed)
        game.start()
        val rand = Random(seed)
        fun randomMove(): MoveResult = game.move(
                game.occupied.get(rand.nextInt(game.occupied.size())),
                game.free.get(rand.nextInt(game.free.size()))
        )
        var res: MoveResult
        while (true) {
            res = randomMove()
            while (! res.success) {
                res = randomMove()
            }
            if (res.gameFinished) {
                break
            }
        }
        assertTrue(res.gameFinished)
        assertEquals(3, game.free.size())
        assertEquals(78, game.occupied.size())
        assertEquals(0, game.currentScore)
        assertEquals(26, game.movesNum)
    }

    test fun moveTest() {
        val game = Game(size, startBalls, seed)
        game.start()
        val field = game.field
        val from = field.cell(3, 3) as Cell
        val to = field.cell(5, 2) as Cell

        assertTrue(game.occupied.contains(from))
        assertFalse(game.occupied.contains(to))
        assertFalse(game.free.contains(from))
        assertTrue(game.free.contains(to))
        assertNotNull(from.ball)
        assertNull(to.ball)

        val res = game.move(from, to)

        assertTrue(res.success)
        assertTrue(res.purged.isEmpty())
        assertFalse(res.gameFinished)
        assertFalse(game.occupied.contains(from))
        assertTrue(game.occupied.contains(to))
        assertTrue(game.free.contains(from))
        assertFalse(game.free.contains(to))
        assertNull(from.ball)
        assertNotNull(to.ball)
    }

    test fun purgeRowTest() {
        val game = Game(size, startBalls, seed)
        game.start()
        val field = game.field
        val line = listOf(field.cell(1, 2), field.cell(2, 2), field.cell(3, 2), field.cell(4, 2))
        for (cell in line) {
            cell?.ball = Ball(Color.blue)
            game.occupied.add(cell as Cell)
            game.free.remove(cell)
        }
        val from = field.cell(3, 3) as Cell
        val to = field.cell(5, 2) as Cell
        val res = game.move(from, to)

        assertTrue(res.success)
        assertEquals(5, res.purged.size())
        assertFalse(res.gameFinished)
        assertEquals(5, game.currentScore)
    }

    test fun scoreTest() {
        val game = Game(size, startBalls, seed)
        game.start()
        val field = game.field
        val line = listOf(field.cell(1, 2), field.cell(2, 2), field.cell(3, 2), field.cell(4, 2), field.cell(6, 2), field.cell(7, 2), field.cell(8, 2))
        for (cell in line) {
            cell?.ball = Ball(Color.blue)
            game.occupied.add(cell as Cell)
            game.free.remove(cell)
        }
        val from = field.cell(3, 3) as Cell
        val to = field.cell(5, 2) as Cell
        val res = game.move(from, to)

        assertTrue(res.success)
        assertEquals(8, res.purged.size())
        assertFalse(res.gameFinished)
        assertEquals(25, game.currentScore)
    }

    test fun purgeColumnTest() {
        val game = Game(size, startBalls, seed)
        game.start()
        val field = game.field
        val line = listOf(field.cell(4, 0), field.cell(4, 1), field.cell(4, 2), field.cell(4, 5))
        for (cell in line) {
            cell?.ball = Ball(Color.blue)
            game.occupied.add(cell as Cell)
            game.free.remove(cell)
        }
        val from = field.cell(3, 3) as Cell
        val to = field.cell(4, 3) as Cell
        val res = game.move(from, to)

        assertTrue(res.success)
        assertEquals(5, res.purged.size())
        assertFalse(res.gameFinished)
        assertEquals(5, game.currentScore)
    }

    test fun purgeDiagonalTest1() {
        val game = Game(size, startBalls, seed)
        game.start()
        val field = game.field
        val line = listOf(field.cell(0, 2), field.cell(1, 3), field.cell(2, 4), field.cell(4, 6))
        for (cell in line) {
            cell?.ball = Ball(Color.blue)
            game.occupied.add(cell as Cell)
            game.free.remove(cell)
        }
        val from = field.cell(3, 3) as Cell
        val to = field.cell(3, 5) as Cell
        val res = game.move(from, to)

        assertTrue(res.success)
        assertEquals(5, res.purged.size())
        assertFalse(res.gameFinished)
        assertEquals(5, game.currentScore)
    }

    test fun purgeDiagonalTest2() {
        val game = Game(size, startBalls, seed)
        game.start()
        val field = game.field
        val line = listOf(field.cell(4, 1), field.cell(5, 2), field.cell(6, 3), field.cell(8, 5))
        for (cell in line) {
            cell?.ball = Ball(Color.blue)
            game.occupied.add(cell as Cell)
            game.free.remove(cell)
        }
        val from = field.cell(3, 3) as Cell
        val to = field.cell(7, 4) as Cell
        val res = game.move(from, to)

        assertTrue(res.success)
        assertEquals(5, res.purged.size())
        assertFalse(res.gameFinished)
        assertEquals(5, game.currentScore)
    }

    test fun purgeDiagonalTest3() {
        val game = Game(size, startBalls, seed)
        game.start()
        val field = game.field
        val line = listOf(field.cell(6, 2), field.cell(5, 3), field.cell(4, 4), field.cell(2, 6), field.cell(1, 7))
        for (cell in line) {
            cell?.ball = Ball(Color.blue)
            game.occupied.add(cell as Cell)
            game.free.remove(cell)
        }
        val from = field.cell(3, 3) as Cell
        val to = field.cell(3, 5) as Cell
        val res = game.move(from, to)

        assertTrue(res.success)
        assertEquals(6, res.purged.size())
        assertFalse(res.gameFinished)
        assertEquals(7, game.currentScore)
    }

    test fun purgeDiagonalTest4() {
        val game = Game(size, startBalls, seed)
        game.start()
        val field = game.field
        val line = listOf(field.cell(8, 4), field.cell(7, 5), field.cell(5, 7), field.cell(4, 8))
        for (cell in line) {
            cell?.ball = Ball(Color.blue)
            game.occupied.add(cell as Cell)
            game.free.remove(cell)
        }
        val from = field.cell(3, 3) as Cell
        val to = field.cell(6, 6) as Cell
        val res = game.move(from, to)

        assertTrue(res.success)
        assertEquals(5, res.purged.size())
        assertFalse(res.gameFinished)
        assertEquals(5, game.currentScore)
    }
}


