package start.up.tracker.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import start.up.tracker.database.dao.AnalyticsDao
import start.up.tracker.database.dao.TaskAnalyticsDao
import start.up.tracker.entities.DayStat
import start.up.tracker.entities.Task
import start.up.tracker.entities.TaskAnalytics
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Analytics @Inject constructor(
    private val analyticsDao: AnalyticsDao,
) {

    suspend fun addTaskToStatisticOnCompletion() = withContext(Dispatchers.Default) {
        val calendar = Calendar.getInstance()
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val currentDay: Int = calendar.get(Calendar.DAY_OF_MONTH)

        var dayStat: DayStat? = analyticsDao.getStatDay(currentYear, currentMonth, currentDay)

        if (dayStat == null) {
            dayStat = DayStat(day = currentDay, month = currentMonth, year = currentYear)
            analyticsDao.insertDayStat(dayStat)
        } else {
            val newDayStat = dayStat.copy(completedTasks = dayStat.completedTasks + 1)
            analyticsDao.updateDayStat(newDayStat)
        }
    }

    suspend fun addTaskToStatisticOnCreate() = withContext(Dispatchers.Default) {
        val calendar = Calendar.getInstance()
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val currentDay: Int = calendar.get(Calendar.DAY_OF_MONTH)

        var dayStat: DayStat? = analyticsDao.getStatDay(currentYear, currentMonth, currentDay)

        if (dayStat == null) {
            dayStat = DayStat(day = currentDay, month = currentMonth, year = currentYear)
            analyticsDao.insertDayStat(dayStat)
        } else {
            val newDayStat = dayStat.copy(allTasks = dayStat.allTasks + 1)
            analyticsDao.updateDayStat(newDayStat)
        }
    }

    suspend fun addTaskToStatisticOnEdit() = withContext(Dispatchers.Default) {
        val calendar = Calendar.getInstance()
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val currentDay: Int = calendar.get(Calendar.DAY_OF_MONTH)

        val dayStat: DayStat = analyticsDao.getStatDay(currentYear, currentMonth, currentDay)

        val newDayStat = dayStat.copy(allTasks = dayStat.allTasks + 1)
        analyticsDao.updateDayStat(newDayStat)
    }
}
