package start.up.tracker.ui.fragments.edit_task

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import start.up.tracker.R
import start.up.tracker.data.fields.Field
import start.up.tracker.databinding.EditTaskFragmentBinding
import start.up.tracker.entities.Task
import start.up.tracker.mvvm.view_models.edit_task.EditTaskViewModel
import start.up.tracker.ui.data.constants.ListItemIds
import start.up.tracker.ui.data.entities.ListItem
import start.up.tracker.ui.data.entities.TasksEvent
import start.up.tracker.ui.data.entities.chips.ChipData
import start.up.tracker.ui.data.entities.chips.ChipsData
import start.up.tracker.ui.data.entities.edit_task.ActionIcon
import start.up.tracker.ui.data.entities.edit_task.ActionIcons
import start.up.tracker.ui.data.entities.tasks.TasksData
import start.up.tracker.ui.extensions.list.ListExtension
import start.up.tracker.ui.fragments.tasks.base.BaseTasksFragment
import start.up.tracker.ui.list.adapters.edit_task.EditTaskAdapter
import start.up.tracker.ui.list.generators.edit_task.EditTaskInfoGenerator
import start.up.tracker.ui.list.view_holders.edit_task.ActionIconViewHolder
import start.up.tracker.ui.list.view_holders.edit_task.ChipsViewHolder
import start.up.tracker.ui.list.view_holders.forms.SelectInputViewHolder
import start.up.tracker.ui.list.view_holders.tasks.AddSubtaskViewHolder
import start.up.tracker.ui.list.view_holders.tasks.OnTaskClickListener
import start.up.tracker.ui.views.forms.base.BaseInputView
import start.up.tracker.utils.TimeHelper
import start.up.tracker.utils.resources.ResourcesUtils
import start.up.tracker.utils.screens.ExtraCodes
import java.util.*

