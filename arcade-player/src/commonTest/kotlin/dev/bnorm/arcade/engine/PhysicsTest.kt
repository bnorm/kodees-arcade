package dev.bnorm.arcade.engine

import dev.bnorm.arcade.rally.ACCELERATION
import dev.bnorm.arcade.rally.BOOST_DEGRADE
import dev.bnorm.arcade.rally.DECELERATION
import dev.bnorm.arcade.rally.MAX_BOOST_THROTTLE
import dev.bnorm.arcade.rally.MAX_SPEED
import dev.bnorm.arcade.rally.MAX_SPEED_BOOST
import dev.bnorm.arcade.rally.MAX_THROTTLE
import dev.bnorm.arcade.rally.MIN_SPEED
import dev.bnorm.arcade.rally.MIN_THROTTLE
import dev.bnorm.arcade.rally.simulateSpeed
import kotlin.test.Test
import kotlin.test.assertEquals

class PhysicsTest {
    // ===== Throttle Control ===== //

    @Test
    fun `test max speed forward`() {
        val start = MAX_SPEED
        val end = simulateSpeed(start, boost = 0.0, throttle = Double.MAX_VALUE)
        assertEquals(start, end)
    }

    @Test
    fun `test max speed reverse`() {
        val start = MIN_SPEED
        val end = simulateSpeed(start, boost = 0.0, throttle = -Double.MAX_VALUE)
        assertEquals(start, end)
    }

    @Test
    fun `test forward acceleration`() {
        val start = 1.0
        val end = simulateSpeed(start, boost = 0.0, throttle = MAX_THROTTLE)
        assertEquals(start + ACCELERATION, end)
    }

    @Test
    fun `test reverse acceleration`() {
        val start = -1.0
        val end = simulateSpeed(start, boost = 0.0, throttle = MIN_THROTTLE)
        assertEquals(start - ACCELERATION, end)
    }

    @Test
    fun `test forward partial acceleration`() {
        val start = 0.0
        val end = simulateSpeed(start, boost = 0.0, throttle = (ACCELERATION / 2) / MAX_SPEED)
        assertEquals(start + ACCELERATION / 2, end)
    }

    @Test
    fun `test reverse partial acceleration`() {
        val start = 0.0
        val end = simulateSpeed(start, boost = 0.0, throttle = (ACCELERATION / 2) / MIN_SPEED)
        assertEquals(start - ACCELERATION / 2, end)
    }

    @Test
    fun `test forward deceleration`() {
        val start = 1.0
        val end = simulateSpeed(start, boost = 0.0, throttle = MIN_THROTTLE)
        assertEquals(start - DECELERATION, end)
    }

    @Test
    fun `test reverse deceleration`() {
        val start = -1.0
        val end = simulateSpeed(start, boost = 0.0, throttle = MAX_THROTTLE)
        assertEquals(start + DECELERATION, end)
    }

    @Test
    fun `test forward partial deceleration`() {
        val start = MAX_SPEED
        val end = simulateSpeed(start, boost = 0.0, throttle = MAX_THROTTLE - (DECELERATION / 2) / MAX_SPEED)
        assertEquals(start - DECELERATION / 2, end)
    }

    @Test
    fun `test reverse partial deceleration`() {
        val start = MIN_SPEED
        val end = simulateSpeed(start, boost = 0.0, throttle = MIN_THROTTLE - (DECELERATION / 2) / MIN_SPEED)
        assertEquals(start + DECELERATION / 2, end)
    }

    // ===== Direction Change ===== //

    @Test
    fun `test reverse to forward`() {
        val end = simulateSpeed(speed = -DECELERATION / 2, boost = 0.0, throttle = MAX_THROTTLE)
        assertEquals(ACCELERATION / 2, end)
    }

    @Test
    fun `test forward to reverse`() {
        val end = simulateSpeed(speed = DECELERATION / 2, boost = 0.0, throttle = MIN_THROTTLE)
        assertEquals(-ACCELERATION / 2, end)
    }

    // ===== Speed Boost ===== //

    @Test
    fun `test max speed boost forward`() {
        val start = MAX_SPEED + MAX_SPEED_BOOST
        val end = simulateSpeed(start, boost = MAX_SPEED_BOOST, throttle = Double.MAX_VALUE)
        assertEquals(start, end)
    }

    @Test
    fun `test speed boost forward acceleration`() {
        val start = MAX_SPEED
        val end = simulateSpeed(start, boost = MAX_SPEED_BOOST, throttle = MAX_BOOST_THROTTLE)
        assertEquals(start + ACCELERATION, end)
    }

    @Test
    fun `test speed boost deceleration`() {
        val start = MAX_SPEED + MAX_SPEED_BOOST
        val end = simulateSpeed(start, boost = 0.0, throttle = MAX_THROTTLE)
        assertEquals(start - DECELERATION, end)
    }

    @Test
    fun `test speed boost degradation`() {
        val start = MAX_SPEED + MAX_SPEED_BOOST
        val end = simulateSpeed(start, boost = 0.0, throttle = MAX_BOOST_THROTTLE)
        assertEquals(start - BOOST_DEGRADE, end)
    }

    @Test
    fun `test no speed boost reverse`() {
        val start = MIN_SPEED
        val end = simulateSpeed(start, boost = MAX_SPEED_BOOST, throttle = -Double.MAX_VALUE)
        assertEquals(start, end)
    }
}
