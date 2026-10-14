package com.alamal.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val RECORDS = "records"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"
    const val WORK_DAY_FORM = "work_day_form"
    const val REMOTE_WORK_FORM = "remote_work_form"
    const val CREDIT_FORM = "credit_form"
    const val PAYMENT = "payment"
    const val STATS = "stats"

    const val ARG_DAY_ID = "dayId"
    const val ARG_ITEM_ID = "id"

    fun workDayForm(dayId: Long): String = "$WORK_DAY_FORM?$ARG_DAY_ID=$dayId"
    fun workDayFormPattern(): String = "$WORK_DAY_FORM?$ARG_DAY_ID={$ARG_DAY_ID}"

    fun remoteWorkForm(id: Long): String = "$REMOTE_WORK_FORM?$ARG_ITEM_ID=$id"
    fun remoteWorkFormPattern(): String = "$REMOTE_WORK_FORM?$ARG_ITEM_ID={$ARG_ITEM_ID}"

    fun creditForm(id: Long): String = "$CREDIT_FORM?$ARG_ITEM_ID=$id"
    fun creditFormPattern(): String = "$CREDIT_FORM?$ARG_ITEM_ID={$ARG_ITEM_ID}"
}
