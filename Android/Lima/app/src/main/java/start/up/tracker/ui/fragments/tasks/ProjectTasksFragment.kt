package start.up.tracker.ui.fragments.tasks

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import start.up.tracker.R
import start.up.tracker.databinding.ProjectTasksFragmentBinding
import start.up.tracker.entities.Task
import start.up.tracker.mvvm.view_models.tasks.ProjectsTasksViewModel
import start.up.tracker.ui.data.entities.TasksEvent
import start.up.tracker.ui.extensions.list.ListExtension
import start.up.tracker.ui.fragments.BaseTasksFragment
import start.up.tracker.ui.list.adapters.tasks.ProjectsTasksAdapter
import start.up.tracker.ui.list.generators.tasks.TasksGenerator
import start.up.tracker.ui.list.view_holders.OnTaskClickListener
import start.up.tracker.utils.onQueryTextChanged

@AndroidEntryPoint
class ProjectTasksFragment :
    BaseTasksFragment(R.layout.project_tasks_fragment),
    OnTaskClickListener {

    private val viewModel: ProjectsTasksViewModel by viewModels()

    private var binding: ProjectTasksFragmentBinding? = null

    private lateinit var adapter: ProjectsTasksAdapter
    private var listExtension: ListExtension? = null
    private val generator: TasksGenerator = TasksGenerator()

    private lateinit var searchView: SearchView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ProjectTasksFragmentBinding.bind(view)

        initAdapter()
        initListeners()
        initObservers()
        initTaskEventListener()

        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchView.setOnQueryTextListener(null)
        binding = null
    }

    override fun onTaskClick(task: Task) {
        viewModel.onTaskSelected(task)
    }

    override fun onCheckBoxClick(task: Task) {
        viewModel.onTaskCheckedChanged(task)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_tasks, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        val pendingQuery = viewModel.searchQuery.value
        if (pendingQuery != null && pendingQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery, false)
        }

        searchView.onQueryTextChanged {
            viewModel.searchQuery.value = it
        }

        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                viewModel.hideCompleted.first() ?: false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_hide_completed_tasks -> {
                item.isChecked = !item.isChecked
                viewModel.onHideCompletedClick(item.isChecked)
                true
            }
            R.id.action_delete_all_completed_tasks -> {
                viewModel.onDeleteAllCompletedClick()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initTaskEventListener() = viewLifecycleOwner.lifecycleScope.launchWhenStarted {
        viewModel.tasksEvent.collect { event ->
            when (event) {
                is TasksEvent.ShowUndoDeleteTaskMessage -> {
                    showUndoDeleteSnackbar() { viewModel.onUndoDeleteTaskClick(event.task) }
                }

                is TasksEvent.NavigateToAddTaskScreen -> {
                    val action = ProjectTasksFragmentDirections.actionProjectTasksToAddEditTask(
                        title = "Add new task",
                        categoryId = viewModel.categoryId
                    )
                    navigateTo(action)
                }

                is TasksEvent.NavigateToEditTaskScreen -> {
                    val action = ProjectTasksFragmentDirections.actionProjectTasksToAddEditTask(
                        event.task,
                        "Edit task",
                        viewModel.categoryId
                    )
                    navigateTo(action)
                }

                is TasksEvent.ShowTaskSavedConfirmationMessage -> {
                    showTaskSavedMessage(event.msg)
                }

                is TasksEvent.NavigateToDeleteAllCompletedScreen -> {
                    val action =
                        ProjectTasksFragmentDirections.actionGlobalDeleteAllCompletedDialog()
                    navigateTo(action)
                }
            }
        }
    }

    private fun initListeners() {
        binding?.editTaskFab?.setOnClickListener {
            viewModel.onAddNewTaskClick()
        }

        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
            viewModel.onAddEditResult(result)
        }
    }

    private fun initObservers() {
        viewModel.tasksOfCategory.observe(viewLifecycleOwner) {
            showTasks(it)
        }
    }

    private fun showTasks(tasks: List<Task>) {
        adapter.addListItems(generator.createListItems(tasks))
    }

    private fun initAdapter() {
        adapter = ProjectsTasksAdapter(
            layoutInflater = layoutInflater,
            listener = this
        )

        listExtension = ListExtension(binding?.projectTasksList)
        listExtension?.setLayoutManager()
        listExtension?.setAdapter(adapter)

        listExtension?.attachSwipeToAdapter(adapter, viewModel)
    }
}
