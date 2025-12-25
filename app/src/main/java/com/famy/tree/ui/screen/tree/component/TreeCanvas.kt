package com.famy.tree.ui.screen.tree.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.Relationship
import com.famy.tree.domain.model.TreeBounds
import com.famy.tree.domain.model.TreeLayoutConfig
import com.famy.tree.domain.model.TreeNode
import com.famy.tree.ui.theme.FemaleCardColor
import com.famy.tree.ui.theme.FemaleCardColorDark
import com.famy.tree.ui.theme.MaleCardColor
import com.famy.tree.ui.theme.MaleCardColorDark
import com.famy.tree.ui.theme.OtherCardColor
import com.famy.tree.ui.theme.OtherCardColorDark
import com.famy.tree.ui.theme.PaternalLineColor
import com.famy.tree.ui.theme.SpouseLineColor
import com.famy.tree.ui.theme.UnknownCardColor
import com.famy.tree.ui.theme.UnknownCardColorDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

private const val PHOTO_CACHE_MAX_SIZE = 50
private const val PHOTO_CACHE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
private const val AVATAR_SIZE = 64
private const val MIN_SCALE_FOR_DETAILS = 0.4f
private const val MIN_SCALE_FOR_PHOTOS = 0.3f

/**
 * Optimized TreeCanvas with:
 * - Viewport culling for nodes and connections
 * - LRU bitmap caching with memory limits
 * - Spatial indexing via QuadTree for efficient hit testing
 * - Paint object pooling
 * - Level-of-detail rendering based on zoom level
 * - Render batching for similar operations
 */
