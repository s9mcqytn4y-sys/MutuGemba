package id.co.nierstyd.mutugemba.data.local.db

import id.co.nierstyd.mutugemba.data.AppDataPaths

object DatabaseFactory {
    fun create(): SqliteDatabase = SqliteDatabase(AppDataPaths.databaseFile())
}
