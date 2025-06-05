import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemRabbitAndPoultryBinding
import com.example.farmdatapod.models.RabbitAndPoultryDiv
import com.google.android.material.textfield.TextInputEditText

class RabbitAndPoultryAdapter(
    private var items: List<RabbitAndPoultryDiv>
) : RecyclerView.Adapter<RabbitAndPoultryAdapter.ViewHolder>() {

    private var itemChangedListener: ((Int, RabbitAndPoultryDiv) -> Unit)? = null

    inner class ViewHolder(private val binding: ItemRabbitAndPoultryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RabbitAndPoultryDiv) {
            binding.apply {
                gradeInput.setText(item.grade)
                moistureInput.setText(item.moisture)
                foreignMatterInput.setText(item.foreign_matter)
                sizeInput.setText(item.size)
                other1Input.setText(item.other1)
                other2Input.setText(item.other2)

                setTextWatcher(gradeInput) { text ->
                    item.grade = text
                    notifyItemChanged(item)
                }
                setTextWatcher(moistureInput) { text ->
                    item.moisture = text
                    notifyItemChanged(item)
                }
                setTextWatcher(foreignMatterInput) { text ->
                    item.foreign_matter = text
                    notifyItemChanged(item)
                }
                setTextWatcher(sizeInput) { text ->
                    item.size = text
                    notifyItemChanged(item)
                }
                setTextWatcher(other1Input) { text ->
                    item.other1 = text
                    notifyItemChanged(item)
                }
                setTextWatcher(other2Input) { text ->
                    item.other2 = text
                    notifyItemChanged(item)
                }
            }
        }

        private fun setTextWatcher(inputField: TextInputEditText, onTextChanged: (String) -> Unit) {
            inputField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onTextChanged(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        private fun notifyItemChanged(item: RabbitAndPoultryDiv) {
            itemChangedListener?.invoke(adapterPosition, item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRabbitAndPoultryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setOnItemChangedListener(listener: (Int, RabbitAndPoultryDiv) -> Unit) {
        this.itemChangedListener = listener
    }

    fun updateItems(newItems: List<RabbitAndPoultryDiv>) {
        items = newItems
        notifyDataSetChanged()
    }
}