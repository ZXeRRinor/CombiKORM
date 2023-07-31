/*
Copyright 2023 ZXeRRinor (zxerrinor@gmail.com)
Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the “Software”),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
 */

import com.sun.corba.se.spi.legacy.interceptor.UnknownType
import annotations.CreatedAt
import annotations.DataRecordUID
import annotations.ForNamedDataArray
import annotations.UpdatedAt
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

internal fun <T : DataRecordTemplate<T>> generateTableCreationQueryFor(dataRecordTemplate: KClass<out T>): String {
    var primaryKey: KProperty<*>? = null
    var createdAt: KProperty<*>? = null
    var updatedAt: KProperty<*>? = null
    val commonProperties = mutableListOf<KProperty<*>>()
    dataRecordTemplate.memberProperties.forEach {
        val annotationClasses = it.annotations.map { annotation -> annotation.annotationClass }
        when {
            DataRecordUID::class in annotationClasses -> primaryKey = it
            CreatedAt::class in annotationClasses -> createdAt = it
            UpdatedAt::class in annotationClasses -> updatedAt = it
            else -> commonProperties.add(it)
        }
    }
    primaryKey ?: throw IllegalArgumentException("Passed data record template hasn't primary key")
    val queryStringBuilder =
        StringBuilder("CREATE TABLE IF NOT EXISTS ")
            .append('"')
            .append(getTableNameFor(dataRecordTemplate))
            .append('"')
            .append(
                generatePrimaryKeyPropertyLine(primaryKey!!, "(")
            )
    commonProperties.forEach {
        it.call(dataRecordTemplate.java.newInstance())?.also { default ->
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
    return queryStringBuilder.append(");").toString()
}

internal fun generatePropertyLine(property: KProperty<*>, startsWith: String): String {
    val lineStringBuilder = StringBuilder().append(startsWith).append(property.name.doubleQuote()).append(' ')
        .append(getPostgreSqlTypeForKotlinType(property.returnType)).append(" NOT NULL")
    return lineStringBuilder.toString()
}

internal fun generatePropertyLineWithDefault(property: KProperty<*>, startsWith: String, default: Any) =
    "${generatePropertyLine(property, startsWith)} DEFAULT $default"

internal fun generatePrimaryKeyPropertyLine(property: KProperty<*>, startsWith: String) =
    "${generatePropertyLine(property, startsWith)} PRIMARY KEY"

internal fun <T : DataRecordTemplate<T>> getTableNameFor(dataRecordTemplate: KClass<out T>): String {
    val baseDataRecordTemplateName: String = DataRecordTemplate::class.simpleName ?: throw ReflectiveOperationException(
        "Unable to get name of class DataRecordTemplate"
    )
    val dataRecordTemplateName: String =
        dataRecordTemplate.simpleName ?: throw ReflectiveOperationException("Unable to get name of passed class")
    return if (ForNamedDataArray::class in dataRecordTemplate.java.annotations.map { it.annotationClass }) (dataRecordTemplate.java.annotations.find { it is ForNamedDataArray } as ForNamedDataArray).name
    else dataRecordTemplateName.removeSuffix(baseDataRecordTemplateName).lowercase() + 's'
}

internal fun getPostgreSqlTypeForKotlinType(type: KType): String {
    return when (type) {
        typeOf<Long>() -> "int8"
        typeOf<Int>() -> "int4"
        typeOf<String>() -> "varchar(255)"
        typeOf<OffsetDateTime>() -> "timestamp"
        typeOf<LocalDateTime>() -> "timestamp"
        typeOf<Float>() -> "float4"
        typeOf<Double>() -> "float8"
        else -> throw UnknownType("Unsupported type")
    }
}

internal fun parseToOffsetDateTime(str: String): OffsetDateTime {
    return OffsetDateTime.parse(str.replace(' ', 'T'))
}

internal fun parseToLocalDateTime(str: String): LocalDateTime {
    return LocalDateTime.parse(str.replace(' ', 'T'))
}

fun getUtcLocalDateTime(): LocalDateTime {
    return OffsetDateTime.now().let { it.minusSeconds(it.offset.totalSeconds.toLong()) }.toLocalDateTime()
}

fun String.singleQuote() = "'$this'"

fun String.doubleQuote() = "\"$this\""

fun StringBuilder.deleteLast(chars: Int): StringBuilder = delete(length - chars, length)