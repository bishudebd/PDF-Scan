package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.data.model.CropQuad
import com.example.ui.theme.CobaltBlue
import kotlin.math.hypot
import kotlin.math.min

/**
 * Interactive 4-point manual perspective crop overlay with draggable corner handles,
 * translucent dimmed outer mask, crisp cobalt blue polygon lines, and rule-of-thirds guides.
 */
@Composable
fun InteractiveCropCanvas(
    bitmap: Bitmap,
    quad: CropQuad,
    onQuadChange: (CropQuad) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var activeHandle by remember { mutableIntStateOf(-1) } // 0=TL, 1=TR, 2=BR, 3=BL

    // Compute exact displayed image rect inside box (ContentScale.Fit)
    val imageRect = remember(boxSize, bitmap.width, bitmap.height) {
        if (boxSize.width == 0 || boxSize.height == 0) {
            RectMetrics(0f, 0f, 1f, 1f)
        } else {
            val scale = min(
                boxSize.width.toFloat() / bitmap.width,
                boxSize.height.toFloat() / bitmap.height
            )
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val left = (boxSize.width - drawW) / 2f
            val top = (boxSize.height - drawH) / 2f
            RectMetrics(left, top, drawW, drawH)
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF0A1220))
            .onSizeChanged { boxSize = it },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Document page to crop",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(quad, imageRect) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val pts = listOf(
                                imageRect.toScreen(quad.topLeft),
                                imageRect.toScreen(quad.topRight),
                                imageRect.toScreen(quad.bottomRight),
                                imageRect.toScreen(quad.bottomLeft)
                            )
                            var closestIdx = -1
                            var closestDist = 110f // generous touch radius in px
                            pts.forEachIndexed { index, pt ->
                                val d = hypot(offset.x - pt.x, offset.y - pt.y)
                                if (d < closestDist) {
                                    closestDist = d
                                    closestIdx = index
                                }
                            }
                            activeHandle = closestIdx
                        },
                        onDragEnd = { activeHandle = -1 },
                        onDragCancel = { activeHandle = -1 },
                        onDrag = { change, _ ->
                            if (activeHandle == -1) return@detectDragGestures
                            change.consume()
                            val pos = change.position
                            val nx = ((pos.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f)
                            val ny = ((pos.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f)
                            val updated = when (activeHandle) {
                                0 -> quad.copy(
                                    topLeft = PointF(
                                        nx.coerceAtMost(quad.topRight.x - 0.1f),
                                        ny.coerceAtMost(quad.bottomLeft.y - 0.1f)
                                    )
                                )
                                1 -> quad.copy(
                                    topRight = PointF(
                                        nx.coerceAtLeast(quad.topLeft.x + 0.1f),
                                        ny.coerceAtMost(quad.bottomRight.y - 0.1f)
                                    )
                                )
                                2 -> quad.copy(
                                    bottomRight = PointF(
                                        nx.coerceAtLeast(quad.bottomLeft.x + 0.1f),
                                        ny.coerceAtLeast(quad.topRight.y + 0.1f)
                                    )
                                )
                                3 -> quad.copy(
                                    bottomLeft = PointF(
                                        nx.coerceAtMost(quad.bottomRight.x - 0.1f),
                                        ny.coerceAtLeast(quad.topLeft.y + 0.1f)
                                    )
                                )
                                else -> quad
                            }
                            onQuadChange(updated)
                        }
                    )
                }
        ) {
            val tl = imageRect.toScreen(quad.topLeft)
            val tr = imageRect.toScreen(quad.topRight)
            val br = imageRect.toScreen(quad.bottomRight)
            val bl = imageRect.toScreen(quad.bottomLeft)

            // Draw translucent fill inside selection
            val quadPath = Path().apply {
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }
            drawPath(
                path = quadPath,
                color = CobaltBlue.copy(alpha = 0.12f)
            )

            // Subtle alignment cross-grid inside quad
            val gridColor = Color.White.copy(alpha = 0.45f)
            for (t in listOf(0.333f, 0.666f)) {
                val topPt = lerp(tl, tr, t)
                val botPt = lerp(bl, br, t)
                drawLine(gridColor, topPt, botPt, strokeWidth = 1.5f)
                val leftPt = lerp(tl, bl, t)
                val rightPt = lerp(tr, br, t)
                drawLine(gridColor, leftPt, rightPt, strokeWidth = 1.5f)
            }

            // Crisp polygon border
            drawPath(
                path = quadPath,
                color = CobaltBlue,
                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draggable corner handles
            listOf(tl, tr, br, bl).forEachIndexed { index, pt ->
                val isDragging = activeHandle == index
                val outerRadius = if (isDragging) 34f else 24f
                drawCircle(
                    color = CobaltBlue.copy(alpha = if (isDragging) 0.35f else 0.25f),
                    radius = outerRadius + 10f,
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = outerRadius,
                    center = pt
                )
                drawCircle(
                    color = CobaltBlue,
                    radius = outerRadius - 6f,
                    center = pt,
                    style = Stroke(width = 5f)
                )
            }
        }
    }
}

private data class RectMetrics(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    fun toScreen(pt: PointF): Offset = Offset(left + pt.x * width, top + pt.y * height)
}

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
