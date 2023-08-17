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

package util

import DataRecordTemplate
import annotations.Name
import kotlin.reflect.KClass

fun String.singleQuote() = "'$this'"

fun String.doubleQuote() = "\"$this\""

fun StringBuilder.deleteLast(chars: Int): StringBuilder = delete(length - chars, length)

internal fun <T : DataRecordTemplate<T>> getDataArrayNameFor(dataRecordTemplate: KClass<out T>): String {
    val baseDataRecordTemplateName: String = DataRecordTemplate::class.simpleName ?: throw ReflectiveOperationException(
        "Unable to get name of class DataRecordTemplate"
    )
    val dataRecordTemplateName: String =
        dataRecordTemplate.simpleName ?: throw ReflectiveOperationException("Unable to get name of passed class")
    return if (Name::class in dataRecordTemplate.java.annotations.map { it.annotationClass })
        (dataRecordTemplate.java.annotations.find { it is Name } as Name).name
    else dataRecordTemplateName.removeSuffix(baseDataRecordTemplateName).lowercase() + 's'
}