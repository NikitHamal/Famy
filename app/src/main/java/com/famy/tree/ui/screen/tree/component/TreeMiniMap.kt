package com.famy.tree.ui.screen.tree.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.TreeBounds
import com.famy.tree.domain.model.TreeLayoutConfig
import com.famy.tree.domain.model.TreeNode

private const val MINI_MAP_WIDTH = 150f
private const val MINI_MAP_HEIGHT = 100f
private const val MINI_MAP_NODE_SIZE = 6f

/**
 * Mini-map component for tree navigation
 * Shows an overview of the entire tree with the current viewport highlighted
 */
@Composable
fun TreeMiniMap(
    nodes: List<TreeNode>,
    bounds: TreeBounds,
    config: TreeLayoutConfig,
    viewportOffsetX: Float,
    viewportOffsetY: Float,
    viewportScale: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    onNavigate: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary

    // Calculate scale to fit tree in mini-map
    val treeWidth = bounds.width.coerceAtLeast(1f)
    val treeHeight = bounds.height.coerceAtLeast(1f)

    val scaleX = MINI_MAP_WIDTH / treeWidth
    val scaleY = MINI_MAP_HEIGHT / treeHeight
    val miniMapScale = minOf(scaleX, scaleY) * 0.9f // 90% to add some padding

    // Calculate offset to center tree in mini-map
    val scaledTreeWidth = treeWidth * miniMapScale
    val scaledTreeHeight = treeHeight * miniMapScale
    val miniMapOffsetX = (MINI_MAP_WIDTH - scaledTreeWidth) / 2f
    val miniMapOffsetY = (MINI_MAP_HEIGHT - scaledTreeHeight) / 2f

    // Pre-compute node positions in mini-map space
    val miniMapNodes = remember(nodes, bounds, miniMapScale) {
        nodes.map { node ->
            MiniMapNode(
                x = (node.x - bounds.minX) * miniMapScale + miniMapOffsetX,
                y = (node.y - bounds.minY) * miniMapScale + miniMapOffsetY,
                gender = node.member.gender,
                hasChildren = node.children.isNotEmpty(),
                childPositions = node.children.map { child ->
                    Pair(
                        (child.x - bounds.minX) * miniMapScale + miniMapOffsetX + MINI_MAP_NODE_SIZE / 2,
                        (child.y - bounds.minY) * miniMapScale + miniMapOffsetY
                    )
                }
            )
        }
    }

    // Calculate viewport rectangle in mini-map space
    val viewportWidth = (canvasWidth / viewportScale) * miniMapScale
    val viewportHeight = (canvasHeight / viewportScale) * miniMapScale
    val viewportCenterXInTree = -viewportOffsetX / viewportScale + bounds.centerX
    val viewportCenterYInTree = -viewportOffsetY / viewportScale + bounds.centerY
    val viewportLeft = (viewportCenterXInTree - bounds.minX) * miniMapScale + miniMapOffsetX - viewportWidth / 2
    val viewportTop = (viewportCenterYInTree - bounds.minY) * miniMapScale + miniMapOffsetY - viewportHeight / 2

    Box(
        modifier = modifier
            .size(MINI_MAP_WIDTH.dp, MINI_MAP_HEIGHT.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size((MINI_MAP_WIDTH - 8).dp, (MINI_MAP_HEIGHT - 8).dp)
                .pointerInput(bounds, miniMapScale) {
                    detectTapGestures { offset ->
                        // Convert tap position to tree coordinates
                        val tapTreeX = (offset.x - miniMapOffsetX) / miniMapScale + bounds.minX
                        val tapTreeY = (offset.y - miniMapOffsetY) / miniMapScale + bounds.minY

                        // Calculate new offset to center on tapped position
                        val newOffsetX = -(tapTreeX - bounds.centerX) * viewportScale
                        val newOffsetY = -(tapTreeY - bounds.centerY) * viewportScale
                        onNavigate(newOffsetX, newOffsetY)
                    }
                }
                .pointerInput(bounds, miniMapScale) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Convert drag to tree offset change
                        val deltaX = -dragAmount.x / miniMapScale * viewportScale
                        val deltaY = -dragAmount.y / miniMapScale * viewportScale
                        onNavigate(viewportOffsetX + deltaX, viewportOffsetY + deltaY)
                    }
                }
        ) {
            // Draw connections
            miniMapNodes.forEach { node ->
                node.childPositions.forEach { (childX, childY) ->
                    val path = Path().apply {
                        moveTo(node.x + MINI_MAP_NODE_SIZE / 2, node.y + MINI_MAP_NODE_SIZE)
                        lineTo(childX, childY)
                    }
                    drawPath(
                        path = path,
                        color = outlineColor.copy(alpha = 0.3f),
                        style = Stroke(width = 0.5f)
                    )
                }
            }

            // Draw nodes
            miniMapNodes.forEach { node ->
                val nodeColor = when (node.gender) {
                    Gender.MALE -> Color(0xFF2196F3).copy(alpha = 0.7f)
                    Gender.FEMALE -> Color(0xFFE91E63).copy(alpha = 0.7f)
                    Gender.OTHER -> Color(0xFF9C27B0).copy(alpha = 0.7f)
                    Gender.UNKNOWN -> Color(0xFF9E9E9E).copy(alpha = 0.7f)
                }

                drawCircle(
                    color = nodeColor,
                    radius = MINI_MAP_NODE_SIZE / 2,
                    center = Offset(node.x + MINI_MAP_NODE_SIZE / 2, node.y + MINI_MAP_NODE_SIZE / 2)
                )
            }

            // Draw viewport rectangle
            drawRect(
                color = primaryColor.copy(alpha = 0.3f),
                topLeft = Offset(viewportLeft, viewportTop),
                size = Size(viewportWidth, viewportHeight)
            )
            drawRect(
                color = primaryColor,
                topLeft = Offset(viewportLeft, viewportTop),
                size = Size(viewportWidth, viewportHeight),
                style = Stroke(width = 2f)
            )
        }
    }
}

private data class MiniMapNode(
    val x: Float,
    val y: Float,
    val gender: Gender,
    val hasChildren: Boolean,
    val childPositions: List<Pair<Float, Float>>
)
