package start.up.tracker.ui.data.entities

import start.up.tracker.entities.Task

sealed class TasksEvent {
    object NavigateToAddTaskScreen : TasksEvent()
    object NavigateBack : TasksEvent()
    object NavigateToDeleteAllCompletedScreen : TasksEvent()
    data class NavigateToEditTaskScreen(val task: Task) : TasksEvent()
    data class ShowUndoDeleteTaskMessage(val task: Task) : TasksEvent()
}
