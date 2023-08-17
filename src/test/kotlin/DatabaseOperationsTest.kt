import database.connection.SqLiteConnectionProvider
import org.junit.jupiter.api.BeforeAll
import kotlin.io.path.Path
import kotlin.test.BeforeTest
import kotlin.test.Test

class DatabaseOperationsTest {
    private val testDatabasePath = Path("src\\test\\resources\\test.db")

    @Test
    fun saveAndLoad() {
        CombiKORM.initForDb(SqLiteConnectionProvider(testDatabasePath.toString()))
        CombiKORM.verbose = true
        val member = Member(0, 10, 10, 1000, 100)
        val memberListFromDatabase = CombiKORM.forDRT(Member()) {
            save(member)
            val result = findBy(Member::memberId, 0)
            println(result)
            result
        }
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