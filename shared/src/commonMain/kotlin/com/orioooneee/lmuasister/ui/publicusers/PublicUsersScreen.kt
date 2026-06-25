package com.orioooneee.lmuasister.ui.publicusers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.orioooneee.lmuasister.data.remote.PublicUserDto
import com.orioooneee.lmuasister.data.remote.RatingDistributionBucketDto
import com.orioooneee.lmuasister.data.remote.RatingDto
import com.orioooneee.lmuasister.data.remote.UsersDistributionDto
import com.orioooneee.lmuasister.ui.IconFlag
import com.orioooneee.lmuasister.ui.IconSearch
import com.orioooneee.lmuasister.ui.components.EmptyState
import com.orioooneee.lmuasister.ui.components.RankBadge
import com.orioooneee.lmuasister.ui.components.SectionHeader
import com.orioooneee.lmuasister.ui.components.rankTierColor
import com.orioooneee.lmuasister.ui.profile.ProfileSkeleton
import com.orioooneee.lmuasister.ui.profile.ProfileView
import com.orioooneee.lmuasister.ui.profile.TrackBreakdownView
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.Surface3
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import org.koin.compose.viewmodel.koinViewModel

private val RankOrder = listOf("Platinum", "Gold", "Silver", "Bronze")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicUsersScreen(
    viewModel: PublicUsersViewModel = koinViewModel(),
    insets: PaddingValues = PaddingValues(),
    onOpenUser: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val topInset = insets.calculateTopPadding()
    val bottomInset = insets.calculateBottomPadding()
    var showSearch by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Carbon)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(topInset + 18.dp))
        DriversTopBar(onSearch = { showSearch = true })
        Spacer(Modifier.height(16.dp))

        when (val s = state) {
            PublicUsersUiState.Loading -> ProfileSkeleton()
            is PublicUsersUiState.Error -> EmptyState(
                title = "Couldn’t load drivers",
                subtitle = s.message,
                accent = MaterialTheme.colorScheme.error,
                actionLabel = "Retry",
                onAction = viewModel::refresh,
                modifier = Modifier.fillMaxWidth().height(360.dp),
            )
            is PublicUsersUiState.Success -> {
                Text(
                    "${s.summary.count} public drivers",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextLow,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                DistributionSection(s.summary.distribution)
                Spacer(Modifier.height(18.dp))
                SectionHeader("Top safety")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    s.summary.topSafety.forEachIndexed { index, user ->
                        PublicUserCard(index + 1, user, onClick = { onOpenUser(user.uid) })
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp + bottomInset))
    }

    if (showSearch) {
        SearchSheet(
            state = search,
            bottomInset = bottomInset,
            onQuery = viewModel::setSearchQuery,
            onLoadMore = viewModel::loadMoreSearch,
            onOpenUser = {
                showSearch = false
                onOpenUser(it)
            },
            onDismiss = {
                showSearch = false
                viewModel.clearSearch()
            },
        )
    }
}

@Composable
private fun DriversTopBar(onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Drivers", style = MaterialTheme.typography.headlineSmall, color = TextHigh, fontWeight = FontWeight.Black)
            Text("Safety and racecraft index", style = MaterialTheme.typography.labelMedium, color = TextLow)
        }
        IconButton(
            onClick = onSearch,
            modifier = Modifier.clip(CircleShape).background(Surface1).border(1.dp, Outline, CircleShape),
        ) {
            Icon(IconSearch, contentDescription = "Search", tint = TextHigh)
        }
    }
}

@Composable
private fun DistributionSection(distribution: UsersDistributionDto) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DistributionCard("Driver Rating", distribution.driverRating)
        DistributionCard("Safety Rating", distribution.safetyRating)
    }
}

@Composable
private fun DistributionCard(title: String, data: Map<String, RatingDistributionBucketDto>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = TextLow, fontWeight = FontWeight.Black)
        RankOrder.forEach { rank ->
            val bucket = data[rank] ?: RatingDistributionBucketDto()
            DistributionRow(rank, bucket)
        }
    }
}

