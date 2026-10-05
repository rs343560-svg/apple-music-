package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.theme.AppleMusicRed

data class RadioStation(
    val id: String,
    val name: String,
    val showName: String,
    val host: String,
    val frequencyDesc: String,
    val colors: List<Color>
)

@Composable
fun RadioScreen(
    songs: List<Song>,
    onTuneIn: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val stations = listOf(
        RadioStation(
            id = "am1",
            name = "Apple Music 1",
            showName = "The Zane Lowe Show",
            host = "Zane Lowe",
            frequencyDesc = "Live • Global Music & Interviews",
            colors = listOf(Color(0xFFE11D48), Color(0xFF4C0519))
        ),
        RadioStation(
            id = "hits",
            name = "Apple Music Hits",
            showName = "Daily Pop Roundup",
            host = "Estelle & Brooke Reese",
            frequencyDesc = "Live • Today's Biggest Chart Hits",
            colors = listOf(Color(0xFF2563EB), Color(0xFF172554))
        ),
        RadioStation(
            id = "country",
            name = "Apple Music Country",
            showName = "Today's Country Hits",
            host = "Kelleigh Bannen",
            frequencyDesc = "Live • Nashville Sound Worldwide",
            colors = listOf(Color(0xFFD97706), Color(0xFF451A03))
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Radio",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Live stations broadcasting worldwide",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        items(stations) { station ->
            val randomSong = songs.randomOrNull() ?: songs.first()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(station.colors))
                    .clickable { onTuneIn(randomSong) }
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ON AIR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E),
                                letterSpacing = 1.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = station.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = station.showName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "With ${station.host} • ${station.frequencyDesc}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onTuneIn(randomSong) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tune In",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}
