package com.nakcive.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nakcive.app.data.entity.Species

@Composable
fun SpeciesInfoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpeciesInfoViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedSpeciesId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "어종 특징", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "생태·서식지 정보는 참고용이며, 금지체장·금어기는 " +
                "수산자원관리법 시행령 기준(2026년 개정본)입니다.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.species) { species ->
                SpeciesInfoRow(
                    species = species,
                    expanded = expandedSpeciesId == species.id,
                    onClick = {
                        expandedSpeciesId = if (expandedSpeciesId == species.id) null else species.id
                    },
                )
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로가기")
        }
    }
}

@Composable
private fun SpeciesInfoRow(species: Species, expanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = species.commonName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            species.scientificName?.let {
                Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    species.description?.let { Text(text = it) }
                    species.ecology?.let { Text(text = "생태: $it") }
                    species.habitat?.let { Text(text = "서식지: $it") }

                    val sizeText = species.minLegalSize?.let { "${it}cm 이하 방생" }
                    val seasonText = if (species.closedSeasonStart != null && species.closedSeasonEnd != null) {
                        "${species.closedSeasonStart} ~ ${species.closedSeasonEnd}"
                    } else {
                        null
                    }
                    if (sizeText != null || seasonText != null) {
                        Text(
                            text = "법정 기준: " +
                                listOfNotNull(
                                    sizeText?.let { "금지체장 $it" },
                                    seasonText?.let { "금어기 $it" },
                                ).joinToString(" · "),
                            fontWeight = FontWeight.Medium,
                        )
                    } else {
                        Text(
                            text = "법정 금지체장·금어기 없음",
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}