@Composable
private fun DistributionRow(rank: String, bucket: RatingDistributionBucketDto) {
    val color = rankTierColor(rank)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(rank, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(82.dp))
        Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(7.dp)).background(Surface3)) {
            Box(
                Modifier
                    .fillMaxWidth((bucket.percentage / 100.0).toFloat().coerceIn(0f, 1f))
                    .height(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(color),
            )
            Text(
                "${bucket.count} (${oneDecimal(bucket.percentage)}%)",
                style = MaterialTheme.typography.labelMedium,
                color = TextHigh,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublicUserCard(index: Int?, user: PublicUserDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = rankTierColor(user.safetyRating?.rank.orEmpty())
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (index != null) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("#$index", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Black)
            }
        }
        DriverFlag(user.nationality, accent, Modifier.size(42.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                user.name ?: user.uid,
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RatingBadge("DR", user.driverRating)
                RatingBadge("SR", user.safetyRating)
                user.badge?.takeIf { it.isNotBlank() }?.let { BadgePill(it) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatBadge("Races", user.races, TextMed)
                StatBadge("Wins", user.wins, Color(0xFFE6C04A))
                StatBadge("Podiums", user.podiums, Amber)
                StatBadge("Poles", user.polePositions, Color(0xFF6FE3F0))
                StatBadge("Fastest laps", user.fastestLaps, Color(0xFFC792EA))
            }
        }
    }
}

@Composable
private fun RatingBadge(label: String, rating: RatingDto?) {
    if (rating == null) return
    val value = rating.label?.takeIf { it.isNotBlank() }
        ?: "${rating.rank.firstOrNull()?.uppercaseChar() ?: '?'}${rating.tier}"
    RankBadge(label, value, rankTierColor(rating.rank))
}

@Composable
private fun BadgePill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Amber.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(text.replace('-', ' '), style = MaterialTheme.typography.labelSmall, color = Amber, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun StatBadge(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.34f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            value.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = TextHigh,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun DriverFlag(nationality: String?, accent: Color, modifier: Modifier = Modifier) {
    val cc = nationality?.takeIf { it.length == 2 && it.all(Char::isLetter) }?.lowercase()
    val shape = CircleShape
    Box(modifier.clip(shape).background(Surface2).border(1.dp, accent.copy(alpha = 0.65f), shape), contentAlignment = Alignment.Center) {
        if (cc == null) {
            Icon(IconFlag, contentDescription = null, tint = TextLow, modifier = Modifier.size(20.dp))
        } else {
            AsyncImage(
                model = "https://flagcdn.com/w160/$cc.png",
                contentDescription = nationality,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSheet(
    state: PublicUserSearchState,
    bottomInset: androidx.compose.ui.unit.Dp,
    onQuery: (String) -> Unit,
    onLoadMore: () -> Unit,
    onOpenUser: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.users.size, state.hasMore, state.loadingMore) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last to listState.layoutInfo.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 4) onLoadMore()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface1,
        contentColor = TextHigh,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Search drivers", style = MaterialTheme.typography.titleLarge, color = TextHigh, fontWeight = FontWeight.Black)
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(IconSearch, contentDescription = null, tint = TextMed) },
                placeholder = { Text("Driver name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextHigh,
                    unfocusedTextColor = TextHigh,
                    focusedBorderColor = Amber,
                    unfocusedBorderColor = Outline,
                    cursorColor = Amber,
                    focusedContainerColor = Surface2,
                    unfocusedContainerColor = Surface2,
                ),
            )

            when {
                state.query.isBlank() -> SearchEmpty("Type a name to search")
                state.loading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Amber)
                }
                state.error != null -> SearchEmpty(state.error)
                state.users.isEmpty() -> SearchEmpty("No drivers found")
                else -> {
                    Text(
                        "${state.total} results",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextLow,
                        fontWeight = FontWeight.SemiBold,
                    )
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.users, key = { it.uid }) { user ->
                            PublicUserCard(index = null, user = user, onClick = { onOpenUser(user.uid) })
                        }
                        if (state.loadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Amber, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp + bottomInset))
        }
    }
}

@Composable
private fun SearchEmpty(text: String) {
    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextLow, textAlign = TextAlign.Center)
    }
}

@Composable
fun PublicUserDetailScreen(
    uid: String,
    viewModel: PublicUsersViewModel = koinViewModel(),
    insets: PaddingValues = PaddingValues(),
    onBack: () -> Unit,
    onOpenTracks: () -> Unit,
) {
    val state by viewModel.detail.collectAsStateWithLifecycle()
    val topInset = insets.calculateTopPadding()
    val bottomInset = insets.calculateBottomPadding()

    LaunchedEffect(uid) { viewModel.loadDetail(uid) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Carbon)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(topInset + 10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHigh)
            }
            Text("Public profile", style = MaterialTheme.typography.titleLarge, color = TextHigh, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(14.dp))
        when (val s = state) {
            PublicUserDetailUiState.Loading -> ProfileSkeleton()
            is PublicUserDetailUiState.Error -> EmptyState(
                title = "User not found",
                subtitle = s.message,
                accent = MaterialTheme.colorScheme.error,
                actionLabel = null,
                onAction = null,
                modifier = Modifier.fillMaxWidth().height(360.dp),
            )
            is PublicUserDetailUiState.Success -> {
                ProfileView(
                    profile = s.profile,
                    accountName = s.profile.name ?: s.profile.uid,
                    readOnly = true,
                    enableTrackBreakdown = s.profile.trackBreakdown.isNotEmpty(),
                    onOpenTracks = onOpenTracks,
                )
            }
        }
        Spacer(Modifier.height(32.dp + bottomInset))
    }
}

@Composable
fun PublicUserTrackBreakdownScreen(
    uid: String,
    viewModel: PublicUsersViewModel = koinViewModel(),
    insets: PaddingValues = PaddingValues(),
    onBack: () -> Unit,
    onOpenTrack: (String) -> Unit,
) {
    val state by viewModel.detail.collectAsStateWithLifecycle()
    LaunchedEffect(uid) { viewModel.loadDetail(uid) }

    when (val s = state) {
        PublicUserDetailUiState.Loading -> TrackBreakdownView(
            profile = null,
            insets = insets,
            onBack = onBack,
            onOpenTrack = onOpenTrack,
        )
        is PublicUserDetailUiState.Error -> Column(Modifier.fillMaxSize().background(Carbon)) {
            Spacer(Modifier.height(insets.calculateTopPadding() + 48.dp))
            EmptyState(
                title = "Couldn’t load tracks",
                subtitle = s.message,
                accent = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxSize(),
            )
        }
        is PublicUserDetailUiState.Success -> TrackBreakdownView(
            profile = s.profile,
            insets = insets,
            onBack = onBack,
            onOpenTrack = onOpenTrack,
        )
    }
}

private fun oneDecimal(value: Double): String {
    val scaled = (value * 10).toInt()
    return "${scaled / 10}.${scaled % 10}"
}
