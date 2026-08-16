package com.nakcive.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nakcive.app.data.entity.Species

private val SizeBadgeColor = Color(0xFFC62828)
private val SeasonBadgeColor = Color(0xFF1565C0)

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
            text = "빨간 배지 = 금지체장(방생기준), 파란 배지 = 금어기 · 수산자원관리법 시행령 " +
                "기준(2026년 개정본). 생태·서식지는 참고용입니다.",
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
private fun RegulationBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color,
        modifier = Modifier.wrapContentWidth(),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
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
            Text(text = species.commonName, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            species.scientificName?.let {
                Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }

            val sizeText = species.minLegalSize?.let { "방생 ${it}cm 이하" }
            val seasonText = if (species.closedSeasonStart != null && species.closedSeasonEnd != null) {
                "금어기 ${species.closedSeasonStart}~${species.closedSeasonEnd}"
            } else {
                null
            }

            if (sizeText != null || seasonText != null) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    sizeText?.let { RegulationBadge(text = it, color = SizeBadgeColor) }
                    seasonText?.let { RegulationBadge(text = it, color = SeasonBadgeColor) }
                }
            } else {
                Text(
                    text = "법정 금지체장·금어기 없음",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    species.description?.let {
                        Text(text = it, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    species.ecology?.let {
                        Text(
                            text = "생태: $it",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    species.habitat?.let {
                        Text(
                            text = "서식지: $it",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}