@Composable
fun TreeCanvas(
    nodes: List<TreeNode>,
    relationships: List<Relationship>,
    bounds: TreeBounds,
    config: TreeLayoutConfig,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    selectedMemberId: Long?,
    onMemberClick: (Long) -> Unit,
    onMemberLongClick: (Long) -> Unit,
    onScaleChange: (Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Local state for smooth interaction
    var localScale by remember { mutableFloatStateOf(scale) }
    var localOffsetX by remember { mutableFloatStateOf(offsetX) }
    var localOffsetY by remember { mutableFloatStateOf(offsetY) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Sync external state changes
    LaunchedEffect(scale) { localScale = scale }
    LaunchedEffect(offsetX) { localOffsetX = offsetX }
    LaunchedEffect(offsetY) { localOffsetY = offsetY }

    // Optimized photo cache using LRU with memory tracking
    val photoCache = remember { createBitmapCache() }

    // Build spatial index for efficient hit testing
    val spatialIndex = remember(nodes, config) {
        buildSpatialIndex(nodes, config.nodeWidth, config.nodeHeight)
    }

    // Precompute node lookup map
    val nodeMap = remember(nodes) {
        nodes.associateBy { it.member.id }
    }

    // Load photos with debouncing to prevent excessive IO
    LaunchedEffect(nodes) {
        snapshotFlow { nodes.mapNotNull { it.member.photoPath } }
            .debounce(100)
            .collectLatest { photoPaths ->
                photoPaths.forEach { path ->
                    if (photoCache.get(path) == null) {
                        withContext(Dispatchers.IO) {
                            loadAndScaleBitmap(path, AVATAR_SIZE)?.let { bitmap ->
                                photoCache.put(path, bitmap)
                            }
                        }
                    }
                }
            }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            photoCache.evictAll()
        }
    }

    // Cached paint objects - stable references
    val paintCache = remember(textColor, outlineColor, isDarkTheme) {
        PaintCache(
            textColor = textColor,
            outlineColor = outlineColor,
            isDarkTheme = isDarkTheme,
            density = density.density
        )
    }

    val nodeWidth = config.nodeWidth
    val nodeHeight = config.nodeHeight
    val cornerRadius = with(density) { 12.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (localScale * zoom).coerceIn(0.2f, 3f)
                    localScale = newScale
                    localOffsetX += pan.x
                    localOffsetY += pan.y
                    onScaleChange(zoom)
                    onPan(pan.x, pan.y)
                }
            }
            .pointerInput(spatialIndex, nodeWidth, nodeHeight) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val transformedTap = transformTapToCanvas(
                            tapOffset, canvasSize, localOffsetX, localOffsetY, localScale
                        )
                        spatialIndex.query(transformedTap)?.let { node ->
                            onMemberClick(node.member.id)
                        }
                    },
                    onLongPress = { tapOffset ->
                        val transformedTap = transformTapToCanvas(
                            tapOffset, canvasSize, localOffsetX, localOffsetY, localScale
                        )
                        spatialIndex.query(transformedTap)?.let { node ->
                            onMemberLongClick(node.member.id)
                        }
                    }
                )
            }
    ) {
        canvasSize = size

        // Calculate viewport with padding for smooth scrolling
        val viewportPadding = maxOf(nodeWidth, nodeHeight) * 2
        val viewportRect = calculateViewport(
            canvasSize = size,
            offsetX = localOffsetX,
            offsetY = localOffsetY,
            scale = localScale,
            padding = viewportPadding
        )

        val centerOffsetX = -bounds.centerX
        val centerOffsetY = -bounds.centerY

        clipRect {
            drawContext.canvas.nativeCanvas.save()

            // Apply transformations
            drawContext.canvas.nativeCanvas.translate(
                size.width / 2 + localOffsetX,
                size.height / 2 + localOffsetY
            )
            drawContext.canvas.nativeCanvas.scale(localScale, localScale)

            // Filter visible nodes using spatial index
            val visibleNodes = spatialIndex.queryRect(
                viewportRect.translate(-centerOffsetX, -centerOffsetY)
            )

            // Determine level of detail based on scale
            val renderDetails = localScale >= MIN_SCALE_FOR_DETAILS
            val renderPhotos = localScale >= MIN_SCALE_FOR_PHOTOS

            // Draw connections first (only for visible or connected nodes)
            drawOptimizedConnections(
                nodes = nodes,
                visibleNodes = visibleNodes,
                nodeMap = nodeMap,
                config = config,
                lineColor = outlineColor,
                spouseLineColor = SpouseLineColor,
                parentChildColor = PaternalLineColor,
                centerOffsetX = centerOffsetX,
                centerOffsetY = centerOffsetY,
                viewportRect = viewportRect,
                scale = localScale
            )

            // Batch render nodes by gender for consistent colors
            val nodesByGender = visibleNodes.groupBy { it.member.gender }

            nodesByGender.forEach { (gender, genderNodes) ->
                val bgColor = getGenderColor(gender, isDarkTheme)

                genderNodes.forEach { node ->
                    drawMemberNodeOptimized(
                        node = node,
                        nodeWidth = nodeWidth,
                        nodeHeight = nodeHeight,
                        cornerRadius = cornerRadius,
                        isSelected = node.member.id == selectedMemberId,
                        backgroundColor = bgColor,
                        paintCache = paintCache,
                        outlineColor = outlineColor,
                        selectedColor = primaryColor,
                        centerOffsetX = centerOffsetX,
                        centerOffsetY = centerOffsetY,
                        photoBitmap = if (renderPhotos) {
                            node.member.photoPath?.let { photoCache.get(it) }
                        } else null,
                        renderDetails = renderDetails
                    )
                }
            }

            drawContext.canvas.nativeCanvas.restore()
        }
    }
}

/**
 * Stable paint cache to avoid recreation during recomposition
 */
