package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Song
import com.example.ui.theme.AppleMusicRed

data class FeaturedAlbum(
    val title: String,
    val subtitle: String,
    val badge: String,
    val artworkUrl: String,
    val bgGradient: List<Color>
)

@Composable
fun NewScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val featuredAlbums = listOf(
        FeaturedAlbum(
            title = "HIT ME HARD AND SOFT",
            subtitle = "Billie Eilish",
            badge = "FEATURED ALBUM",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/92/9f/69/929f69f1-9977-3a44-d674-11f70c852d1b/24UMGIM36186.rgb.jpg/600x600bb.jpg",
            bgGradient = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
        ),
        FeaturedAlbum(
            title = "Short n' Sweet",
            subtitle = "Sabrina Carpenter",
            badge = "GLOBAL PHENOMENON",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/cb/64/8c/cb648cf7-e7bb-bd00-efe0-d744312c29a8/24UMGIM32304.rgb.jpg/600x600bb.jpg",
            bgGradient = listOf(Color(0xFFB45309), Color(0xFF451A03))
        ),
        FeaturedAlbum(
            title = "Where I've Been, Isn't Where I'm Going",
            subtitle = "Shaboozey",
            badge = "TOP COUNTRY HIT",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/54/50/75/54507577-9e1d-c2c0-79dd-81e65aa7fb9d/197342550925_cover.jpg/600x600bb.jpg",
            bgGradient = listOf(Color(0xFF9A3412), Color(0xFF292524))
        ),
        FeaturedAlbum(
            title = "Die With A Smile",
            subtitle = "Lady Gaga & Bruno Mars",
            badge = "CHART TOPPER",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/11/ae/f2/11aef294-f57c-bab9-c9fc-529162984e62/24UMGIM85348.rgb.jpg/600x600bb.jpg",
            bgGradient = listOf(Color(0xFF6B21A8), Color(0xFF1E1B4B))
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
                    text = "Browse",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // Featured Carousel
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(featuredAlbums) { album ->
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = album.badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleMusicRed,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = album.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = album.subtitle,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(album.bgGradient))
                        ) {
                            AsyncImage(
                                model = album.artworkUrl,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Releases",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "See All",
                    fontSize = 14.sp,
                    color = AppleMusicRed,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Horizontal items for new releases
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(songs.take(8)) { song ->
                    Column(
                        modifier = Modifier
                            .width(135.dp)
                            .clickable { onSongClick(song) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(135.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = song.artwork,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = song.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}
