package com.rasd.app.data.importer

import com.rasd.app.data.model.StudentEntity

/**
 * تحويل صفوف ملف Excel/CSV إلى طلاب حسب الأعمدة التي اختارها المستخدم.
 * دالة نقية (بلا سياق أندرويد) لذا تُختبر مباشرة.
 *
 * - العمود العربي إلزامي: أي صف بلا اسم عربي يُتجاهل.
 * - أي فهرس سالب يعني «تجاهل هذا العمود».
 */
object StudentImportMapper {

    fun map(
        rows: List<List<String>>,
        classId: Long,
        numberIdx: Int,
        arabicIdx: Int,
        englishIdx: Int,
        skipHeader: Boolean
    ): List<StudentEntity> {
        val data = if (skipHeader && rows.size > 1) rows.drop(1) else rows
        return data.mapNotNull { row ->
            val name = row.getOrNull(arabicIdx)?.trim().orEmpty()
            if (name.isBlank()) {
                null
            } else {
                StudentEntity(
                    classId = classId,
                    studentNumber = if (numberIdx >= 0) row.getOrNull(numberIdx)?.trim().orEmpty() else "",
                    arabicName = name,
                    englishName = if (englishIdx >= 0) row.getOrNull(englishIdx)?.trim().orEmpty() else ""
                )
            }
        }
    }
}
