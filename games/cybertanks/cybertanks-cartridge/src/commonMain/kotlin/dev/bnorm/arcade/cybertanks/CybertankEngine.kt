package dev.bnorm.arcade.cybertanks

import dev.bnorm.arcade.agent.ArcadeAgent
import dev.bnorm.arcade.engine.ArcadeEngine
import dev.bnorm.arcade.engine.EngineResult
import dev.bnorm.arcade.engine.EngineState
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class CybertankEngine private constructor(
    private val battleFieldWidth: Int,
    private val battleFieldHeight: Int,
    private val states: List<AgentState>,
) : ArcadeEngine {
    override fun init(): EngineState {
        states.forEach { it.cybertank.onStart(it.tankState) }
        return CybertankEngineState(
            tanks = states.map {
                CybertankEngineState.Tank(
                    x = it.tankState.x,
                    y = it.tankState.y,
                    heading = it.tankState.heading.radians,
                    gunHeading = it.tankState.gunHeading.radians,
                    radarHeading = it.tankState.radarHeading.radians,
                )
            }
        )
    }

    override fun advance(): EngineResult {
        for (state in states) {
            state.cybertank.onTurn(state.tankState)
        }
        for (state in states) {
            state.tick()
        }
        return EngineResult.Running(
            CybertankEngineState(
                tanks = states.map {
                    CybertankEngineState.Tank(
                        x = it.tankState.x,
                        y = it.tankState.y,
                        heading = it.tankState.heading.radians,
                        gunHeading = it.tankState.gunHeading.radians,
                        radarHeading = it.tankState.radarHeading.radians,
                    )
                }
            )
        )
    }

    // https://robowiki.net/wiki/Robocode/Game_Physics#Robocode_processing_loop
    private fun AgentState.tick() {
        // TODO will coerceIn cause boxing of inline class Angle?
        //  if so, specialize to increase performance?

        // Turn gun.
        val gunTurn = tankState.remainingGunTurn.coerceIn(-Physics.GUN_TURN_RATE, Physics.GUN_TURN_RATE)
        tankState.gunHeading = (tankState.gunHeading + gunTurn).toAbsolute()
        tankState.remainingGunTurn -= gunTurn

        // Turn radar.
        val radarTurn = tankState.remainingRadarTurn.coerceIn(-Physics.RADAR_TURN_RATE, Physics.RADAR_TURN_RATE)
        tankState.radarHeading = (tankState.radarHeading + radarTurn).toAbsolute()
        tankState.remainingRadarTurn -= radarTurn

        // Move tank.
        // https://robowiki.net/wiki/User:Positive/Optimal_Velocity#Nat.27s_updateMovement
        // TODO having a `remainingMove` makes the engine logic complex.
        //  most Robocode bots don't really care about distance.
        //  instead of `tank.move(dist)`, should it be `tank.speed(s)`?
        val tankTurn = simulateTurn(tankState.speed, tankState.remainingTurn)
        val newSpeed = simulateSpeed(tankState.speed, tankState.remainingMove)
        tankState.heading = (tankState.heading + tankTurn).toAbsolute()
        tankState.speed = newSpeed
        tankState.remainingTurn -= tankTurn
        tankState.remainingMove -= newSpeed

        tankState.x += newSpeed * sin(tankState.heading)
        tankState.y += newSpeed * cos(tankState.heading)
        if (tankState.x !in 16.0..battleFieldWidth - 16.0 || tankState.y !in 16.0..battleFieldHeight - 16.0) {
            tankState.x = tankState.x.coerceIn(16.0, battleFieldWidth - 16.0)
            tankState.y = tankState.y.coerceIn(16.0, battleFieldHeight - 16.0)
            tankState.speed = 0.0
        }
    }

    class Factory : ArcadeEngine.Factory {
        override fun isSupported(agent: ArcadeAgent): Boolean {
            return agent is Cybertank
        }

        override fun create(agents: List<ArcadeAgent>): ArcadeEngine {
            val battleFieldWidth = 800
            val battleFieldHeight = 600
            val states = agents.map {
                val state = TankState(
                    x = Random.nextDouble(50.0, battleFieldWidth - 50.0),
                    y = Random.nextDouble(50.0, battleFieldHeight - 50.0),
                    heading = Angle.ofRadians(Random.nextDouble(0.0, 2 * PI)),
                )
                AgentState(it as Cybertank, state)
            }
            return CybertankEngine(battleFieldWidth, battleFieldHeight, states)
        }
    }
}

private fun simulateTurn(currentSpeed: Double, turn: Angle): Angle = when {
    turn < Angle.ZERO -> -simulateTurn(currentSpeed, -turn)
    else -> turn.coerceAtMost(Physics.getTurnRate(currentSpeed))
}

// https://robowiki.net/wiki/User:Voidious/Optimal_Velocity#Hijack_2
private fun simulateSpeed(speed: Double, distance: Double): Double {
    // Flip function so distance is always positive.
    if (distance < 0.0) return -simulateSpeed(-speed, -distance)

    val newSpeed = when (distance) {
        Double.POSITIVE_INFINITY -> Physics.MAX_SPEED
        else -> min(getMaxVelocity(distance), Physics.MAX_SPEED)
    }

    return when {
        speed >= 0.0 -> newSpeed.coerceIn(speed - Physics.DECELERATION, speed + Physics.ACCELERATION)
        else -> newSpeed.coerceIn(speed - Physics.ACCELERATION, speed + maxDecel(-speed))
    }
}

private fun getMaxVelocity(distance: Double): Double {
    // sum of 0... decelTime, solving for decelTime using quadratic formula
    val sqrt = sqrt((4.0 * 2.0 / Physics.DECELERATION) * distance + 1.0)
    val decelTime = ceil((sqrt - 1.0) / 2.0).coerceAtLeast(1.0)

    if (decelTime == Double.POSITIVE_INFINITY) return Physics.MAX_SPEED

    // sum of 0..(decelTime-1)
    val decelDist = (decelTime / 2.0) * (decelTime - 1.0) * Physics.DECELERATION
    return ((decelTime - 1.0) * Physics.DECELERATION) + ((distance - decelDist) / decelTime)
}

private fun maxDecel(speed: Double): Double {
    val decelTime = speed / Physics.DECELERATION
    val accelTime = 1.0 - decelTime
    return decelTime.coerceAtMost(1.0) * Physics.DECELERATION +
            accelTime.coerceAtLeast(0.0) * Physics.ACCELERATION
}
