package bruhcollective.itaysonlab.jetispot.core.lyrics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spotify.lyrics.v2.lyrics.proto.LyricsResponse.LyricsLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import xyz.gianlu.librespot.common.Base62
import javax.inject.Inject

class SpLyricsController @Inject constructor(
    private val requester: SpLyricsRequester
): CoroutineScope by MainScope() {
    private val base62 = Base62.createInstanceWithInvertedCharacterSet()
    private var _songJob: Job? = null

    var currentSongLine by mutableStateOf("")
        private set

    var currentSongLineIndex by mutableIntStateOf(-1)
        private set

    var currentLyricsLines by mutableStateOf<List<LyricsLine>>(emptyList())
        private set

    var currentLyricsState by mutableStateOf(LyricsState.Loading)
        private set

    fun setSong(track: com.spotify.metadata.Metadata.Track) {
        _songJob?.cancel()
        _songJob = launch {
            currentSongLineIndex = -1
            if (track.hasLyrics) {
                currentLyricsState = LyricsState.Loading

                val response = requester.request(
                    base62.encode(track.gid.toByteArray(), 22).decodeToString()
                )

                if (response == null || response.lyrics == null) {
                    currentLyricsState = LyricsState.Unavailable
                } else {
                    currentLyricsLines = response.lyrics.linesList ?: emptyList()
                }
            } else {
                currentLyricsState = LyricsState.Unavailable
            }
        }
    }

    @Suppress("LiftReturnOrAssignment")
    fun setProgress(pos: Long){
        val lyricIndex = currentLyricsLines.indexOfLast { it.startTimeMs < pos }
        currentSongLineIndex = lyricIndex

        if (currentLyricsState == LyricsState.Unavailable) {
            currentSongLine = "N/A"
        } else {
            if (lyricIndex == -1) {
                currentSongLine = "\uD83C\uDFB5"
            } else {
                currentSongLine = currentLyricsLines[lyricIndex].words
            }
        }
    }

    enum class LyricsState {
        Loading,
        Unavailable,
        Ready
    }
}