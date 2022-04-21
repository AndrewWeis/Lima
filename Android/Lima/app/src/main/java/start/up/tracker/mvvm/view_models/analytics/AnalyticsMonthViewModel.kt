package start.up.tracker.mvvm.view_models.analytics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import start.up.tracker.database.dao.AnalyticsDao
import start.up.tracker.entities.DayStat
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class AnalyticsMonthViewModel @Inject constructor(
    private val dao: AnalyticsDao,
) : ViewModel() {

    inner class ChartData(d: MutableList<DataEntry>, t: String, n: String, a: Int, i: String) {
        val data = d
        val title = t
        val monthName = n
        val average = a
        val date = i
    }

    val chartDataList: MutableList<ChartData> = ArrayList()

    private var _statMonth: MutableLiveData<Boolean> = MutableLiveData(false)
    val statMonth: LiveData<Boolean>
        get() = _statMonth

    init {
        loadTasks()
    }

    private fun loadTasks() = viewModelScope.launch {
        loadCompletedTasks()
        loadAllTasks()
        loadCompletedTasks()

        _statMonth.value = true
    }

    private suspend fun loadCompletedTasks() {
        val calendar = Calendar.getInstance()
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val stats = dao.getStatMonth(currentYear, currentMonth)

        val data: MutableList<DataEntry> = ArrayList()
        val currentMonthName = SimpleDateFormat("MMMM").format(calendar.time)
        val currentDate =
            StringBuilder().append(currentMonthName).append(" ").append(currentYear).toString()

        calendar.set(currentYear, currentMonth, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthList: MutableMap<Int, Int> = mutableMapOf()

        for (i in 1..maxDay) {
            monthList[i] = 0
        }

        var sum = 0

        stats.forEach {
            monthList[it.day] = it.completedTasks
            sum += it.completedTasks
        }

        val average = sum / maxDay

        monthList.forEach {
            data.add(ValueDataEntry(it.key.toString(), it.value))
        }

        chartDataList.add(ChartData(data, "Completed tasks", currentMonthName, average,
            currentDate))
    }

    private suspend fun loadAllTasks() {
        val calendar = Calendar.getInstance()
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val stats = dao.getStatMonth(currentYear, currentMonth)
        val data: MutableList<DataEntry> = ArrayList()
        val currentMonthName = SimpleDateFormat("MMMM").format(calendar.time)
        val currentDate =
            StringBuilder().append(currentMonthName).append(" ").append(currentYear).toString()

        calendar.set(currentYear, currentMonth, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthList: MutableMap<Int, Int> = mutableMapOf()

        for (i in 1..maxDay) {
            monthList[i] = 0
        }

        var sum = 0

        stats.forEach {
            monthList[it.day] = it.allTasks
            sum += it.allTasks
        }

        val average = sum / maxDay

        monthList.forEach {
            data.add(ValueDataEntry(it.key.toString(), it.value))
        }

        chartDataList.add(ChartData(data, "All tasks", currentMonthName, average,
            currentDate))
    }
}
