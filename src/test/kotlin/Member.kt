import annotations.CreatedAt
import annotations.Id
import annotations.UpdatedAt
import java.time.LocalDateTime

data class Member(
    @Id
    var memberId: Long = 0,
    var xp: Long = 0,
    var level: Int = 0,
    var messages: Long = 0,
    var countedMessages: Long = 0
) : DataRecordTemplate<Member>() {
    @CreatedAt
    var createdAt: LocalDateTime = LocalDateTime.now()

    @UpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.now()
}