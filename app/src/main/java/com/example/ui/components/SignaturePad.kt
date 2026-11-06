package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

data class DrawPath(val path: androidx.compose.ui.graphics.Path, val strokeWidth: Float, val color: androidx.compose.ui.graphics.Color)

@Composable
fun SignaturePad(
    onSignatureCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val paths = remember { mutableStateListOf<DrawPath>() }
    var strokeWidth by remember { mutableStateOf(6f) }
    var canvasWidth by remember { mutableStateOf(800f) }
    var canvasHeight by remember { mutableStateOf(400f) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Draw Your Signature",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Color.White)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val p = androidx.compose.ui.graphics.Path().apply { moveTo(offset.x, offset.y) }
                            paths.add(DrawPath(p, strokeWidth, androidx.compose.ui.graphics.Color.Black))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            paths.lastOrNull()?.path?.lineTo(change.position.x, change.position.y)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                canvasWidth = size.width
                canvasHeight = size.height
                for (p in paths) {
                    drawPath(
                        path = p.path,
                        color = p.color,
                        style = Stroke(
                            width = p.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1) },
                    enabled = paths.isNotEmpty()
                ) {
                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo")
                }
                IconButton(
                    onClick = { paths.clear() },
                    enabled = paths.isNotEmpty()
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                }
            }

            Button(
                onClick = {
                    val bmp = Bitmap.createBitmap(canvasWidth.toInt().coerceAtLeast(100), canvasHeight.toInt().coerceAtLeast(100), Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    // Draw transparent background
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    for (dp in paths) {
                        paint.strokeWidth = dp.strokeWidth
                        canvas.drawPath(dp.path.asAndroidPath(), paint)
                    }
                    onSignatureCaptured(bmp)
                },
                enabled = paths.isNotEmpty()
            ) {
                Text("Use Signature")
            }
        }
    }
}