@Stable
private class PaintCache(
    textColor: Color,
    outlineColor: Color,
    isDarkTheme: Boolean,
    density: Float
) {
    val textPaint = Paint().apply {
        color = textColor.toArgb()
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val subtitlePaint = Paint().apply {
        color = textColor.copy(alpha = 0.7f).toArgb()
        textSize = 24f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val initialPaint = Paint().apply {
        color = outlineColor.copy(alpha = 0.7f).toArgb()
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val photoPaint = Paint().apply {
        isAntiAlias = true
    }
}

/**
 * Optimized spatial index using a simple grid-based approach
 * More efficient than QuadTree for family trees with relatively uniform distribution
 */
@Stable
private class SpatialIndex(
    private val nodes: List<TreeNode>,
    private val nodeWidth: Float,
    private val nodeHeight: Float,
    private val cellSize: Float
) {
    private val grid: Map<Pair<Int, Int>, MutableList<TreeNode>>
    private val nodeRects: Map<Long, Rect>

    init {
        val gridMap = mutableMapOf<Pair<Int, Int>, MutableList<TreeNode>>()
        val rects = mutableMapOf<Long, Rect>()

        nodes.forEach { node ->
            val rect = Rect(
                offset = Offset(node.x, node.y),
                size = Size(nodeWidth, nodeHeight)
            )
            rects[node.member.id] = rect

            // Add to all cells that the node overlaps
            val minCellX = (node.x / cellSize).toInt()
            val maxCellX = ((node.x + nodeWidth) / cellSize).toInt()
            val minCellY = (node.y / cellSize).toInt()
            val maxCellY = ((node.y + nodeHeight) / cellSize).toInt()

            for (cx in minCellX..maxCellX) {
                for (cy in minCellY..maxCellY) {
                    gridMap.getOrPut(cx to cy) { mutableListOf() }.add(node)
                }
            }
        }

        grid = gridMap
        nodeRects = rects
    }

    fun query(point: Offset): TreeNode? {
        val cellX = (point.x / cellSize).toInt()
        val cellY = (point.y / cellSize).toInt()

        return grid[cellX to cellY]?.find { node ->
            nodeRects[node.member.id]?.contains(point) == true
        }
    }

    fun queryRect(rect: Rect): List<TreeNode> {
        val result = mutableSetOf<TreeNode>()
        val minCellX = (rect.left / cellSize).toInt()
        val maxCellX = (rect.right / cellSize).toInt()
        val minCellY = (rect.top / cellSize).toInt()
        val maxCellY = (rect.bottom / cellSize).toInt()

        for (cx in minCellX..maxCellX) {
            for (cy in minCellY..maxCellY) {
                grid[cx to cy]?.forEach { node ->
                    nodeRects[node.member.id]?.let { nodeRect ->
                        if (rect.overlaps(nodeRect)) {
                            result.add(node)
                        }
                    }
                }
            }
        }

        return result.toList()
    }
}

private fun buildSpatialIndex(
    nodes: List<TreeNode>,
    nodeWidth: Float,
    nodeHeight: Float
): SpatialIndex {
    // Cell size should be roughly 2-4x the node size for optimal performance
    val cellSize = maxOf(nodeWidth, nodeHeight) * 3
    return SpatialIndex(nodes, nodeWidth, nodeHeight, cellSize)
}

private fun createBitmapCache(): LruCache<String, Bitmap> {
    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSize = minOf(maxMemory / 8, PHOTO_CACHE_SIZE_BYTES / 1024)

    return object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }
}

private fun transformTapToCanvas(
    tapOffset: Offset,
    canvasSize: Size,
    offsetX: Float,
    offsetY: Float,
    scale: Float
): Offset {
    val x = (tapOffset.x - canvasSize.width / 2 - offsetX) / scale
    val y = (tapOffset.y - canvasSize.height / 2 - offsetY) / scale
    return Offset(x, y)
}

private fun calculateViewport(
    canvasSize: Size,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    padding: Float
): Rect {
    val halfWidth = (canvasSize.width / 2 / scale) + padding
    val halfHeight = (canvasSize.height / 2 / scale) + padding
    val centerX = -offsetX / scale
    val centerY = -offsetY / scale

    return Rect(
        left = centerX - halfWidth,
        top = centerY - halfHeight,
        right = centerX + halfWidth,
        bottom = centerY + halfHeight
    )
}

private fun Rect.translate(dx: Float, dy: Float): Rect {
    return Rect(
        left = left + dx,
        top = top + dy,
        right = right + dx,
        bottom = bottom + dy
    )
}

/**
 * Optimized connection drawing that only renders visible connections
 */
private fun DrawScope.drawOptimizedConnections(
    nodes: List<TreeNode>,
    visibleNodes: List<TreeNode>,
    nodeMap: Map<Long, TreeNode>,
    config: TreeLayoutConfig,
    lineColor: Color,
    spouseLineColor: Color,
    parentChildColor: Color,
    centerOffsetX: Float,
    centerOffsetY: Float,
    viewportRect: Rect,
    scale: Float
) {
    val strokeWidth = (2f / scale).coerceIn(1f, 4f) // Adjust stroke for zoom
    val nodeWidth = config.nodeWidth
    val nodeHeight = config.nodeHeight

    val visibleNodeIds = visibleNodes.map { it.member.id }.toSet()

    // Draw parent-child connections
    nodes.forEach { parentNode ->
        val parentCenterX = parentNode.x + centerOffsetX + nodeWidth / 2
        val parentBottomY = parentNode.y + centerOffsetY + nodeHeight

        parentNode.children.forEach { childNode ->
            // Only draw if either parent or child is visible
            if (parentNode.member.id in visibleNodeIds || childNode.member.id in visibleNodeIds) {
                val childCenterX = childNode.x + centerOffsetX + nodeWidth / 2
                val childTopY = childNode.y + centerOffsetY

                // Check if connection is within viewport
                if (isConnectionVisible(
                        parentCenterX, parentBottomY,
                        childCenterX, childTopY,
                        viewportRect, centerOffsetX, centerOffsetY
                    )
                ) {
                    val path = Path().apply {
                        moveTo(parentCenterX, parentBottomY)
                        val midY = (parentBottomY + childTopY) / 2
                        cubicTo(
                            parentCenterX, midY,
                            childCenterX, midY,
                            childCenterX, childTopY
                        )
                    }

                    drawPath(
                        path = path,
                        color = parentChildColor.copy(alpha = 0.6f),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }

        // Draw spouse connections
        parentNode.spouses.forEach { spouse ->
            val spouseNode = nodeMap[spouse.id]
            if (spouseNode != null) {
                if (parentNode.member.id in visibleNodeIds || spouse.id in visibleNodeIds) {
                    val spouseCenterX = spouseNode.x + centerOffsetX + nodeWidth / 2
                    val spouseCenterY = spouseNode.y + centerOffsetY + nodeHeight / 2
                    val nodeCenterX = parentNode.x + centerOffsetX + nodeWidth / 2
                    val nodeCenterY = parentNode.y + centerOffsetY + nodeHeight / 2

                    drawLine(
                        color = spouseLineColor.copy(alpha = 0.6f),
                        start = Offset(nodeCenterX + nodeWidth / 2, nodeCenterY),
                        end = Offset(spouseCenterX - nodeWidth / 2, spouseCenterY),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }
}

private fun isConnectionVisible(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    viewportRect: Rect,
    centerOffsetX: Float,
    centerOffsetY: Float
): Boolean {
    val minX = min(x1, x2) - centerOffsetX
    val maxX = max(x1, x2) - centerOffsetX
    val minY = min(y1, y2) - centerOffsetY
    val maxY = max(y1, y2) - centerOffsetY

    return !(maxX < viewportRect.left ||
            minX > viewportRect.right ||
            maxY < viewportRect.top ||
            minY > viewportRect.bottom)
}

/**
 * Optimized node rendering with level-of-detail support
 */
private fun DrawScope.drawMemberNodeOptimized(
    node: TreeNode,
    nodeWidth: Float,
    nodeHeight: Float,
    cornerRadius: Float,
    isSelected: Boolean,
    backgroundColor: Color,
    paintCache: PaintCache,
    outlineColor: Color,
    selectedColor: Color,
    centerOffsetX: Float,
    centerOffsetY: Float,
    photoBitmap: Bitmap?,
    renderDetails: Boolean
) {
    val member = node.member
    val x = node.x + centerOffsetX
    val y = node.y + centerOffsetY

    // Draw card background
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(
                    offset = Offset(x, y),
                    size = Size(nodeWidth, nodeHeight)
                ),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        )
    }

    drawPath(path = path, color = backgroundColor)

    // Draw selection/outline
    val strokeColor = if (isSelected) selectedColor else outlineColor
    val strokeWidth = if (isSelected) 4f else 1f

    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = strokeWidth)
    )

    if (!renderDetails) {
        // Simplified rendering at low zoom levels
        drawCircle(
            color = outlineColor.copy(alpha = 0.3f),
            radius = 20f,
            center = Offset(x + nodeWidth / 2, y + nodeHeight / 2)
        )
        return
    }

    // Full detail rendering
    val avatarRadius = 24f
    val avatarCenterX = x + nodeWidth / 2
    val avatarCenterY = y + 35f

    if (photoBitmap != null && !photoBitmap.isRecycled) {
        // Draw photo with shader
        val photoPaint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(
                photoBitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            ).apply {
                val bitmapScale = (avatarRadius * 2) / photoBitmap.width.toFloat()
                val matrix = Matrix()
                matrix.setScale(bitmapScale, bitmapScale)
                matrix.postTranslate(avatarCenterX - avatarRadius, avatarCenterY - avatarRadius)
                setLocalMatrix(matrix)
            }
        }

        drawContext.canvas.nativeCanvas.drawCircle(
            avatarCenterX,
            avatarCenterY,
            avatarRadius,
            photoPaint
        )

        drawCircle(
            color = outlineColor.copy(alpha = 0.5f),
            radius = avatarRadius,
            center = Offset(avatarCenterX, avatarCenterY),
            style = Stroke(width = 2f)
        )
    } else {
        // Draw placeholder with initial
        drawCircle(
            color = outlineColor.copy(alpha = 0.3f),
            radius = avatarRadius,
            center = Offset(avatarCenterX, avatarCenterY)
        )

        drawContext.canvas.nativeCanvas.drawText(
            member.firstName.take(1).uppercase(),
            avatarCenterX,
            avatarCenterY + 10f,
            paintCache.initialPaint
        )
    }

    // Gender indicator
    val genderIndicatorColor = when (member.gender) {
        Gender.MALE -> Color(0xFF2196F3)
        Gender.FEMALE -> Color(0xFFE91E63)
        Gender.OTHER -> Color(0xFF9C27B0)
        Gender.UNKNOWN -> Color(0xFF9E9E9E)
    }

    drawCircle(
        color = genderIndicatorColor,
        radius = 5f,
        center = Offset(x + nodeWidth - 12f, y + 12f)
    )

    // Deceased indicator
    if (!member.isLiving) {
        drawLine(
            color = outlineColor.copy(alpha = 0.5f),
            start = Offset(x + 8f, y + 8f),
            end = Offset(x + 20f, y + 20f),
            strokeWidth = 2f
        )
        drawLine(
            color = outlineColor.copy(alpha = 0.5f),
            start = Offset(x + 20f, y + 8f),
            end = Offset(x + 8f, y + 20f),
            strokeWidth = 2f
        )
    }

    // Name
    drawContext.canvas.nativeCanvas.drawText(
        member.firstName.take(12),
        x + nodeWidth / 2,
        y + 80f,
        paintCache.textPaint
    )

    member.lastName?.let { lastName ->
        drawContext.canvas.nativeCanvas.drawText(
            lastName.take(12),
            x + nodeWidth / 2,
            y + 105f,
            paintCache.subtitlePaint
        )
    }

    // Age
    member.age?.let { age ->
        val ageText = if (member.isLiving) "$age" else "($age)"
        drawContext.canvas.nativeCanvas.drawText(
            ageText,
            x + nodeWidth / 2,
            y + nodeHeight - 10f,
            paintCache.subtitlePaint
        )
    }
}

private fun getGenderColor(gender: Gender, isDarkTheme: Boolean): Color {
    return when (gender) {
        Gender.MALE -> if (isDarkTheme) MaleCardColorDark else MaleCardColor
        Gender.FEMALE -> if (isDarkTheme) FemaleCardColorDark else FemaleCardColor
        Gender.OTHER -> if (isDarkTheme) OtherCardColorDark else OtherCardColor
        Gender.UNKNOWN -> if (isDarkTheme) UnknownCardColorDark else UnknownCardColor
    }
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

private fun loadAndScaleBitmap(path: String, targetSize: Int): Bitmap? {
    return try {
        val file = File(path)
        if (!file.exists()) return null

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        val sampleSize = calculateInSampleSize(options.outWidth, options.outHeight, targetSize)

        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Memory efficient

        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        scaledBitmap
    } catch (e: Exception) {
        null
    }
}

private fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
    var inSampleSize = 1
    if (width > targetSize || height > targetSize) {
        val halfWidth = width / 2
        val halfHeight = height / 2
        while ((halfWidth / inSampleSize) >= targetSize && (halfHeight / inSampleSize) >= targetSize) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