@AndroidEntryPoint
class EditTaskFragment :
    BaseTasksFragment(R.layout.edit_task_fragment),
    BaseInputView.TextInputListener,
    SelectInputViewHolder.TextInputSelectionListener,
    TimePickerDialog.OnTimeSetListener,
    DatePickerDialog.OnDateSetListener,
    ChipsViewHolder.ProjectViewHolderListener,
    OnTaskClickListener,
    AddSubtaskViewHolder.OnAddSubtaskClickListener,
    ActionIconViewHolder.ActionIconClickListener {

    private val viewModel: EditTaskViewModel by viewModels()

    private var binding: EditTaskFragmentBinding? = null

    private lateinit var adapter: EditTaskAdapter
    private var listExtension: ListExtension? = null
    private val generator: EditTaskInfoGenerator = EditTaskInfoGenerator()

    // todo(костыльное решение, подумать как можно сделать по-другому)
    private var isTaskTimeTypeStart = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = EditTaskFragmentBinding.bind(view)

        initAdapter()
        initListeners()
        initObservers()
        initResultListeners()
        initEventsListener()
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveDataAboutSubtask()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        listExtension = null
    }

    override fun onTextInputDataChange(listItem: ListItem) {
        val value = listItem.data as String
        when (listItem.id) {
            ListItemIds.TASK_TITLE -> viewModel.onTaskTitleChanged(value)
            ListItemIds.TASK_DESCRIPTION -> viewModel.onTaskDescriptionChanged(value)
        }
    }

    override fun onFocusLose(listItem: ListItem) {
        hideKeyboard()
        binding?.content?.requestFocus()
    }

    override fun onFocusGain(listItem: ListItem) {
        /*
         * Код добавлен, так как не всегда при захвате фокуса показывается клавиатура.
         * Например, когда используется функция copy-paste
         */
        showKeyboard()
    }

    override fun onClearClick(listItem: ListItem) {
        when (listItem.id) {
            ListItemIds.TASK_TITLE -> viewModel.onTaskTitleClearClick()
            ListItemIds.TASK_DESCRIPTION -> viewModel.onTaskDescriptionClearClick()
        }
    }

    override fun onDoneClick(item: ListItem) {
        hideKeyboard()
    }

    override fun onTextInputSelectionClick(listItem: ListItem) {
        when (listItem.id) {
            ListItemIds.TASK_TIME_START -> {
                isTaskTimeTypeStart = true
                openTimePicker()
            }
            ListItemIds.TASK_TIME_END -> {
                isTaskTimeTypeStart = false
                openTimePicker()
            }
            ListItemIds.TASK_DATE ->
                openDatePicker()
        }
    }

    override fun onChipClick(listItem: ListItem) {
        val chipData = listItem.data as ChipData

        when (listItem.id) {
            ListItemIds.TASK_PROJECTS -> viewModel.onCategoryChipChanged(chipData)
        }
    }

    override fun onTaskClick(task: Task) {
        viewModel.onTaskSelected(task)
    }

    override fun onCheckBoxClick(task: Task) {
        viewModel.onTaskCheckedChanged(task)
    }

    override fun onAddSubtaskClick() {
        viewModel.onAddSubtaskClick()
    }

    override fun onTimeSet(timePicker: TimePicker, hours: Int, minutes: Int) {
        if (isTaskTimeTypeStart) {
            viewModel.onTaskStartTimeChanged(hours * 60 + minutes)
        } else {
            viewModel.onTaskEndTimeChanged(hours * 60 + minutes)
        }
    }

    override fun onDateSet(datePicker: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val milliseconds = TimeHelper.getDateInMilliseconds(year, month, dayOfMonth)
        viewModel.onTaskDateChanged(milliseconds)
    }

    override fun onActionClickListener(actionIconId: Int) {
        when (actionIconId) {
            ActionIcon.ICON_PRIORITY -> viewModel.onIconPriorityClick()
        }
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            requireContext(), R.style.DatePicker, this,
            calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DAY_OF_MONTH]
        )

        datePickerDialog.datePicker.minDate = Calendar.getInstance().timeInMillis

        datePickerDialog.show()
    }

    private fun openTimePicker() {
        val calendar = Calendar.getInstance()

        val timePickerDialog = TimePickerDialog(
            requireContext(), R.style.DatePicker, this,
            calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], TimeHelper.isSystem24Hour
        )

        timePickerDialog.show()
    }

    private fun onSubtasksNumberChanged(number: Int) {
        viewModel.onSubtasksNumberChanged(number)
    }

    private fun onCompletedSubtasksNumberChanged(number: Int) {
        viewModel.onCompletedSubtasksNumberChanged(number)
    }

    private fun showActionsIcons(icons: ActionIcons) {
        val listItem: ListItem = generator.createActionsIconsListItem(icons)

        if (binding?.editTasksList?.isComputingLayout == false) {
            adapter.setActionsIconsListItem(listItem)
            return
        }

        binding?.editTasksList?.post {
            adapter.setActionsIconsListItem(listItem)
        }
    }

    private fun showAddSubtaskButton() {
        val listItem: ListItem = generator.createAddSubtaskButton()

        if (binding?.editTasksList?.isComputingLayout == false) {
            adapter.setAddSubtaskButtonListItem(listItem)
            return
        }

        binding?.editTasksList?.post {
            adapter.setAddSubtaskButtonListItem(listItem)
        }
    }

    private fun showSubtasks(subtasks: TasksData) {
        val listItem: ListItem = generator.createSubtasksListItems(subtasks)

        if (binding?.editTasksList?.isComputingLayout == false) {
            adapter.setSubtasksListItem(listItem)
            return
        }

        binding?.editTasksList?.post {
            adapter.setSubtasksListItem(listItem)
        }
    }

    private fun showProjects(chips: ChipsData) {
        val listItem: ListItem = generator.createProjectsChipsListItems(chips)

        if (binding?.editTasksList?.isComputingLayout == false) {
            adapter.setProjectChipListItem(listItem)
            return
        }

        binding?.editTasksList?.post {
            adapter.setProjectChipListItem(listItem)
        }
    }

    private fun showTitleField(field: Field<String>) {
        val listItem: ListItem = generator.createTitleListItem(field)

        if (binding?.editTasksList?.isComputingLayout == false) {
            adapter.setTitleItem(listItem)
            return
        }

        binding?.editTasksList?.post {
            adapter.setTitleItem(listItem)
        }
    }

    private fun showEditableTaskInfo(task: Task) {
        setDescription(task)
        setStartTime(task)
        setEndTime(task)
        setDate(task)
    }

    private fun setDate(task: Task) {
        val listItem: ListItem = generator.createTaskDateListItem(task.date)

        if (binding?.editTasksList?.isComputingLayout == false) {
            adapter.setDateItem(listItem)
            return
        }

        binding?.editTasksList?.post {
            adapter.setDateItem(listItem)
        }
    }

    private fun setEndTime(task: Task) {
        val listItem: ListItem = generator.createTaskEndTimeListItem(task.endTimeInMinutes)

        if (binding?.editTasksList?.isComputingLayout == false) {
            adapter.setEndTimeItem(listItem)
            return
        }

        binding?.editTasksList?.post {
            adapter.setEndTimeItem(listItem)
        }
    }

    private fun setStartTime(task: Task) {
        val listItem: ListItem = generator.createTaskStartTimeListItem(task.startTimeInMinutes)

        if (binding?.editTasksList?.isComputingLayout == false) {
            adapter.setStartTimeItem(listItem)
            return
        }

        binding?.editTasksList?.post {
            adapter.setStartTimeItem(listItem)
        }
    }

    private fun setDescription(task: Task) {
        val listItem: ListItem = generator.createTaskDescriptionListItem(task.description)

        if (binding?.editTasksList?.isComputingLayout == false) {
            adapter.setDescriptionItem(listItem)
            return
        }

        binding?.editTasksList?.post {
            adapter.setDescriptionItem(listItem)
        }
    }

    private fun initAdapter() {
        adapter = EditTaskAdapter(
            viewModel = viewModel,
            layoutInflater = layoutInflater,
            textInputListener = this,
            textInputSelectionListener = this,
            projectViewHolderListener = this,
            onTaskClickListener = this,
            onAddSubtaskListener = this,
            actionIconClickListener = this
        )

        listExtension = ListExtension(binding?.editTasksList)
        listExtension?.setVerticalLayoutManager()
        listExtension?.setAdapter(adapter)
    }

    private fun initObservers() {
        viewModel.taskInfoLiveData.observe(viewLifecycleOwner) { task ->
            showEditableTaskInfo(task)
        }

        viewModel.titleField.observe(viewLifecycleOwner) { field ->
            showTitleField(field)
        }

        viewModel.projectsChips.observe(viewLifecycleOwner) { projectsChips ->
            showProjects(projectsChips)
        }

        viewModel.subtasks.observe(viewLifecycleOwner) { subtasks ->
            showSubtasks(subtasks)
            showAddSubtaskButton()
            onSubtasksNumberChanged(subtasks.tasks.size)
            onCompletedSubtasksNumberChanged(subtasks.tasks.count { it.completed })
        }

        viewModel.actionsIcons.observe(viewLifecycleOwner) { icons ->
            showActionsIcons(icons)
        }
    }

    private fun initResultListeners() {
        setFragmentResultListener(ExtraCodes.IGNORE_CLICKED_REQUEST) { _, _ ->
            viewModel.saveTask()
        }

        setFragmentResultListener(ExtraCodes.PRIORITY_REQUEST) { requestKey, bundle ->
            val result = bundle.getInt(requestKey)
            viewModel.onPriorityChanged(result)
        }
    }

    private fun initEventsListener() = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
        viewModel.tasksEvent.collect { event ->
            when (event) {
                is TasksEvent.NavigateBack -> {
                    findNavController().popBackStack()
                }

                is TasksEvent.NavigateToAddTaskScreen -> {
                    val action = EditTaskFragmentDirections.actionAddEditTaskSelf(
                        title = ResourcesUtils.getString(R.string.title_add_task),
                        projectId = viewModel.task.projectId,
                        parentTaskId = viewModel.task.taskId
                    )
                    navigateTo(action)
                }

                is TasksEvent.NavigateToEditTaskScreen -> {
                    val action = EditTaskFragmentDirections.actionAddEditTaskSelf(
                        task = event.task,
                        title = ResourcesUtils.getString(R.string.title_edit_task),
                        projectId = event.task.projectId,
                        parentTaskId = event.task.taskId
                    )
                    navigateTo(action)
                }

                is TasksEvent.ShowAnalyticMessageDialog -> {
                    val action = EditTaskFragmentDirections.actionAddEditTaskToAnalyticsMessagesDialog(
                            messages = event.messages
                        )
                    navigateTo(action)
                }

                is TasksEvent.NavigateToPriorityDialog -> {
                    val action = EditTaskFragmentDirections.actionAddEditTaskToPriority(
                        // todo (method for analytics which returns what technique we need to use for the display of priority)
                        // ActiveAnalytics.getPrincipleIdForPriorityDisplay
                        selectedPriorityId = event.priorityId,
                    )
                    navigateTo(action)
                }
            }
        }
    }

    private fun initListeners() {
        binding?.doneButton?.setOnClickListener {
            viewModel.onSaveClick()
        }
    }
}
