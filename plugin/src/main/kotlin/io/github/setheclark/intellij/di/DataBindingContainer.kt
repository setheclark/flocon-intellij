package io.github.setheclark.intellij.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.*
import io.github.setheclark.intellij.db.MapColumnAdapter

@BindingContainer
class DataBindingContainer {

    @Provides
    @SingleIn(AppScope::class)
    fun provideFloconDb(
        mapColumnAdapter: MapColumnAdapter,
    ): FloconDb {
        return FloconDb(
            driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY),
            NetworkCallEntity.Adapter(
                request_headersAdapter = mapColumnAdapter,
                response_headersAdapter = mapColumnAdapter,
            )
        )
    }

    @Provides
    fun provideDeviceEntityQueries(
        db: FloconDb,
    ): DeviceEntityQueries {
        return db.deviceEntityQueries
    }

    @Provides
    fun provideDeviceAppEntityQueries(
        db: FloconDb,
    ): DeviceAppEntityQueries {
        return db.deviceAppEntityQueries
    }

    @Provides
    fun provideNetworkCallEntityQueries(
        db: FloconDb,
    ): NetworkCallEntityQueries {
        return db.networkCallEntityQueries
    }
}