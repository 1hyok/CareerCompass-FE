package com.careercompass.core.data.mapper

import com.careercompass.core.model.board.Board
import com.careercompass.core.model.board.BoardDetection
import com.careercompass.core.model.board.BoardDetectionStatus
import com.careercompass.core.model.board.BoardPreviewItem
import com.careercompass.core.model.board.BoardRegistration
import com.careercompass.core.model.board.BoardStatus
import com.careercompass.core.model.board.BoardType
import com.careercompass.core.model.board.BoardUpdate
import com.careercompass.core.network.dto.BoardDetectionDto
import com.careercompass.core.network.dto.BoardDto
import com.careercompass.core.network.dto.BoardRegisterRequestDto
import com.careercompass.core.network.dto.BoardUpdateRequestDto
import kotlinx.serialization.json.JsonNull

internal object BoardMapper {
    fun toBoard(dto: BoardDto): Board =
        Board(
            id = dto.id,
            url = dto.url,
            name = dto.name,
            type = BoardType.fromWireValue(dto.type) ?: BoardType.Other,
            cycleHours = dto.cycleHours,
            isActive = dto.isActive,
            status = BoardStatus.fromWireValue(dto.status),
            failCount = dto.failCount,
            lastCollectedAt = dto.lastCollectedAt?.let(WireTime::parseInstant),
        )

    fun toDetection(dto: BoardDetectionDto): BoardDetection {
        val status =
            BoardDetectionStatus.fromWireValue(dto.detectStatus)
                ?: throw IllegalStateException("알 수 없는 감지 상태입니다: ${dto.detectStatus}")
        val preview =
            dto.preview
                .orEmpty()
                .filter { it.title.isNotBlank() && it.url.isNotBlank() }
                .map { BoardPreviewItem(title = it.title, url = it.url, date = it.date?.let(WireTime::parseDate)) }
        val dateSelector = dto.selectors?.get("date")?.takeUnless { it is JsonNull }
        return BoardDetection(
            status = status,
            preview = preview,
            hasDateSelector = dateSelector != null,
        )
    }

    fun toRegisterRequest(registration: BoardRegistration): BoardRegisterRequestDto =
        BoardRegisterRequestDto(
            url = registration.url,
            name = registration.name,
            type = registration.type.wireValue,
            cycleHours = registration.cycleHours,
        )

    fun toUpdateRequest(update: BoardUpdate): BoardUpdateRequestDto =
        BoardUpdateRequestDto(
            name = update.name,
            type = update.type?.wireValue,
            cycleHours = update.cycleHours,
            isActive = update.isActive,
        )
}
