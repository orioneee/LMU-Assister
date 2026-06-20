package com.orioooneee.lmuasister.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.ui.components.BlockSkeleton
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.details.CircleButton
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextMed
import org.koin.compose.koinInject

/** Privacy policy — fetched as plain text from GET /api/v2/privacy and rendered in-app. */
@Composable
fun PrivacyPolicyScreen(insets: PaddingValues, onBack: () -> Unit) {
    val api = koinInject<BackendApi>()
    val result by produceState<Result<String>?>(null) {
        value = runCatching { api.privacy() }
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp + insets.calculateTopPadding(), end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(Modifier, onBack)
            Text(
                "Privacy policy",
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
            )
        }

        when (val r = result) {
            null -> {
                val brush = shimmerBrush()
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(8) { BlockSkeleton(brush, 56.dp) }
                }
            }
            else -> r.fold(
                onSuccess = { body ->
                    SelectionContainer(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + insets.calculateBottomPadding()),
                    ) {
                        Text(
                            body.trim(),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = TextMed,
                        )
                    }
                },
                onFailure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Couldn't load the privacy policy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMed,
                        )
                    }
                },
            )
        }
    }
}
