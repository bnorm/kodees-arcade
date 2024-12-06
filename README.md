# Kodee's Arcade

## Project Structure

This project is broken down into the following components:

### [Arcade Core](./arcade-core/README.md)

The core module contains various utilities used across all other modules.

### [Arcade Agent](./arcade-agent/README.md)

The agent module is responsible for loading game agents.

Open questions:

* [ ] Where does the game agent API live? Is this the API module instead?

### [Arcade Engine](./arcade-engine/README.md)

The engine module is responsible for loading and running game engines.

Open questions:

* [ ] Where does the game engine API live? Is this the API module instead?

### [Arcade Runner](./arcade-runner/README.md)

The runner module is responsible for running a game — engine and agents — and producing a stream of
data which can be rendered or saved to a file.

### [Arcade Render](./arcade-render/README.md)

The render module is responsible for loading and executing game renderers.

Open questions:

* [ ] Where does the game render API live? Is this the API module instead?

### [Arcade UI](./arcade-ui/README.md)

The UI module is responsible for creating the surrounding application UI elements.

### [Arcade Main](./arcade-main/README.md)

The main module is the entry point for running the desktop application.

Open questions:

* [ ] Is this also where the web entry point is?

## Game Infrastructure

WIP

I see three things needed for defining a game within the arcade:

1. The agent API:
    * Used by the end user to create their agent(s) for the desired game.
    * Defines how an agent interacts with the game.
    * Each agent API should use the arcade agent API as its base, so the arcade knows which game
      engine is used to run the agent.

2. The game engine:
    * Used by the arcade to execute the game.
    * Defines which agent APIs are supported by the game.
    * Defines how a game can be configured (DSL?): agent selection rules, game parameters, etc.
    * Responsible for producing the initial "state" of the game, the next state of the game given
      the previous state, determining when the game is complete, etc. The set of game agents will
      also be provided for all of these, so they can be "executed" as needed.

3. The game renderer:
    * Used by the arcade to render the game.
    * Responsible for rendering a "state" of the game onto the screen (Skia Canvas?).

While the engine and renderer will probably come packaged together, it is ideal that they are
separate parts so the game can be executed without a UI for batch processing. This also enables
recording of games which can be replayed at a later time.

Open questions:

* [ ] Should the game API be a single artifact that includes both the engine and renderer? This way
  a game is able to implement both parts easier?
* [ ] Are there games that may not want a renderer?
