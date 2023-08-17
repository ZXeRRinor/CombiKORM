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

package database.util

import CombiKORM
import DataRecordTemplate
import annotations.CreatedAt
import annotations.Id
import annotations.UpdatedAt
import util.*
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties

class SqlQueryStringGenerator<T : DataRecordTemplate<T>>(private val dataRecordTemplateClass: KClass<out T>) {
    private val dataArrayName = getDataArrayNameFor(dataRecordTemplateClass)
    private val propertyList = dataRecordTemplateClass.memberProperties.toList()

    fun <R> countBy(property: KProperty1<T, R>, value: R): String {
        val result = "SELECT COUNT(*) FROM \"$dataArrayName\" WHERE \"${property.name}\" = $value"
        if (CombiKORM.verbose) println(result)
        return result
    }

    fun loadAll(): String {
        val result = "SELECT * FROM \"$dataArrayName\""
        if (CombiKORM.verbose) println(result)
        return result
    }

    fun <R> findBy(property: KProperty1<T, R>, value: R): String {
        val result = "SELECT * FROM \"$dataArrayName\" WHERE \"${property.name}\" = $value"
        if (CombiKORM.verbose) println(result)
        return result
    }

    fun <R> deleteBy(property: KProperty1<T, R>, value: R): String {
        val result = "DELETE FROM \"$dataArrayName\" WHERE \"${property.name}\" = $value"
        if (CombiKORM.verbose) println(result)
        return result
    }

    fun write(dataRecordTemplate: T): String {
        val line1 = StringBuilder("INSERT INTO \"$dataArrayName\" (")
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
        val result = line1.toString()
        if (CombiKORM.verbose) println(result)
        return result
    }

    fun update(dataRecordTemplate: T, primaryKey: KProperty<*>): String {
        val queryBuilder = StringBuilder("UPDATE \"$dataArrayName\" SET ")
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
        val result = queryBuilder.toString()
        if (CombiKORM.verbose) println(result)
        return result
    }

    fun tableCreation(): String {
        var primaryKey: KProperty<*>? = null
        var createdAt: KProperty<*>? = null
        var updatedAt: KProperty<*>? = null
        val commonProperties = mutableListOf<KProperty<*>>()
        dataRecordTemplateClass.memberProperties.forEach {
            val annotationClasses = it.annotations.map { annotation -> annotation.annotationClass }
            when {
                Id::class in annotationClasses -> primaryKey = it
                CreatedAt::class in annotationClasses -> createdAt = it
                UpdatedAt::class in annotationClasses -> updatedAt = it
                else -> commonProperties.add(it)
            }
        }
        primaryKey ?: throw IllegalArgumentException("Passed data record template hasn't primary key")
        val queryStringBuilder =
            StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append('"')
                .append(getDataArrayNameFor(dataRecordTemplateClass))
                .append('"')
                .append(
                    generatePrimaryKeyPropertyLine(primaryKey!!, "(")
                )
        commonProperties.forEach {
            it.call(dataRecordTemplateClass.java.newInstance())?.also { default ->
                queryStringBuilder.append(
                    generatePropertyLineWithDefault(it, ", ", default)
                )
            }
        }
        createdAt?.let {
            queryStringBuilder.append(generatePropertyLine(it, ", "))
        }
        updatedAt?.let {
            queryStringBuilder.append(generatePropertyLine(it, ", "))
        }
        val result = queryStringBuilder.append(");").toString()
        if (CombiKORM.verbose) println(result)
        return result
    }

    fun tableDrop(): String {
        val result = "DROP TABLE $dataArrayName;"
        if (CombiKORM.verbose) println(result)
        return result
    }
}