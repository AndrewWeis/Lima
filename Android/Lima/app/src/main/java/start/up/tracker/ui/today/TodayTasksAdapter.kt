package start.up.tracker.ui.today

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import start.up.tracker.R
import start.up.tracker.data.models.ExtendedTask
import start.up.tracker.databinding.ItemTaskTodayBinding


class TodayTasksAdapter(private val listener: OnItemClickListener) : ListAdapter<ExtendedTask,
        TodayTasksAdapter.TasksViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TasksViewHolder {
        val binding = ItemTaskTodayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TasksViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TasksViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class TasksViewHolder(private val binding: ItemTaskTodayBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                root.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val todayTask = getItem(position)
                        listener.onItemClick(todayTask)
                    }
                }

                checkBoxCompleted.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val todayTask = getItem(position)
                        listener.onCheckBoxClick(todayTask, checkBoxCompleted.isChecked)
                    }
                }
            }
        }

        fun bind(extendedTask: ExtendedTask) {
            binding.apply {
                checkBoxCompleted.isChecked = extendedTask.completed
                textViewName.text = extendedTask.taskName
                textViewName.paint.isStrikeThruText = extendedTask.completed
                textCategoryName.text = extendedTask.categoryName
                textCategoryName.setTextColor(extendedTask.color)
                categoryCircle.background.setTint(extendedTask.color)

                if (extendedTask.priority == 4) {
                    icPriority.visibility = View.GONE
                } else {
                    icPriority.setImageResource(chooseIconDrawable(extendedTask.priority))
                }
            }
        }

        private fun chooseIconDrawable(priority: Int): Int {
            return when(priority) {
                1 -> R.drawable.ic_priority_1
                2 -> R.drawable.ic_priority_2
                3 -> R.drawable.ic_priority_3
                else -> R.drawable.ic_android // This should never be reached
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(extendedTask: ExtendedTask)
        fun onCheckBoxClick(extendedTask: ExtendedTask, isChecked: Boolean)
    }

    class DiffCallback : DiffUtil.ItemCallback<ExtendedTask>() {
        override fun areItemsTheSame(oldItem: ExtendedTask, newItem: ExtendedTask) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ExtendedTask, newItem: ExtendedTask) =
            oldItem == newItem
    }
}