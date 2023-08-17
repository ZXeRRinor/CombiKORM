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
import exceptions.IllegalDataRecordTemplateException
import exceptions.IllegalDataRecordTemplatePropertyOwnerException
import util.getDataArrayNameFor
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

abstract class BaseDatabaseInteractor<X : DataRecordTemplate<X>>(protected val dataRecordTemplateClass: KClass<out X>) :
    DatabaseInteractable {
    protected val propertyList = dataRecordTemplateClass.memberProperties.toList()

    fun <T : DataRecordTemplate<T>> isForDataRecordTemplate(dataRecordTemplate: T) =
        dataRecordTemplateClass == dataRecordTemplate::class

    fun <T : DataRecordTemplate<T>> isForDataRecordTemplate(dataRecordTemplate: KClass<T>) =
        dataRecordTemplateClass == dataRecordTemplate

    abstract operator fun <R> invoke(dbActions: BaseDatabaseInteractor<X>.() -> R): R

    protected abstract fun <R> executeCountBy(property: KProperty1<X, R>, value: R): Long
    protected abstract fun executeIsUnique(dataRecordTemplate: X): Boolean
    protected abstract fun executeLoadAll(): List<X>
    protected abstract fun executeDeleteAll()
    protected abstract fun <R> executeFindBy(property: KProperty1<X, R>, value: R): List<X>
    protected abstract fun <R> executeDeleteBy(property: KProperty1<X, R>, value: R)
    protected abstract fun executeSave(dataRecordTemplate: X): DatabaseSaveStatus
    protected abstract fun write(dataRecordTemplate: X): DatabaseSaveStatus
    protected abstract fun update(dataRecordTemplate: X, primaryKey: KProperty<*>): DatabaseSaveStatus


    //<editor-fold desc="Proxy for DatabaseInteractable interface">
    override fun <T : DataRecordTemplate<T>, R> countBy(property: KProperty1<T, R>, value: R) =
        safeRunByPropertyOwner(property) { executeCountBy(it, value) }

    override fun <T : DataRecordTemplate<T>> isUnique(dataRecordTemplate: T) =
        safeRunByDataRecordTemplate(dataRecordTemplate) { executeIsUnique(it) }

    @Suppress("UNCHECKED_CAST") //checked by dataRecordTemplateClass == this.dataRecordTemplateClass (T == X)
    final override fun <T : DataRecordTemplate<T>> loadAll(dataRecordTemplateClass: KClass<T>): List<T> {
        if (dataRecordTemplateClass == this.dataRecordTemplateClass) return executeLoadAll() as List<T>
        else throw IllegalDataRecordTemplateException(this.dataRecordTemplateClass, dataRecordTemplateClass)
    }

    @Suppress("UNCHECKED_CAST") //checked by dataRecordTemplateClass == this.dataRecordTemplateClass (T == X)
    final override fun <T : DataRecordTemplate<T>> deleteAll(dataRecordTemplateClass: KClass<T>) {
        if (dataRecordTemplateClass == this.dataRecordTemplateClass) return executeDeleteAll()
        else throw IllegalDataRecordTemplateException(this.dataRecordTemplateClass, dataRecordTemplateClass)
    }

    @Suppress("UNCHECKED_CAST") //checked by safeRunByPropertyOwner (T == X)
    override fun <T : DataRecordTemplate<T>, R> findBy(property: KProperty1<T, R>, value: R) =
        safeRunByPropertyOwner(property) { executeFindBy(it, value) } as List<T>

    override fun <T : DataRecordTemplate<T>, R> deleteBy(property: KProperty1<T, R>, value: R) =
        safeRunByPropertyOwner(property) { executeDeleteBy(it, value) }

    override fun <T : DataRecordTemplate<T>> save(dataRecordTemplate: T) =
        safeRunByDataRecordTemplate(dataRecordTemplate) { executeSave(it) }
    //</editor-fold>

    @Suppress("UNCHECKED_CAST") //checked by propertyOwnerClass == dataRecordTemplateClass (T == X)
    private fun <T : DataRecordTemplate<T>, R, Y> safeRunByPropertyOwner(
        property: KProperty1<T, R>, action: (KProperty1<X, R>) -> Y
    ): Y {
        val propertyOwnerClass = (property as CallableReference).owner as KClass<*>
        if (propertyOwnerClass == dataRecordTemplateClass) return action(property as KProperty1<X, R>)
        else throw IllegalDataRecordTemplatePropertyOwnerException(dataRecordTemplateClass, propertyOwnerClass)
    }

    @Suppress("UNCHECKED_CAST") //checked by dataRecordTemplate::class == dataRecordTemplateClass (T == X)
    private fun <T : DataRecordTemplate<T>, Y> safeRunByDataRecordTemplate(
        dataRecordTemplate: T, action: (X) -> Y
    ): Y {
        if (isForDataRecordTemplate(dataRecordTemplate)) return action(dataRecordTemplate as X)
        else throw IllegalDataRecordTemplateException(dataRecordTemplateClass, dataRecordTemplate::class)
    }
}