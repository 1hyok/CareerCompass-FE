package com.cambridge.core.data.mapper

import com.cambridge.core.model.application.PastApplication
import com.cambridge.core.model.application.PastApplicationCategory
import com.cambridge.core.model.application.PastApplicationItem
import com.cambridge.core.network.dto.PastApplicationDto
import com.cambridge.core.network.dto.PastApplicationItemDto

internal object PastApplicationMapper {
    fun toApplication(dto: PastApplicationDto): PastApplication =
        PastApplication(
            id = dto.id,
            label = dto.label,
            items = dto.items.map(::toItem),
            createdAt = dto.createdAt?.let(WireTime::parseInstant),
        )

    /** 알 수 없는 분류는 명세의 「분류가 불확실한 경우 기타」 규칙대로 [PastApplicationCategory.Other] 로 받는다. */
    fun toItem(dto: PastApplicationItemDto): PastApplicationItem =
        PastApplicationItem(
            id = dto.id,
            category = PastApplicationCategory.fromWireValue(dto.category) ?: PastApplicationCategory.Other,
            content = dto.content,
            confident = dto.confident,
        )
}
