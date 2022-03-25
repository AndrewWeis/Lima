package start.up.tracker.analytics

import start.up.tracker.analytics.entities.AnalyticsMessage
import start.up.tracker.entities.Task
import java.util.ArrayList

interface Principle {
    fun setStatus(status: Boolean)
    fun canBeEnabled(activePrinciplesIds: List<Int>): Boolean
    fun getStatus(): Boolean
    fun setNotifications(notifications: Boolean)
    fun getNotifications(): Boolean
    fun getTimeToRead(): Int?
    fun getReference(): String?
    fun getName(): String?
    fun getId(): Int?
    fun getIncompatiblePrinciplesIds(): ArrayList<Int>?
    suspend fun logicAddTask(task: Task) : AnalyticsMessage?
    suspend fun logicEditTask(task: Task) : AnalyticsMessage?
}