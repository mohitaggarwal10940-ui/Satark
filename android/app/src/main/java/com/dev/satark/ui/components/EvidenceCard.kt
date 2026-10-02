package com.dev.satark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dev.satark.R
import com.dev.satark.data.model.Evidence
import com.dev.satark.ui.theme.RiskLow
import com.dev.satark.ui.theme.RiskMedium
import com.dev.satark.ui.theme.RiskVeryHigh
import com.dev.satark.ui.theme.SatarkTheme
import com.dev.satark.ui.theme.ShieldBlue

@Composable
fun EvidenceCard(
    evidence: Evidence,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    val statusColor = when (evidence.status.uppercase()) {
        "VERIFIED" -> RiskLow

        "PARTIALLY_VERIFIED",
        "UNVERIFIED",
        "NOT_VERIFIED",
        "INSUFFICIENT_EVIDENCE",
        "SOURCE_UNAVAILABLE" -> RiskMedium

        "REFUTED",
        "MISLEADING",
        "CONTRADICTED" -> RiskVeryHigh

        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = evidence.claim,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = evidence.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = evidence.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!evidence.sourceUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                uriHandler.openUri(evidence.sourceUrl)
                            } catch (_: Exception) {
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = stringResource(R.string.action_view_official_source),
                        tint = ShieldBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_view_official_source),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ShieldBlue
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EvidenceCardPreview() {
    SatarkTheme {
        EvidenceCard(
            evidence = Evidence(
                claim = "Registration claim",
                status = "UNVERIFIED",
                details = "The available information was insufficient to verify the registration claim.",
                sourceUrl = "https://www.sebi.gov.in"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
