package br.com.helpdev.lapscounter

import android.content.Context
import io.realm.DynamicRealm
import io.realm.RealmConfiguration
import io.realm.RealmMigration

internal object AppApplicationRealmConfiguration {
    private const val SCHEMA_VERSION = 1L

    fun create(context: Context, name: String = "app.realm"): RealmConfiguration =
        RealmConfiguration.Builder()
            .directory(context.filesDir)
            .name(name)
            .schemaVersion(SCHEMA_VERSION)
            .migration(PreserveLegacySchemaMigration)
            .build()

    private object PreserveLegacySchemaMigration : RealmMigration {
        override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
            // Version 1 records the existing schema explicitly; no fields changed from version 0.
        }
    }
}
