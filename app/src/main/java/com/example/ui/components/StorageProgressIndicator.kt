package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cache.DiskStats
import com.example.cache.MediaCacheManager
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSecondary

@Composable
fun StorageProgressIndicator(
    diskStats: DiskStats,
    cachedItemCount: Int,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    val totalDevice = diskStats.totalDeviceBytes.coerceAtLeast(1L).toFloat()
    val cached = diskStats.cachedBytes.coerceAtLeast(0L).toFloat()
    val available = diskStats.availableBytes.coerceAtLeast(0L).toFloat()
    val otherUsed = (totalDevice - available - cached).coerceAtLeast(0f)

    // Calculate fractions of total disk space
    val cachedFraction = (cached / totalDevice).coerceIn(0f, 1f)
    val otherFraction = (otherUsed / totalDevice).coerceIn(0f, 1f)
    val availableFraction = (available / totalDevice).coerceIn(0f, 1f)

    // Smooth animation for cache updates
    val animatedCacheFraction by animateFloatAsState(
        targetValue = cachedFraction,
        animationSpec = tween(durationMillis = 600),
        label = "cacheFraction"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Multi-segment Visual Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                .testTag("storage_progress_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            ) {
                // Media Cache segment (Emerald Green)
                // Give it a minimum visual representation if cache > 0 so it's readily perceptible
                val visualCacheWeight = if (diskStats.cachedBytes > 0) {
                    animatedCacheFraction.coerceAtLeast(0.015f)
                } else 0f

                if (visualCacheWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(visualCacheWeight)
                            .background(EmeraldSecondary)
                            .testTag("storage_segment_media_cache")
                    )
                }

                // Other Used Space segment (Subtle Slate)
                if (otherFraction > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(otherFraction)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                            .testTag("storage_segment_other_used")
                    )
                }

                // Available Free Space segment (Cyan Primary)
                if (availableFraction > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(availableFraction)
                            .background(CyanPrimary.copy(alpha = 0.85f))
                            .testTag("storage_segment_available")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend & Statistics
        if (showDetails) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Cache indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldSecondary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Media Cache ($cachedItemCount items)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = MediaCacheManager.formatBytes(diskStats.cachedBytes),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSecondary
                        )
                    }
                }

                // Free Disk Space indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Free Disk Space",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = MediaCacheManager.formatBytes(diskStats.availableBytes),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                }

                // Total Device Storage
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Device Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MediaCacheManager.formatBytes(diskStats.totalDeviceBytes),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
