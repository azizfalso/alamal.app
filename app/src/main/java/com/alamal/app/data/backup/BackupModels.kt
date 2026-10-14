package com.alamal.app.data.backup

import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.CreditEntity
import com.alamal.app.data.db.entity.PeriodEntity
import com.alamal.app.data.db.entity.RemoteWorkEntity
import com.alamal.app.data.db.entity.WorkDayEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializable snapshot of the entire database.
 * Uses org.json (built into Android) — no extra dependencies.
 */
data class BackupPayload(
    val schemaVersion: Int = 1,
    val appVersion: String = "1.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val workDays: List<WorkDayEntity>,
    val breaks: List<BreakEntity>,
    val remoteWorks: List<RemoteWorkEntity>,
    val credits: List<CreditEntity>,
    val periods: List<PeriodEntity>,
    val settingsMap: Map<String, String> = emptyMap(),
) {

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("schemaVersion", schemaVersion)
        o.put("appVersion", appVersion)
        o.put("exportedAt", exportedAt)

        o.put("workDays", JSONArray().apply {
            workDays.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id)
                    put("periodId", d.periodId ?: JSONObject.NULL)
                    put("clockIn", d.clockIn)
                    put("clockOut", d.clockOut ?: JSONObject.NULL)
                    put("hourlyRate", d.hourlyRate)
                    put("note", d.note ?: JSONObject.NULL)
                    put("createdAt", d.createdAt)
                })
            }
        })

        o.put("breaks", JSONArray().apply {
            breaks.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("workDayId", b.workDayId)
                    put("startTime", b.startTime)
                    put("endTime", b.endTime)
                    put("note", b.note ?: JSONObject.NULL)
                })
            }
        })

        o.put("remoteWorks", JSONArray().apply {
            remoteWorks.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("periodId", r.periodId ?: JSONObject.NULL)
                    put("date", r.date)
                    put("amount", r.amount)
                    put("note", r.note ?: JSONObject.NULL)
                    put("createdAt", r.createdAt)
                })
            }
        })

        o.put("credits", JSONArray().apply {
            credits.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("periodId", c.periodId ?: JSONObject.NULL)
                    put("date", c.date)
                    put("amount", c.amount)
                    put("note", c.note ?: JSONObject.NULL)
                    put("createdAt", c.createdAt)
                })
            }
        })

        o.put("periods", JSONArray().apply {
            periods.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("startDate", p.startDate)
                    put("endDate", p.endDate)
                    put("carriedBalance", p.carriedBalance)
                    put("totalWages", p.totalWages)
                    put("totalRemote", p.totalRemote)
                    put("totalCredits", p.totalCredits)
                    put("amountDue", p.amountDue)
                    put("amountReceived", p.amountReceived)
                    put("remaining", p.remaining)
                    put("note", p.note ?: JSONObject.NULL)
                    put("archivedAt", p.archivedAt)
                })
            }
        })

        val sm = JSONObject()
        settingsMap.forEach { (k, v) -> sm.put(k, v) }
        o.put("settings", sm)

        return o
    }

    companion object {
        fun fromJson(o: JSONObject): BackupPayload {
            val workDays = o.optJSONArray("workDays")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val j = arr.getJSONObject(i)
                    WorkDayEntity(
                        id = j.optLong("id", 0L),
                        periodId = if (j.isNull("periodId")) null else j.optLong("periodId"),
                        clockIn = j.optLong("clockIn", 0L),
                        clockOut = if (j.isNull("clockOut")) null else j.optLong("clockOut"),
                        hourlyRate = j.optDouble("hourlyRate", 0.0),
                        note = if (j.isNull("note")) null else j.optString("note"),
                        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
                    )
                }
            } ?: emptyList()

            val breaks = o.optJSONArray("breaks")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val j = arr.getJSONObject(i)
                    BreakEntity(
                        id = j.optLong("id", 0L),
                        workDayId = j.optLong("workDayId", 0L),
                        startTime = j.optLong("startTime", 0L),
                        endTime = j.optLong("endTime", 0L),
                        note = if (j.isNull("note")) null else j.optString("note"),
                    )
                }
            } ?: emptyList()

            val remoteWorks = o.optJSONArray("remoteWorks")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val j = arr.getJSONObject(i)
                    RemoteWorkEntity(
                        id = j.optLong("id", 0L),
                        periodId = if (j.isNull("periodId")) null else j.optLong("periodId"),
                        date = j.optLong("date", 0L),
                        amount = j.optDouble("amount", 0.0),
                        note = if (j.isNull("note")) null else j.optString("note"),
                        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
                    )
                }
            } ?: emptyList()

            val credits = o.optJSONArray("credits")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val j = arr.getJSONObject(i)
                    CreditEntity(
                        id = j.optLong("id", 0L),
                        periodId = if (j.isNull("periodId")) null else j.optLong("periodId"),
                        date = j.optLong("date", 0L),
                        amount = j.optDouble("amount", 0.0),
                        note = if (j.isNull("note")) null else j.optString("note"),
                        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
                    )
                }
            } ?: emptyList()

            val periods = o.optJSONArray("periods")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val j = arr.getJSONObject(i)
                    PeriodEntity(
                        id = j.optLong("id", 0L),
                        startDate = j.optLong("startDate", 0L),
                        endDate = j.optLong("endDate", 0L),
                        carriedBalance = j.optDouble("carriedBalance", 0.0),
                        totalWages = j.optDouble("totalWages", 0.0),
                        totalRemote = j.optDouble("totalRemote", 0.0),
                        totalCredits = j.optDouble("totalCredits", 0.0),
                        amountDue = j.optDouble("amountDue", 0.0),
                        amountReceived = j.optDouble("amountReceived", 0.0),
                        remaining = j.optDouble("remaining", 0.0),
                        note = if (j.isNull("note")) null else j.optString("note"),
                        archivedAt = j.optLong("archivedAt", System.currentTimeMillis()),
                    )
                }
            } ?: emptyList()

            val settingsMap = mutableMapOf<String, String>()
            o.optJSONObject("settings")?.let { sm ->
                sm.keys().forEach { k -> settingsMap[k] = sm.optString(k) }
            }

            return BackupPayload(
                schemaVersion = o.optInt("schemaVersion", 1),
                appVersion = o.optString("appVersion", "1.0"),
                exportedAt = o.optLong("exportedAt", System.currentTimeMillis()),
                workDays = workDays,
                breaks = breaks,
                remoteWorks = remoteWorks,
                credits = credits,
                periods = periods,
                settingsMap = settingsMap,
            )
        }
    }
}
