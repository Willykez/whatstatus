package com.willykez.wastatus.ui.chat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willykez.wastatus.model.DirectChatMessage
import com.willykez.wastatus.ui.theme.LocalExtendedColors

private val AvatarPalette = listOf(
    Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFF9C6CDA))),
    Brush.linearGradient(listOf(Color(0xFF146C2E), Color(0xFF4CAF6E))),
    Brush.linearGradient(listOf(Color(0xFF8A5300), Color(0xFFE0A34D))),
    Brush.linearGradient(listOf(Color(0xFF8E4A9E), Color(0xFFD679E8)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    history: List<DirectChatMessage>,
    onSendMessage: (String, String) -> Unit
) {
    val context = LocalContext.current
    val extended = LocalExtendedColors.current
    var phoneNumber by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, extended.vaultAccent))
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Direct WhatsApp Chat",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Message anyone on WhatsApp instantly without saving their contact number.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        item {
            Text(
                text = "RECIPIENT PHONE NUMBER",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                placeholder = { Text("+1 (555) 000-0000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_phone")
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MESSAGE (OPTIONAL)",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Hi! Saw your status...") },
                minLines = 3,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_message")
            )
            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
                    if (cleanPhone.isBlank()) {
                        Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSendMessage(cleanPhone, messageText)

                    try {
                        val encodedMsg = Uri.encode(messageText)
                        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "WhatsApp not installed or could not open link", Toast.LENGTH_SHORT).show()
                    }

                    messageText = ""
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_send_direct")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Open in WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RECENT DIRECT CHATS",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (history.isEmpty()) {
                Text(
                    text = "No direct chats yet — messages you send from here will show up in this list.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }

        items(history, key = { it.id }) { msg ->
            val avatarBrush = AvatarPalette[kotlin.math.abs(msg.phoneNumber.hashCode()) % AvatarPalette.size]
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(avatarBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = msg.phoneNumber.filter { it.isDigit() }.takeLast(2).ifBlank { "#" },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = msg.phoneNumber,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = msg.messageText.ifBlank { "(No initial text)" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = msg.timestamp,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
