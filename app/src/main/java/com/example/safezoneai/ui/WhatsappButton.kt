package com.example.safezoneai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.utils.WhatsAppUtils

/**
 * 💬 BOTÓN DE WHATSAPP - VERSIÓN RESPONSIVE Y SIEMPRE VISIBLE
 *
 * Características:
 * - Siempre visible, no depende de la ubicación
 * - Responsive: se adapta al ancho de pantalla
 * - Muestra feedback cuando no hay ubicación
 * - Funciona en modo compacto y expandido
 */

@Composable
fun WhatsAppShareButton(
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,  // Modo compacto para espacios reducidos
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val whatsAppUtils = WhatsAppUtils(context)
    val hasLocation = latitude != null && longitude != null

    Button(
        onClick = {
            if (hasLocation) {
                whatsAppUtils.shareEmergencyLocation(latitude!!, longitude!!)
            }
        },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(if (isCompact) 48.dp else 56.dp)
            .padding(horizontal = if (isCompact) 12.dp else 20.dp, vertical = 4.dp)
            .shadow(if (hasLocation) 6.dp else 2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (hasLocation) Color(0xFF25D366) else Color(0xFF25D366).copy(alpha = 0.5f),
            disabledContainerColor = Color(0xFF25D366).copy(alpha = 0.3f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                tint = PureWhite,
                modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
            )

            Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))

            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (isCompact) "COMPARTIR" else "COMPARTIR POR WHATSAPP",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompact) 14.sp else 16.sp,
                    color = PureWhite
                )

                // Mostrar estado solo si no hay ubicación
                AnimatedVisibility(
                    visible = !hasLocation,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Esperando ubicación GPS...",
                        fontSize = 10.sp,
                        color = PureWhite.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * 💬 BOTÓN FLOTANTE DE WHATSAPP
 * Versión FAB para usar en pantallas con poco espacio
 */
@Composable
fun WhatsAppFloatingButton(
    latitude: Double?,
    longitude: Double?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val whatsAppUtils = WhatsAppUtils(context)
    val hasLocation = latitude != null && longitude != null

    FloatingActionButton(
        onClick = {
            if (hasLocation) {
                whatsAppUtils.shareEmergencyLocation(latitude!!, longitude!!)
            }
            onClick()
        },
        modifier = modifier
            .size(64.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        containerColor = if (hasLocation) Color(0xFF25D366) else Color(0xFF25D366).copy(alpha = 0.5f),
        contentColor = PureWhite
    ) {
        Icon(
            Icons.Default.Share,
            contentDescription = "Compartir por WhatsApp",
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * 💬 MINI BOTÓN DE WHATSAPP
 * Para usar en listas o cards
 */
@Composable
fun WhatsAppMiniButton(
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val whatsAppUtils = WhatsAppUtils(context)

    IconButton(
        onClick = {
            if (latitude != null && longitude != null) {
                whatsAppUtils.shareEmergencyLocation(latitude, longitude)
            }
        },
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            Icons.Default.Share,
            contentDescription = "Compartir",
            tint = Color(0xFF25D366)
        )
    }
}