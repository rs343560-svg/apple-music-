package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveMusicFilters
import com.example.data.model.AvailabilityFilter
import com.example.data.model.ContentType
import com.example.data.model.DurationFilter
import com.example.data.model.ExplicitFilter
import com.example.data.model.GenreFilter
import com.example.data.model.LanguageFilter
import com.example.data.model.SortOption
import com.example.data.model.YearFilter
import com.example.ui.theme.AppleMusicRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterSheet(
    initialFilters: ActiveMusicFilters,
    sheetState: SheetState,
    onApply: (ActiveMusicFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var filters by remember { mutableStateOf(initialFilters) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF18181C),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = AppleMusicRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Music Filters & Sorting",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (!filters.isDefault) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { filters = ActiveMusicFilters() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = AppleMusicRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleMusicRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort By
            FilterSectionTitle("Sort Results By")
            ScrollableChipRow {
                SortOption.values().forEach { option ->
                    FilterChip(
                        label = option.label,
                        isSelected = filters.sortOption == option,
                        onClick = { filters = filters.copy(sortOption = option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Type
            FilterSectionTitle("Type")
            ScrollableChipRow {
                ContentType.values().forEach { type ->
                    FilterChip(
                        label = type.label,
                        isSelected = filters.contentType == type,
                        onClick = { filters = filters.copy(contentType = type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Genre
            FilterSectionTitle("Genre")
            ScrollableChipRow {
                GenreFilter.values().forEach { genre ->
                    FilterChip(
                        label = genre.label,
                        isSelected = filters.genre == genre,
                        onClick = { filters = filters.copy(genre = genre) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language
            FilterSectionTitle("Language")
            ScrollableChipRow {
                LanguageFilter.values().forEach { lang ->
                    FilterChip(
                        label = lang.label,
                        isSelected = filters.language == lang,
                        onClick = { filters = filters.copy(language = lang) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Release Year
            FilterSectionTitle("Release Era")
            ScrollableChipRow {
                YearFilter.values().forEach { yr ->
                    FilterChip(
                        label = yr.label,
                        isSelected = filters.year == yr,
                        onClick = { filters = filters.copy(year = yr) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Duration
            FilterSectionTitle("Track Duration")
            ScrollableChipRow {
                DurationFilter.values().forEach { dur ->
                    FilterChip(
                        label = dur.label,
                        isSelected = filters.duration == dur,
                        onClick = { filters = filters.copy(duration = dur) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explicitness & Offline Availability
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FilterSectionTitle("Explicit Content")
                    ExplicitFilter.values().forEach { exp ->
                        FilterChip(
                            label = exp.label,
                            isSelected = filters.explicit == exp,
                            onClick = { filters = filters.copy(explicit = exp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    FilterSectionTitle("Offline Availability")
                    AvailabilityFilter.values().forEach { av ->
                        FilterChip(
                            label = av.label,
                            isSelected = filters.availability == av,
                            onClick = { filters = filters.copy(availability = av) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                }

                Button(
                    onClick = {
                        onApply(filters)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleMusicRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    val count = filters.activeCount
                    Text(
                        text = if (count > 0) "Apply Filters ($count)" else "Apply Filters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun ScrollableChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AppleMusicRed else Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = if (isSelected) AppleMusicRed else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = Color.White
        )
    }
}
