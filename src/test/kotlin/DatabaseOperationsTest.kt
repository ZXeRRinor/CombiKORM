import database.connection.SqLiteConnectionProvider
import org.junit.jupiter.api.BeforeAll
import java.time.Duration
import java.time.LocalDateTime
import kotlin.io.path.Path
import kotlin.test.Test

class DatabaseOperationsTest {
    private val testDatabasePath = Path("src\\test\\resources\\test.db")

    @Test
    fun saveAndLoad() {
        CombiKORM.initForDb(SqLiteConnectionProvider(testDatabasePath.toString()))
        CombiKORM.verbose = true
        val member = Member(0, 10, 10, 1000, 100)
        CombiKORM.forDRT(Member())
        val start = LocalDateTime.now()
        val memberListFromDatabase = CombiKORM.forDRT(Member()) {
            save(member)
            val result = findBy(Member::memberId, 0)
            deleteAll(Member::class)
            println(result)
            result
        }
        println("Execution time = ${Duration.between(start, LocalDateTime.now()).toMillis()} ms")
        assert(memberListFromDatabase.isNotEmpty() && memberListFromDatabase.first() == member)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun init(): Unit {
            println("Start")
        }
    }
}