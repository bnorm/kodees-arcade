package dev.bnorm.arcade.machine

import io.ktor.util.cio.use
import io.ktor.util.cio.writeChannel
import java.nio.file.Path

// TODO is there a way to create this with JS as well?
//  - filekit is great for desktop
//  - but the API is not great for dealing with large files...
fun RecordGame(game: Game, path: Path): WriteGame {
    return WriteGame(game) { writer ->
        path.toFile().writeChannel().use {
            writer(this)
        }
    }
}
