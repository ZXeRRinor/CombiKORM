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

package database.interaction

import CombiKORM
import DataRecordTemplate
import DatabaseSaveStatus
import annotations.Id
import database.interaction.base.BaseDatabaseInteractor
import database.util.SqlQueryStringGenerator
import database.util.getTypedObject
import java.net.ConnectException
import java.sql.Connection
import java.sql.Statement
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class SqlDatabaseInteractor<T : DataRecordTemplate<T>>(dataRecordTemplateClass: KClass<out T>) :
    BaseDatabaseInteractor<T>(dataRecordTemplateClass) {
    private val generator = SqlQueryStringGenerator(dataRecordTemplateClass)
    private var activeDatabaseSessions = 1

    private var connection: Connection? = CombiKORM.getDbConnection()
        get() = if (field?.isClosed != false || field?.isValid(3) != true) {
            field?.close()
            CombiKORM.getDbConnection()
        } else field
    private var statement: Statement? = connection?.createStatement()
        get() = field ?: connection?.createStatement()?.also { field = it }

    init {
        statement?.run {
            execute(generator.tableCreation())
            close()
        }
    }

    override operator fun <R> invoke(dbActions: BaseDatabaseInteractor<T>.() -> R): R {
        val result = dbActions()
        statement?.close()
        return result
    }

    fun withBatchSave(dbActions: SqlDatabaseInteractor<T>.() -> Unit): Unit {

    }

    override fun <R> executeCountBy(property: KProperty1<T, R>, value: R): Long = statement?.run {
        val query = generator.countBy(property, value)
        val result = executeQuery(query)
        result.next()
        return@run result.getLong(1)
    } ?: throw ConnectException("Unable to connect to database")

    override fun executeIsUnique(dataRecordTemplate: T): Boolean {
        val primaryKey: KProperty1<T, Long> = propertyList
            .find { Id::class in it.annotations.map { annotation -> annotation.annotationClass } }
            ?.let { it as KProperty1<T, Long> } //check possibility of using other types
            ?: throw IllegalArgumentException("UID not found")
        return executeCountBy(primaryKey, primaryKey.call(dataRecordTemplate)) == 0L
    }

    override fun executeLoadAll(): List<T> = statement?.run {
        val queryResult = executeQuery(generator.loadAll())
        val result = mutableListOf<T>()
        while (queryResult.next()) {
            val currentDataRecordTemplate = dataRecordTemplateClass.java.newInstance()
            propertyList.forEach { property ->
                val classOfPropertyType = property.returnType.classifier as KClass<*>
                val columnValue = queryResult.getTypedObject(property.name, classOfPropertyType)
                (property as KMutableProperty<*>).setter.call(currentDataRecordTemplate, columnValue)
            }
            result.add(currentDataRecordTemplate)
        }
        return@run result
    } ?: throw ConnectException("Connection to database is null")

    override fun executeDeleteAll() {
        statement?.run {
            execute(generator.tableDrop())
        } ?: throw ConnectException("Connection to database is null")
    }

    override fun <R> executeFindBy(property: KProperty1<T, R>, value: R): List<T> = statement?.run {
        if (property !in propertyList)
            throw IllegalArgumentException("Property doesn't belong to the data record template")
        val queryResult = executeQuery(generator.findBy(property, value))
        val result = mutableListOf<T>()
        while (queryResult.next()) {
            val currentDataRecordTemplate = dataRecordTemplateClass.java.newInstance()
            propertyList.forEach { property ->
                val classOfPropertyType = property.returnType.classifier as KClass<*>
                val columnValue = queryResult.getTypedObject(property.name, classOfPropertyType)
                (property as KMutableProperty<*>).setter.call(currentDataRecordTemplate, columnValue)
            }
            result.add(currentDataRecordTemplate)
        }
        return@run result
    } ?: throw ConnectException("Connection to database is null")

    override fun <R> executeDeleteBy(property: KProperty1<T, R>, value: R) {
        statement?.run {
            if (property !in propertyList)
                throw IllegalArgumentException("Property doesn't belong to the data record template")
            execute(generator.deleteBy(property, value))
        } ?: throw ConnectException("Connection to database is null")
    }

    override fun executeSave(dataRecordTemplate: T): DatabaseSaveStatus {
        val primaryKey: KProperty1<T, Long> = propertyList
            .find { Id::class in it.annotations.map { annotation -> annotation.annotationClass } }
            ?.let { it as KProperty1<T, Long> } //check possibility of using other types
            ?: throw IllegalArgumentException("UID not found")
        val isUnique = executeCountBy(primaryKey, primaryKey.call(dataRecordTemplate)) == 0L
        return if (isUnique) write(dataRecordTemplate)
        else update(dataRecordTemplate, primaryKey)
    }

    override fun write(dataRecordTemplate: T): DatabaseSaveStatus = statement?.run {
        if (execute(generator.write(dataRecordTemplate))) DatabaseSaveStatus.SUCCESS
        else DatabaseSaveStatus.FAIL
    } ?: DatabaseSaveStatus.FAIL

    override fun update(dataRecordTemplate: T, primaryKey: KProperty<*>): DatabaseSaveStatus = statement?.run {
        if (execute(generator.update(dataRecordTemplate, primaryKey))) DatabaseSaveStatus.SUCCESS
        else DatabaseSaveStatus.FAIL
    } ?: DatabaseSaveStatus.FAIL
}