package com.smartorganizer.launcher.ui.components

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * State holder for drag-and-drop reordering in a LazyVerticalGrid.
 * Tracks the dragged item index and current drag offset.
 */
class DragDropState(
    val gridState: LazyGridState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    var draggingItemOffset by mutableFloatStateOf(0f)
        private set

    private var draggingItemInitialOffset: Float = 0f
    private var currentDropTarget: Int? = null

    fun onDragStart(offset: Offset) {
        val item = gridState.layoutInfo.visibleItemsInfo
            .firstOrNull { itemInfo ->
                offset.x.toInt() in itemInfo.offset.x..(itemInfo.offset.x + itemInfo.size.width) &&
                offset.y.toInt() in itemInfo.offset.y..(itemInfo.offset.y + itemInfo.size.height)
            }
        item?.let {
            draggingItemIndex = it.index
            draggingItemInitialOffset = it.offset.y.toFloat()
            draggingItemOffset = 0f
            currentDropTarget = it.index
        }
    }

    fun onDrag(dragDelta: Offset) {
        draggingItemOffset += dragDelta.y
        val currentIndex = draggingItemIndex ?: return

        val currentY = draggingItemInitialOffset + draggingItemOffset
        val targetItem: LazyGridItemInfo? = gridState.layoutInfo.visibleItemsInfo
            .filter { it.index != currentIndex }
            .minByOrNull { info ->
                val centerY = info.offset.y + info.size.height / 2f
                kotlin.math.abs(currentY - centerY)
            }

        targetItem?.let { target ->
            if (target.index != currentDropTarget) {
                onMove(currentIndex, target.index)
                currentDropTarget = target.index
                draggingItemIndex = target.index
                draggingItemInitialOffset = target.offset.y.toFloat()
                draggingItemOffset = currentY - target.offset.y.toFloat()
            }
        }
    }

    fun onDragEnd() {
        draggingItemIndex = null
        draggingItemOffset = 0f
        draggingItemInitialOffset = 0f
        currentDropTarget = null
    }

    fun onDragCancel() = onDragEnd()
}

@Composable
fun rememberDragDropState(
    gridState: LazyGridState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): DragDropState = remember(gridState) {
    DragDropState(gridState, onMove)
}
