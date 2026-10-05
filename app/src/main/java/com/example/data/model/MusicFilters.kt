package com.example.data.model

enum class ContentType(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums")
}

enum class GenreFilter(val label: String) {
    ALL("All Genres"),
    POP("Pop"),
    BOLLYWOOD("Bollywood"),
    PUNJABI("Punjabi"),
    HIP_HOP("Hip-Hop"),
    ROCK("Rock"),
    R_AND_B("R&B / Soul"),
    COUNTRY("Country"),
    ALTERNATIVE("Alternative"),
    DANCE("Dance / EDM"),
    INDIE("Indie")
}

enum class LanguageFilter(val label: String) {
    ALL("All Languages"),
    ENGLISH("English"),
    HINDI("Hindi"),
    PUNJABI("Punjabi"),
    SPANISH("Spanish"),
    OTHER("Other")
}

enum class YearFilter(val label: String) {
    ALL("All Years"),
    CURRENT_2024_2026("2024 - 2026"),
    YEARS_2020_2023("2020 - 2023"),
    DECADE_2010S("2010s"),
    DECADE_2000S("2000s"),
    CLASSIC("90s & Classic")
}

enum class DurationFilter(val label: String) {
    ANY("Any Length"),
    SHORT("Short (< 3 min)"),
    MEDIUM("Medium (3 - 4 min)"),
    LONG("Long (> 4 min)")
}

enum class ExplicitFilter(val label: String) {
    ALL("All Tracks"),
    CLEAN_ONLY("Clean Only"),
    EXPLICIT_ONLY("Explicit Only")
}

enum class AvailabilityFilter(val label: String) {
    ALL("All Tracks"),
    DOWNLOADED_ONLY("Downloaded Offline")
}

enum class SortOption(val label: String) {
    BEST_MATCH("Best Match"),
    POPULARITY("Most Popular"),
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    TITLE_A_Z("Title (A to Z)"),
    ARTIST_A_Z("Artist (A to Z)")
}

data class ActiveMusicFilters(
    val contentType: ContentType = ContentType.ALL,
    val genre: GenreFilter = GenreFilter.ALL,
    val language: LanguageFilter = LanguageFilter.ALL,
    val year: YearFilter = YearFilter.ALL,
    val duration: DurationFilter = DurationFilter.ANY,
    val explicit: ExplicitFilter = ExplicitFilter.ALL,
    val availability: AvailabilityFilter = AvailabilityFilter.ALL,
    val sortOption: SortOption = SortOption.BEST_MATCH
) {
    val isDefault: Boolean
        get() = contentType == ContentType.ALL &&
                genre == GenreFilter.ALL &&
                language == LanguageFilter.ALL &&
                year == YearFilter.ALL &&
                duration == DurationFilter.ANY &&
                explicit == ExplicitFilter.ALL &&
                availability == AvailabilityFilter.ALL &&
                sortOption == SortOption.BEST_MATCH

    val activeCount: Int
        get() {
            var count = 0
            if (contentType != ContentType.ALL) count++
            if (genre != GenreFilter.ALL) count++
            if (language != LanguageFilter.ALL) count++
            if (year != YearFilter.ALL) count++
            if (duration != DurationFilter.ANY) count++
            if (explicit != ExplicitFilter.ALL) count++
            if (availability != AvailabilityFilter.ALL) count++
            if (sortOption != SortOption.BEST_MATCH) count++
            return count
        }
}
