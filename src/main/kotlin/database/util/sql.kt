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

import com.sun.corba.se.spi.legacy.interceptor.UnknownType
import util.doubleQuote
import util.parseToLocalDateTime
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal fun <T : Any> ResultSet.getTypedObject(columnLabel: String, typeClass: KClass<T>): T = when (typeClass) {
    Long::class -> getLong(columnLabel)
    Int::class -> getInt(columnLabel)
    Short::class -> getShort(columnLabel)
    String::class -> getString(columnLabel)
    LocalDateTime::class -> parseToLocalDateTime(getTimestamp(columnLabel).toString())
    Float::class -> getFloat(columnLabel)
    Double::class -> getDouble(columnLabel)
    else -> throw UnknownType("Unsupported type")
} as T

internal fun generatePropertyLine(property: KProperty<*>, startsWith: String): String {
    val lineStringBuilder = StringBuilder().append(startsWith).append(property.name.doubleQuote()).append(' ')
        .append(getPostgreSqlTypeForKotlinType(property.returnType)).append(" NOT NULL")
    return lineStringBuilder.toString()
}

internal fun generatePropertyLineWithDefault(property: KProperty<*>, startsWith: String, default: Any) =
    "${generatePropertyLine(property, startsWith)} DEFAULT $default"

internal fun generatePrimaryKeyPropertyLine(property: KProperty<*>, startsWith: String) =
    "${generatePropertyLine(property, startsWith)} PRIMARY KEY"

internal fun getPostgreSqlTypeForKotlinType(type: KType): String {
    return when (type) {
        typeOf<Long>() -> "int8"
        typeOf<Int>() -> "int4"
        typeOf<Short>() -> "smallint"
        typeOf<String>() -> "text"
        typeOf<OffsetDateTime>() -> "timestamp"
        typeOf<LocalDateTime>() -> "timestamp"
        typeOf<Float>() -> "float4"
        typeOf<Double>() -> "float8"
        else -> throw UnknownType("Unsupported type")
    }
}