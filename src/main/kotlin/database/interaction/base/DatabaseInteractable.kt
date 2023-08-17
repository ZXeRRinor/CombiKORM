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

package database.interaction.base

import DataRecordTemplate
import DatabaseSaveStatus
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface DatabaseInteractable {
    fun <T : DataRecordTemplate<T>, R> countBy(property: KProperty1<T, R>, value: R): Long
    fun <T : DataRecordTemplate<T>> isUnique(dataRecordTemplate: T): Boolean
    fun <T : DataRecordTemplate<T>> loadAll(dataRecordTemplateClass: KClass<T>): List<T>
    fun <T : DataRecordTemplate<T>> deleteAll(dataRecordTemplateClass: KClass<T>): Unit
    fun <T : DataRecordTemplate<T>, R> findBy(property: KProperty1<T, R>, value: R): List<T>
    fun <T : DataRecordTemplate<T>, R> deleteBy(property: KProperty1<T, R>, value: R): Unit
    fun <T : DataRecordTemplate<T>> save(dataRecordTemplate: T): DatabaseSaveStatus
}