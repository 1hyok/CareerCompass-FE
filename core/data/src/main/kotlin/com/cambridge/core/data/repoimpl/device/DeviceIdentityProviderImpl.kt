package com.cambridge.core.data.repoimpl.device

import com.cambridge.core.datastore.DeviceDataSource
import com.cambridge.core.domain.device.DeviceIdentityProvider
import javax.inject.Inject

internal class DeviceIdentityProviderImpl
    @Inject
    constructor(
        private val deviceDataSource: DeviceDataSource,
    ) : DeviceIdentityProvider {
        override suspend fun deviceId(): String = deviceDataSource.getOrCreateDeviceId()
    }
