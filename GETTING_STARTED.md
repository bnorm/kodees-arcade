# Getting Started

## First

You'll need an environment you are comfortable with, in which you can develop Kotlin. Specifically, you'll need to be
able to execute Gradle commands to build a Wasm program which can be used by the Arcade.

[//]: # (TODO fill in with more specific details?)

## Running the Application

To run the desktop application, use the following Gradle command from the root directory.

```shell
> ./gradlew arcadeRun
```

Most interactions with the application happen through its menu:

* Race:
    * New - Opens a dialog window for starting a new race.
    * Load - Opens a file selector to watch a recorded race. (There is currently no exposed way to record a race, so
      this menu item is mostly useless.)
    * Download - Downloads a race from the server to watch. Server must be running at `localhost:8080`.
* Track
    * Create - Opens a track builder dialog. Built tracks are persisted local so they are available between application
      restarts.
    * Download - Downloads a track from the server to use locally. Server must be running at `localhost:8080`.
* Settings
    * Watch - Opens a directory selector and adds the directory to a list of "watched" directories for `.wasm` files.
      When a new race dialog is opened, the directories will be loaded as the "Available Drivers" section in the dialog.

## Building a Driver

To create your own driver for racing, you need to build a Wasm WASI (preview 1) program, which exports the needed
functions. For the simplest approach, there are a few available samples which can be either be copied or overwritten.
Each of these samples extend the `Driver` abstract class to help implement the required functions.

* [Snail](/arcade-player-samples/snail/README.md): A very basic driver, which slowly moves to the next checkpoint.
* [Kodee](/arcade-player-samples/kodee/README.md): Attempts to calculate and follow a racing (neither very
  successfully).

For non-Kotlin drivers, there is an available [WIT file](/arcade-player/world.wit) which describes the imports, exports,
and other data structures used for communication. There are samples of a few non-Kotlin drivers:

* [Ferris](/arcade-player-samples/ferris/README.md): Another basic driver, but written in Rust!

Each of the sample drivers directories contain a README on how to compile them and the location of the resulting `.wasm`
file.

### Callbacks

#### `fun onTurn(car: Car, controls: Controls)`

This is the only required callback for a driver. Provides information about the car (`Car`) including position and next
required checkpoint, and provides a way to control (`Controls`) the throttle and steering of the car.

#### `fun onRace(race: Race)`

Called before a race starts. Provides information about the race, including track information and number of laps.
Helpful to perform one-time calculations for the race, for example, to find the racing line of the track.

### Throttle and Steering

Your driver has two ways to control its car: throttle and steering.

#### Throttle

Controls how fast a car moves. However, unlike a real car, the throttle is used here to contorl both acceleration and
deceleration. The throttle is specified as a floating-point number (`Double`) between `1.0` and `-1.0`, where `1.0`
means maximum speed and `-1.0` means maximum reverse speed.

For the specific speed physics,
[take a look at the code](/arcade-player/src/commonMain/kotlin/dev/bnorm/arcade/rally/Physics.kt#L18).

#### Steering

Controls which way a car is turning. A floating-point number (`Double`) between `1.0` and `-1.0` can be specified, which
determines how far left (`1.0`) or right (`-1.0`) the wheels are turned. Note that depending on the car's speed, some
understeer may occur if the steering is too far in either direction. This means that to achieve the optimal turning
radius, a smaller steering value may need to be specified.

For the specific turning physics,
[take a look at the code](/arcade-player/src/commonMain/kotlin/dev/bnorm/arcade/rally/Physics.kt#L77).

### Checkpoints

To perform a lap of the track, a car must go through a series of checkpoints. Each checkpoint is described as a line
segment, with a starting and ending point. The player API includes a number of helpful geometry classes and functions,
which should make navigating to the next checkpoint easy.

### Example

Here's an example Driver!

```kotlin
object Sample : Driver() {
    private lateinit var track: Track
    override fun onRace(race: Race) {
        // Save the track for use in each 'move' call.
        this.track = race.track
    }

    override fun move(car: Car, controls: Controls) {
        // Don't worry about breaking for corners, just drive slow... for now!
        controls.throttle = 0.25

        // Figure out how to steer!
        // Get the next checkpoint Segment.
        val checkpoint = track.checkpoints[car.nextCheckpoint]
        // Calculate the Angle to the center of the checkpoint.
        val checkpointHeading = car.location.angleTo(checkpoint.center)
        // Calculate the relative Angle (bearing) to the checkpoint from our own heading.
        // 'toRelative' makes sure that angle is in the range -180.0°<..180.0°.
        val checkpointBearing = (checkpointHeading - car.velocity.angle).toRelative()

        // A -1.0 (left), 0.0, or 1.0 (right) value, which indicates the relative direction of the checkpoint.
        val checkpointDirection = sign(checkpointBearing)
        if (checkpointDirection == 0.0) {
            // Straight ahead!
            controls.steering = 0.0
        } else {
            // Calculate the turn angle at the current speed for our desired steering direction.
            // 'getTurn' is provided by the player API and is shared with the game engine.
            val turn = getTurn(speed = car.velocity.magnitude, steering = checkpointDirection)
            // Calculate the absoulte steering required to align with the checkpoint.
            val absoluteSteering = abs(checkpointBearing / turn).coerceAtMost(1.0)
            // Turn in the correct direction!
            controls.steering = checkpointDirection * absoluteSteering
        }
    }
}
```
