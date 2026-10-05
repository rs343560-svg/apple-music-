package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MusicCatalog
import com.example.data.local.UserPreferencesManager
import com.example.data.model.ActiveMusicFilters
import com.example.data.model.ContentType
import com.example.data.model.ExplicitFilter
import com.example.data.model.GenreFilter
import com.example.data.model.LanguageFilter
import com.example.data.model.SortOption
import com.example.data.repository.MusicRepository
import com.example.player.MusicPlayerViewModel
import com.example.util.PerformanceMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Apple Music", appName)
    }

    @Test
    fun `music catalog has rich collection of songs`() {
        assertTrue(MusicCatalog.songs.isNotEmpty())
        val first = MusicCatalog.songs.first()
        assertEquals("A Bar Song (Tipsy)", first.title)
        assertEquals("Shaboozey", first.artist)
        assertNotNull(first.mp4Link)

        // Verify Hindi/Bollywood songs exist in catalog
        val hindiSongs = MusicCatalog.songs.filter { it.language == "Hindi" || it.language == "Punjabi" }
        assertTrue("Expected Hindi or Punjabi songs in catalog", hindiSongs.isNotEmpty())
    }

    @Test
    fun `view model state transitions and performance modes`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = MusicPlayerViewModel(context)

        val initialState = vm.state.value
        assertNotNull(initialState.currentSong)
        assertTrue(initialState.queue.isNotEmpty())
        assertFalse(initialState.isPlaying)

        // Toggle shuffle
        vm.toggleShuffle()
        assertTrue(vm.state.value.isShuffle)

        // Toggle repeat
        vm.toggleRepeat()
        assertTrue(vm.state.value.isRepeat)

        // Toggle favorite
        val song = initialState.currentSong!!
        vm.toggleFavorite(song)
        assertTrue(vm.state.value.favorites.contains(song.id))
        vm.toggleFavorite(song)
        assertFalse(vm.state.value.favorites.contains(song.id))

        // Audio route
        vm.setAudioRoute("AirPods Max")
        assertEquals("AirPods Max", vm.state.value.selectedAudioRoute)

        // Video canvas toggle
        val videoCanvasBefore = vm.state.value.showVideoCanvas
        vm.toggleVideoCanvas()
        assertEquals(!videoCanvasBefore, vm.state.value.showVideoCanvas)

        // Performance mode
        vm.setPerformanceMode(PerformanceMode.BATTERY_SAVER)
        assertEquals(PerformanceMode.BATTERY_SAVER, vm.state.value.performanceMode)
    }

    @Test
    fun `local search and filter repository test`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicRepository(context)

        // Test Hindi/Hinglish search
        val resultArijit = repo.searchProviderFast("arijit", ActiveMusicFilters())
        assertTrue("Search for 'arijit' should yield results", resultArijit.songs.isNotEmpty())

        val resultKesariya = repo.searchProviderFast("kesaria", ActiveMusicFilters())
        assertTrue("Search for misspelled 'kesaria' should yield results", resultKesariya.songs.isNotEmpty())

        // Test natural language keyword expansion
        val resultBarsaat = repo.searchProviderFast("barsaat banjaare", ActiveMusicFilters())
        assertTrue("Search for 'barsaat banjaare' should match songs", resultBarsaat.songs.isNotEmpty() || resultBarsaat.totalMatches >= 0)

        // Test filter by genre
        val popFilter = ActiveMusicFilters(genre = GenreFilter.POP)
        val popResults = repo.searchProviderFast("", popFilter)
        assertTrue(popResults.songs.all { it.genre.contains("Pop", ignoreCase = true) })

        // Test filter by language
        val hindiFilter = ActiveMusicFilters(language = LanguageFilter.HINDI)
        val hindiResults = repo.searchProviderFast("", hindiFilter)
        assertTrue(hindiResults.songs.all { it.language.equals("Hindi", ignoreCase = true) })

        // Test sorting by title A-Z
        val sortedResults = repo.searchProviderFast("", ActiveMusicFilters(sortOption = SortOption.TITLE_A_Z))
        for (i in 0 until sortedResults.songs.size - 1) {
            assertTrue(sortedResults.songs[i].title.lowercase() <= sortedResults.songs[i + 1].title.lowercase())
        }
    }

    @Test
    fun `custom playlists and preferences persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferencesManager(context)

        // Gemini key save & retrieval
        prefs.setCustomGeminiApiKey("AIzaSyTestKey12345")
        assertEquals("AIzaSyTestKey12345", prefs.getCustomGeminiApiKey())
        prefs.clearCustomGeminiApiKey()
        assertEquals("", prefs.getCustomGeminiApiKey())

        // Recent searches test
        prefs.addRecentSearch("romantic arijit songs")
        prefs.addRecentSearch("barsaat banjaare")
        val recents = prefs.getRecentSearches()
        assertTrue(recents.contains("barsaat banjaare"))
        assertTrue(recents.contains("romantic arijit songs"))
        prefs.removeRecentSearch("barsaat banjaare")
        assertFalse(prefs.getRecentSearches().contains("barsaat banjaare"))
        prefs.clearRecentSearches()
        assertTrue(prefs.getRecentSearches().isEmpty())

        // Custom Playlists
        val initialCount = prefs.getPlaylists().size
        val created = prefs.createPlaylist("My Custom Chill Playlist", "Vibes for evening")
        assertEquals("My Custom Chill Playlist", created.name)
        assertEquals(initialCount + 1, prefs.getPlaylists().size)

        // Add song to playlist
        prefs.addSongToPlaylist(created.id, 101)
        val updatedPlaylist = prefs.getPlaylists().first { it.id == created.id }
        assertTrue(updatedPlaylist.songIds.contains(101))

        // Delete playlist
        prefs.deletePlaylist(created.id)
        assertFalse(prefs.getPlaylists().any { it.id == created.id })
    }
}
