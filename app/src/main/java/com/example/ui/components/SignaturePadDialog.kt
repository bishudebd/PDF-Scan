package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.DeepNavy

private data class DrawnStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

/**
 * Interactive finger/stylus signature pad that renders crisp transparent PNG bitmaps
 * for signing PDF documents natively.
 */
@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSaveSignature: (Bitmap) -> Unit
) {
    val inkColors = listOf(DeepNavy, Color.Black, CobaltBlue, Color(0xFF1E3A8A))
    var selectedColor by remember { mutableStateOf(DeepNavy) }
    var strokeWidth by remember { mutableFloatStateOf(9f) }
    val strokes = remember { mutableStateListOf<DrawnStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 420)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Draw Signature",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close signature pad")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Drawing Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .onSizeChanged { canvasSize = it }
                ) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .testTag("signature_canvas")
                            .pointerInput(selectedColor, strokeWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPoints = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentPoints = currentPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPoints.size >= 2) {
                                            strokes.add(
                                                DrawnStroke(
                                                    points = currentPoints,
                                                    color = selectedColor,
                                                    strokeWidth = strokeWidth
                                                )
                                            )
                                        }
                                        currentPoints = emptyList()
                                    },
                                    onDragCancel = { currentPoints = emptyList() }
                                )
                            }
                    ) {
                        // Subtle baseline guide
                        val baseY = size.height * 0.78f
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(28f, baseY),
                            end = Offset(size.width - 28f, baseY),
                            strokeWidth = 2f
                        )

                        fun drawPointList(pts: List<Offset>, color: Color, width: Float) {
                            if (pts.size < 2) return
                            val path = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].x, pts[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = width,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        strokes.forEach { s -> drawPointList(s.points, s.color, s.strokeWidth) }
                        if (currentPoints.size >= 2) {
                            drawPointList(currentPoints, selectedColor, strokeWidth)
                        }
                    }

                    if (strokes.isEmpty() && currentPoints.isEmpty()) {
                        Text(
                            text = "Sign here with your finger",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ink Color & Undo/Clear row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        inkColors.forEach { color ->
                            val isSelected = selectedColor == color
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) CobaltBlue else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                            enabled = strokes.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = "Undo stroke")
                        }
                        IconButton(
                            onClick = { strokes.clear() },
                            enabled = strokes.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear signature")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pen Weight",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 4f..18f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val bmp = renderSignatureToBitmap(strokes, canvasSize.width.coerceAtLeast(600), canvasSize.height.coerceAtLeast(240))
                        onSaveSignature(bmp)
                    },
                    enabled = strokes.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_signature_button")
                ) {
                    Text("Save & Use Signature")
                }
            }
        }
    }
}

private fun renderSignatureToBitmap(
    strokes: List<DrawnStroke>,
    width: Int,
    height: Int
): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
    }
    strokes.forEach { stroke ->
        if (stroke.points.size >= 2) {
            paint.color = stroke.color.toArgb()
            paint.strokeWidth = stroke.strokeWidth
            val path = AndroidPath().apply {
                moveTo(stroke.points.first().x, stroke.points.first().y)
                for (i in 1 until stroke.points.size) {
                    lineTo(stroke.points[i].x, stroke.points[i].y)
                }
            }
            canvas.drawPath(path, paint)
        }
    }
    return bmp
}
