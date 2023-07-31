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

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object CombiKORM {
    private val dbAccessorsRegistry = mutableListOf<DbAccessor<*>>()
    private var url: String? = null
    private var username: String? = null
    private var password: String? = null

    fun initForDb(url: String, username: String, password: String) {
        this.url = url
        this.username = username
        this.password = password
    }

    @Suppress("UNCHECKED_CAST") // checked by it.dataRecordTemplateClass == dataRecordTemplate::class
    fun <T : DataRecordTemplate<T>> forDRT(dataRecordTemplate: T): DbAccessor<T> {
        return (dbAccessorsRegistry.find { it.dataRecordTemplateClass == dataRecordTemplate::class }
                as DbAccessor<T>?)
            ?: DbAccessor(dataRecordTemplate::class).also { dbAccessorsRegistry.add(it) }
    }

    @Suppress("UNCHECKED_CAST") // checked by it.dataRecordTemplateClass == dataRecordTemplate::class
    fun <T : DataRecordTemplate<T>, R> forDRT(dataRecordTemplate: T, dbActions: DbAccessor<T>.() -> R): R {
        val accessor = (dbAccessorsRegistry.find { it.dataRecordTemplateClass == dataRecordTemplate::class }
                as DbAccessor<T>?)
            ?: DbAccessor(dataRecordTemplate::class).also { dbAccessorsRegistry.add(it) }
        return accessor {
            dbActions()
        }
    }

    internal fun getDbConnection(): Connection? {
        try {
            Class.forName("org.postgresql.Driver")
            try {
                return DriverManager.getConnection(
                    url ?: return null,
                    username ?: return null,
                    password ?: return null
                )
            } catch (e: SQLException) {
                println(e.message)
            }
        } catch (e: ClassNotFoundException) {
            println(e.message)
        }
        return null
    }
}