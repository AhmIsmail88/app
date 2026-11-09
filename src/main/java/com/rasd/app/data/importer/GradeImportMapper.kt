package com.rasd.app.data.importer

import com.rasd.app.data.model.StudentEntity

/** نتيجة مطابقة ملف درجات: الدرجات المطابقة وعدد الصفوف غير المطابقة والمرفوضة. */
data class GradeImportResult(
    val scores: List<Pair<Long, Float>>,
    val unmatched: Int,
    val invalid: Int
)

/**
 * مطابقة صفوف ملف Excel/CSV لدرجات طلاب فصل معيّن.
 * دالة نقية (بلا أندرويد) لذا تُختبر مباشرة.
 *
 * المطابقة بالترتيب: رقم الطالب، ثم الاسم بالعربية، ثم الاسم بالإنجليزية.
 * أي فهرس سالب يعني «تجاهل هذا العمود».
 */
object GradeImportMapper {

    fun map(
        rows: List<List<String>>,
        students: List<StudentEntity>,
        numberIdx: Int,
        arabicIdx: Int,
        englishIdx: Int,
        scoreIdx: Int,
        skipHeader: Boolean,
        maximumScore: Float
    ): GradeImportResult {
        val data = if (skipHeader && rows.size > 1) rows.drop(1) else rows

        val byNumber = students.filter { it.studentNumber.isNotBlank() }
            .associateBy { it.studentNumber.trim() }
        val byArabic = students.filter { it.arabicName.isNotBlank() }
            .associateBy { it.arabicName.trim() }
        val byEnglish = students.filter { it.englishName.isNotBlank() }
            .associateBy { it.englishName.trim().lowercase() }

        var unmatched = 0
        var invalid = 0
        val out = LinkedHashMap<Long, Float>()

        data.forEach { row ->
            val raw = row.getOrNull(scoreIdx)?.trim().orEmpty()
            if (raw.isBlank()) return@forEach

            val number = if (numberIdx >= 0) row.getOrNull(numberIdx)?.trim().orEmpty() else ""
            val arabic = if (arabicIdx >= 0) row.getOrNull(arabicIdx)?.trim().orEmpty() else ""
            val english = if (englishIdx >= 0) row.getOrNull(englishIdx)?.trim()?.lowercase().orEmpty() else ""

            val student = when {
                number.isNotBlank() && byNumber.containsKey(number) -> byNumber[number]
                arabic.isNotBlank() && byArabic.containsKey(arabic) -> byArabic[arabic]
                english.isNotBlank() && byEnglish.containsKey(english) -> byEnglish[english]
                else -> null
            }
            if (student == null) {
                unmatched++
                return@forEach
            }

            val value = raw.replace(',', '.').toFloatOrNull()
            if (value == null || value < 0f || (maximumScore > 0f && value > maximumScore)) {
                invalid++
                return@forEach
            }
            out[student.id] = value
        }

        return GradeImportResult(out.map { it.key to it.value }, unmatched, invalid)
    }
}
