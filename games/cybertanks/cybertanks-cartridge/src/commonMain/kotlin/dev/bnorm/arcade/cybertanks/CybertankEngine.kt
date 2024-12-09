package dev.bnorm.arcade.cybertanks

import dev.bnorm.arcade.agent.ArcadeAgent
import dev.bnorm.arcade.engine.ArcadeEngine
import dev.bnorm.arcade.engine.EngineResult
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class CybertankEngine private constructor(
    private val states: List<AgentState>,
) : ArcadeEngine {
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
                        heading = it.tankState.heading,
                        gunHeading = it.tankState.gunHeading,
                        radarHeading = it.tankState.radarHeading,
                    )
                }
            )
        )
    }

    private fun AgentState.tick() {
        // MAX: 10 degrees
        val turn = tankState.remainingTurn.coerceIn(-PI / 18.0, PI / 18.0)
        tankState.heading += turn
        tankState.remainingTurn -= turn

        // TODO ramp down velocity
        // TODO allow crossing 0.0 in single tick?
        val newVelocity = if (tankState.remainingMove < 0.0) {
            if (tankState.velocity > 0.0) {
                (tankState.velocity - 2.0).coerceAtLeast(0.0)
            } else {
                (tankState.velocity - 1.0).coerceAtLeast(-8.0)
            }
        } else {
            if (tankState.velocity < 0.0) {
                (tankState.velocity + 2.0).coerceAtMost(0.0)
            } else {
                (tankState.velocity + 1.0).coerceAtMost(8.0)
            }
        }
        tankState.remainingMove -= newVelocity - tankState.velocity
        tankState.velocity = newVelocity

        tankState.x += tankState.velocity * sin(tankState.heading)
        tankState.y += tankState.velocity * cos(tankState.heading)

        // MAX: 30 degrees
        val gunTurn = tankState.remainingGunTurn.coerceIn(-PI / 6.0, PI / 6.0)
        tankState.gunHeading += turn
        tankState.remainingGunTurn -= gunTurn

        // MAX: 45 degrees
        val radarTurn = tankState.remainingRadarTurn.coerceIn(-PI / 4.0, PI / 4.0)
        tankState.radarHeading += turn
        tankState.remainingRadarTurn -= radarTurn
    }

    class Factory : ArcadeEngine.Factory {
        override fun isSupported(agent: ArcadeAgent): Boolean {
            return agent is Cybertank
        }

        override fun create(agents: List<ArcadeAgent>): ArcadeEngine {
            val states = agents.map {
                val state = TankState(
                    x = Random.nextDouble(50.0, 750.0),
                    y = Random.nextDouble(50.0, 550.0),
                    heading = Random.nextDouble(0.0, 2 * PI),
                )
                AgentState(it as Cybertank, state)
            }
            states.forEach { it.cybertank.onStart(it.tankState) }
            return CybertankEngine(states)
        }
    }
}
