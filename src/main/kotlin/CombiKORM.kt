/*
MIT License

Copyright (c) 2023 ZXeRRinor (zxerrinor@gmail.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

import database.connection.DatabaseConnectionProvider
import database.interaction.SqlDatabaseInteractor
import database.interaction.base.BaseDatabaseInteractor
import database.interaction.base.DatabaseInteractable

object CombiKORM {
    var verbose = false
    private val dbAccessorsRegistry = mutableListOf<BaseDatabaseInteractor<*>>()
    private var connectionProvider: DatabaseConnectionProvider? = null

    fun initForDb(connectionProvider: DatabaseConnectionProvider) {
        this.connectionProvider = connectionProvider
    }

    @Deprecated("Only for SQL", ReplaceWith("{}"), DeprecationLevel.WARNING)
    @Suppress("UNCHECKED_CAST") // checked by it.isForDataRecordTemplate(dataRecordTemplate)
    fun <T : DataRecordTemplate<T>> forDRT(dataRecordTemplate: T): BaseDatabaseInteractor<T> {
        return (dbAccessorsRegistry.find { it.isForDataRecordTemplate(dataRecordTemplate) }
                as BaseDatabaseInteractor<T>?)
            ?: SqlDatabaseInteractor(dataRecordTemplate::class).also { dbAccessorsRegistry.add(it) }
    }

    @Deprecated("Only for SQL", ReplaceWith("{}"), DeprecationLevel.WARNING)
    @Suppress("UNCHECKED_CAST") // checked by it.isForDataRecordTemplate(dataRecordTemplate)
    fun <T : DataRecordTemplate<T>, R> forDRT(dataRecordTemplate: T, dbActions: DatabaseInteractable.() -> R): R {
        val interactor = (dbAccessorsRegistry.find { it.isForDataRecordTemplate(dataRecordTemplate) }
                as BaseDatabaseInteractor<T>?)
            ?: SqlDatabaseInteractor(dataRecordTemplate::class).also { dbAccessorsRegistry.add(it) }
        return interactor {
            dbActions()
        }
    }

    internal fun getDbConnection() = connectionProvider?.createConnection()
}