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

import annotations.CreatedAt
import annotations.UniqueId
import annotations.UpdatedAt
import java.lang.IllegalArgumentException
import java.net.ConnectException
import java.sql.Connection
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties

class DbAccessor<T : DataRecordTemplate<T>>(val dataRecordTemplateClass: KClass<out T>) {
    private var connection: Connection? = CombiKORM.getDbConnection()
    private var activeDatabaseSessions = 1

    init {
        connection?.createStatement()?.run {
            execute(generateTableCreationQueryFor(dataRecordTemplateClass))
            close()
        }
    }

    private val tableName = getTableNameFor(dataRecordTemplateClass)
    private val propertyList = dataRecordTemplateClass.memberProperties.toList()

    operator fun <R> invoke(dbActions: DbAccessor<T>.() -> R): R {
        if (connection?.isClosed != false || connection?.isValid(3) != true) {
            connection?.close()
            connection = CombiKORM.getDbConnection()
        }
        return dbActions()
    }

    fun withBatchSave(dbActions: DbAccessor<T>.() -> Unit): Unit {

    }

    fun <R> countBy(property: KProperty1<T, R>, value: R): Long = connection?.createStatement()?.run {
        val query = """SELECT COUNT(*) FROM "$tableName" WHERE "${property.name}" = $value"""
        val result = executeQuery(query)
        result.next()
        val res = result.getLong(1)
        close()
        return@run res
    } ?: throw ConnectException("Unable to connect to database")

    fun loadAll(): List<T> = connection?.createStatement()?.run {
        val queryResult = executeQuery("SELECT * FROM \"$tableName\"")
        val result = mutableListOf<T>()
        while (queryResult.next()) {
            val currentDataRecordTemplate = dataRecordTemplateClass.java.newInstance()
            propertyList.forEach { property ->
                val annotationClasses = property.annotations.map { annotation -> annotation.annotationClass }
                val columnValue = when {
                    CreatedAt::class in annotationClasses ->
                        parseToLocalDateTime(queryResult.getTimestamp(property.name).toString())

                    UpdatedAt::class in annotationClasses ->
                        parseToLocalDateTime(queryResult.getTimestamp(property.name).toString())

                    else -> (property.returnType.classifier as KClass<*>).cast(
                        queryResult.getObject(property.name)
                    )
                }
                (property as KMutableProperty<*>).setter.call(currentDataRecordTemplate, columnValue)
            }
            result.add(currentDataRecordTemplate)
        }
        close()
        return@run result
    } ?: throw ConnectException("Connection to database is null")

    fun <R> findBy(property: KProperty1<T, R>, value: R): List<T> = connection?.createStatement()?.run {
        if (property !in propertyList)
            throw IllegalArgumentException("Property doesn't belong to the data record template")
        val queryResult = executeQuery("""SELECT * FROM "$tableName" WHERE "${property.name}" = $value""")
        val result = mutableListOf<T>()
        while (queryResult.next()) {
            val currentDataRecordTemplate = dataRecordTemplateClass.java.newInstance()
            propertyList.forEach { property ->
                val annotationClasses = property.annotations.map { annotation -> annotation.annotationClass }
                val columnValue = when {
                    CreatedAt::class in annotationClasses ->
                        parseToLocalDateTime(queryResult.getTimestamp(property.name).toString())

                    UpdatedAt::class in annotationClasses ->
                        parseToLocalDateTime(queryResult.getTimestamp(property.name).toString())

                    else -> (property.returnType.classifier as KClass<*>).cast(
                        queryResult.getObject(property.name)
                    )
                }
                (property as KMutableProperty<*>).setter.call(currentDataRecordTemplate, columnValue)
            }
            result.add(currentDataRecordTemplate)
        }
        close()
        return@run result
    } ?: throw ConnectException("Connection to database is null")

    fun <R> deleteBy(property: KProperty1<T, R>, value: R): Unit = connection?.createStatement()?.run {
        if (property !in propertyList)
            throw IllegalArgumentException("Property doesn't belong to the data record template")
        execute("""DELETE FROM "$tableName" WHERE "${property.name}" = $value""")
        close()
    } ?: throw ConnectException("Connection to database is null")

    fun save(dataRecordTemplate: T): DatabaseSaveStatus = connection?.createStatement()?.run {
        val primaryKey: KProperty1<T, Long> = propertyList
            .find { UniqueId::class in it.annotations.map { annotation -> annotation.annotationClass } }
            ?.let { it as KProperty1<T, Long> }
            ?: throw IllegalArgumentException("UID not found")
        val isUnique = countBy(primaryKey, primaryKey.call(dataRecordTemplate)) == 0L
        return@run if (isUnique) {
            write(dataRecordTemplate)
        } else {
            update(dataRecordTemplate, primaryKey)
        }
    } ?: throw ConnectException("Connection to database is null")

    private fun write(dataRecordTemplate: T): DatabaseSaveStatus {
        val line1 = StringBuilder("INSERT INTO \"$tableName\" (")
        val line2 = StringBuilder("VALUES (")
        propertyList.forEach {
            val annotationClasses = it.annotations.map { annotation -> annotation.annotationClass }
            line1.append("${it.name.doubleQuote()}, ")
            line2.append(
                if (CreatedAt::class in annotationClasses || UpdatedAt::class in annotationClasses) {
                    val timeUtcNow = getUtcLocalDateTime()
                    (it as KMutableProperty<*>).setter.call(dataRecordTemplate, timeUtcNow)
                    timeUtcNow.toString().replace('T', ' ').singleQuote()
                } else if (it.returnType == typeOf<String>()) it.call(dataRecordTemplate).toString().singleQuote()
                else it.call(dataRecordTemplate)
            )
            line2.append(", ")
        }
        line1.deleteLast(2)
        line1.append(") ")
        line2.deleteLast(2)
        line2.append(");")
        line1.append(line2.toString())
        return connection?.createStatement()?.run {
            val res = if (execute(line1.toString())) DatabaseSaveStatus.SUCCESS else DatabaseSaveStatus.FAIL
            close()
            return@run res
        } ?: DatabaseSaveStatus.FAIL
    }

    private fun update(dataRecordTemplate: T, primaryKey: KProperty<*>): DatabaseSaveStatus {
        val queryBuilder = StringBuilder("UPDATE \"$tableName\" SET ")
        propertyList.forEach {
            val annotationClasses = it.annotations.map { annotation -> annotation.annotationClass }
            val newValue = when {
                CreatedAt::class in annotationClasses -> return@forEach
                UpdatedAt::class in annotationClasses -> {
                    val timeUtcNow = getUtcLocalDateTime()
                    (it as KMutableProperty<*>).setter.call(dataRecordTemplate, timeUtcNow)
                    timeUtcNow.toString().replace('T', ' ').singleQuote()
                }

                it.returnType == typeOf<String>() -> it.call(dataRecordTemplate).toString().singleQuote()
                else -> it.call(dataRecordTemplate)
            }
            queryBuilder.append(it.name.doubleQuote()).append(" = ").append(newValue).append(", ")
        }
        queryBuilder.deleteLast(2)
        queryBuilder.append(""" WHERE "${primaryKey.name}" = ${primaryKey.call(dataRecordTemplate)};""")
        return connection?.createStatement()?.run {
            val res = if (execute(queryBuilder.toString())) DatabaseSaveStatus.SUCCESS else DatabaseSaveStatus.FAIL
            close()
            return@run res
        } ?: DatabaseSaveStatus.FAIL
    }
}