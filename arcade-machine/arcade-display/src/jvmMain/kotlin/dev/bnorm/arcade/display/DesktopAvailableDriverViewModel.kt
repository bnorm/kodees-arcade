package dev.bnorm.arcade.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import dev.bnorm.arcade.Cache
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.nameWithoutExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding<AvailableDriverViewModel>())
class DesktopAvailableDriverViewModel(
    @ViewModelCoroutineScope scope: CoroutineScope,
    private val cache: Cache<PlatformFile>,
) : ViewModel<AvailableDriverEvent, AvailableDriverModel>(scope), AvailableDriverViewModel {
    override fun watch(directory: PlatformFile) {
        take(AvailableDriverEvent.Watch(directory))
    }

    override fun unwatch(watched: AvailableDriverModel.Watched) {
        take(AvailableDriverEvent.Unwatch(watched))
    }

    @Composable
    override fun models(events: Flow<AvailableDriverEvent>): AvailableDriverModel {
        return AvailableDriverPresenter(events, cache)
    }
}

sealed class AvailableDriverEvent {
    data class Watch(val directory: PlatformFile) : AvailableDriverEvent()
    data class Unwatch(val watched: AvailableDriverModel.Watched) : AvailableDriverEvent()
}

@Composable
fun AvailableDriverPresenter(
    events: Flow<AvailableDriverEvent>,
    cache: Cache<PlatformFile>,
): AvailableDriverModel {
    val keys = remember(cache) {
        mutableStateSetOf<String>().apply {
            addAll(cache.keys)
        }
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is AvailableDriverEvent.Watch -> {
                    val key = event.directory.hashCode().toString()
                    cache[key] = event.directory
                    keys.add(key)
                }

                is AvailableDriverEvent.Unwatch -> {
                    val key = event.watched.key
                    cache.remove(key)
                    keys.remove(key)
                }
            }
        }
    }

    val watched = keys.mapNotNull {
        val directory = cache[it] ?: return@mapNotNull null
        AvailableDriverModel.Watched(it, directory)
    }

    return AvailableDriverModel(
        watched = watched,
        drivers = buildList {
            for (w in watched) {
                for (file in w.directory.list()) {
                    if (file.extension == "wasm") {
                        add(AvailableDriverModel.Driver(file.nameWithoutExtension, file))
                    }
                }
            }
        }
    )
}
