package pl.elcaro.elcarobokapp.service

data class ContactHelpResponse(
    val phone: PhoneDto,
    val email: EmailDto
)

data class PhoneDto(
    val number: String,
    val schedule: ScheduleDto
)

data class EmailDto(
    val address: String,
    val schedule: ScheduleDto
)

data class ScheduleDto(
    val days: String,
    val hours: String
)

data class FaqCategoryResponse(
    val id: String,
    val icon: String,
    val title: String,
    val route: String
)

data class FaqPageResponse(
    val id: String,
    val title: String,
    val content: List<FaqItem>
)

data class FaqItem(
    val id: String,
    val title: String,
    val answer: String,
    val type: String
)
