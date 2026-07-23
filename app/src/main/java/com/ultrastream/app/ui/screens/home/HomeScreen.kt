package com.ultrastream.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.ui.components.ContinueWatchingCard
import com.ultrastream.app.ui.components.HScrollRow
import com.ultrastream.app.ui.components.PosterCard
import com.ultrastream.app.ui.components.SectionHeader

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onItemClick: (id: String, type: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Continue Watching
        item {
            SectionHeader(title = "Continue Watching")
            HScrollRow {
                // For now dummy, later from history
                Text("No history yet")
            }
        }

        // Catalogs from addons
        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // For each addon/catalog, we'll show a row
            // Since we don't have actual catalog data yet, we show placeholder
            item {
                SectionHeader(title = "Trending")
                HScrollRow {
                    repeat(10) {
                        Text("Placeholder $it")
                    }
                }
            }
        }
    }
}
