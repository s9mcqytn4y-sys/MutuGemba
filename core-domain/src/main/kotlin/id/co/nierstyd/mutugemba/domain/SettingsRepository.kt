package id.co.nierstyd.mutugemba.domain

interface SettingsRepository {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )
}

object AppSettingsKeys {
    const val LAST_PAGE = "nav.lastPage"
    const val LAST_LINE_ID = "input.lastLineId"
    const val LAST_SHIFT_ID = "input.lastShiftId"
    const val LAST_PART_ID = "input.lastPartId"
    const val LAST_DEFECT_TYPE_ID = "input.lastDefectTypeId"
    const val ALLOW_DUPLICATE_INSPECTION = "input.allowDuplicateSameDay"
    const val DEV_QC_LINE_ID = "dev.qcLineId"
    const val DEV_DEMO_MODE = "dev.demoMode"
    const val DEV_USE_DUMMY_DATA = "dev.useDummyData"
    const val MANUAL_HOLIDAY_DATES = "calendar.manualHolidayDates"
    const val INSPECTION_PART_DEFECT_LAYOUT = "inspection.partDefectLayout"
    const val REPORT_ARCHIVE_ENTRIES = "report.archive.entries"
    const val SESSION_USER_ID = "session.userId"
    const val SESSION_USER_NAME = "session.userName"
    const val SESSION_EMPLOYEE_ID = "session.employeeId"
    const val SESSION_FULL_NAME = "session.fullName"
    const val SESSION_POSITION = "session.position"
    const val SESSION_DEPARTMENT = "session.department"
    const val SESSION_LINE_CODE = "session.lineCode"
    const val SESSION_ROLE = "session.role"
    const val SESSION_LOGIN_AT = "session.loginAt"
    const val SESSION_EXPIRES_AT = "session.expiresAt"
    const val SESSION_PHOTO_PATH = "session.photoPath"
}
