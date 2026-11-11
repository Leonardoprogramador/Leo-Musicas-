package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High fidelity custom Canvas drawing representing headphones and floating notes
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .padding(16.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width * 0.35f

            // 1. Draw Headphone headband arch
            drawArc(
                color = primaryColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 2. Draw Left Ear Cup
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(cx - radius - 12.dp.toPx(), cy - 10.dp.toPx()),
                size = Size(24.dp.toPx(), 46.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // 3. Draw Right Ear Cup
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(cx + radius - 12.dp.toPx(), cy - 10.dp.toPx()),
                size = Size(24.dp.toPx(), 46.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // 4. Draw Floating Wave Lines
            drawLine(
                color = tertiaryColor,
                start = Offset(cx - 50.dp.toPx(), cy + 45.dp.toPx()),
                end = Offset(cx + 50.dp.toPx(), cy + 45.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tertiaryColor,
                start = Offset(cx - 30.dp.toPx(), cy + 55.dp.toPx()),
                end = Offset(cx + 30.dp.toPx(), cy + 55.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirmar",
    dismissText: String = "Cancelar",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (isDestructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun TextInputDialog(
    title: String,
    placeholder: String,
    initialValue: String = "",
    confirmText: String = "Salvar",
    dismissText: String = "Cancelar",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            OutlinedTextField(
                value = textValue.value,
                onValueChange = { textValue.value = it },
                placeholder = { Text(text = placeholder) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.tertiary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(textValue.value)
                    onDismiss()
                },
                enabled = textValue.value.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = confirmText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
