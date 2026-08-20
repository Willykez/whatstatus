package com.willykez.wastatus.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willykez.wastatus.ui.theme.LocalExtendedColors

/**
 * Shown once, before any folder-access prompt. Explains — in plain language,
 * before the system picker ever appears — exactly what WaStatus asks for and
 * why, so the SAF grant doesn't feel like an unexplained, sketchy request
 * the first time a user sees it.
 */
@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    val extended = LocalExtendedColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Gradient hero banner — the same visual language used across every other screen's hero card.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, extended.vaultAccent))
                )
                .padding(horizontal = 24.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Welcome to WaStatus",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Before we start, here's exactly what we ask for and why — no surprises.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            OnboardingPoint(
                icon = Icons.Default.Folder,
                title = "One folder, chosen by you",
                body = "You'll pick WhatsApp's own \"WhatsApp\" folder in the system file picker. WaStatus never gets broad storage access — only that folder, and only until you revoke it.",
                tint = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.primaryContainer
            )
            OnboardingPoint(
                icon = Icons.Default.PhotoLibrary,
                title = "Saves go to your gallery",
                body = "Tapping Save copies a status into a normal \"WaStatus\" album in your Photos/Gallery — a real file you own, not something locked inside this app.",
                tint = extended.success,
                container = extended.successContainer
            )
            OnboardingPoint(
                icon = Icons.Default.CleaningServices,
                title = "Cleaner only deletes what you approve",
                body = "Freeing up WhatsApp's cache always shows a confirmation first, with the exact file count and size, before anything is removed.",
                tint = extended.warning,
                container = extended.warningContainer
            )
            OnboardingPoint(
                icon = Icons.Default.Lock,
                title = "Nothing leaves your device",
                body = "No status content, contacts, or messages are ever uploaded anywhere. Everything above happens entirely on-device.",
                tint = extended.vaultAccent,
                container = extended.vaultAccentContainer
            )
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Button(
                onClick = onGetStarted,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_onboarding_get_started")
            ) {
                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Independent, unofficial app — not affiliated with WhatsApp LLC or Meta Platforms, Inc.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OnboardingPoint(icon: ImageVector, title: String, body: String, tint: Color, container: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
