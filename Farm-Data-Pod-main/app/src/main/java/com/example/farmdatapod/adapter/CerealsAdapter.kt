import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.CerealsDiv
import com.google.android.material.textfield.TextInputEditText

class CerealsAdapter(private var items: List<CerealsDiv>) : RecyclerView.Adapter<CerealsAdapter.ViewHolder>() {

    private var onItemChangedListener: ((Int, String) -> Unit)? = null

    fun setOnItemChangedListener(listener: (Int, String) -> Unit) {
        onItemChangedListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gradeInput: TextInputEditText = itemView.findViewById(R.id.gradeInput)
        val moistureInput: TextInputEditText = itemView.findViewById(R.id.moistureInput)
        val maturityInput: TextInputEditText = itemView.findViewById(R.id.maturityInput)
        val foreignMatterInput: TextInputEditText = itemView.findViewById(R.id.foreignMatterInput)
        val mechanicalDamageInput: TextInputEditText = itemView.findViewById(R.id.mechanicalDamageInput)
        val sizeInput: TextInputEditText = itemView.findViewById(R.id.sizeInput)
        val pestDiseaseInput: TextInputEditText = itemView.findViewById(R.id.pestDiseaseInput)
        val mouldInput: TextInputEditText = itemView.findViewById(R.id.mouldInput)
        val other1Input: TextInputEditText = itemView.findViewById(R.id.other1Input)
        val other2Input: TextInputEditText = itemView.findViewById(R.id.other2Input)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cereals, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.gradeInput.setText(item.grade)
        holder.moistureInput.setText(item.moisture)
        holder.maturityInput.setText(item.maturity)
        holder.foreignMatterInput.setText(item.foreign_matter)
        holder.mechanicalDamageInput.setText(item.mechanical_damage)
        holder.sizeInput.setText(item.size)
        holder.pestDiseaseInput.setText(item.pest_and_disease)
        holder.mouldInput.setText(item.mould)
        holder.other1Input.setText(item.other1)
        holder.other2Input.setText(item.other2)

        setupTextWatchers(holder)
    }

    override fun getItemCount() = items.size

    private fun setupTextWatchers(holder: ViewHolder) {
        holder.gradeInput.addTextChangedListener(createTextWatcher(0))
        holder.moistureInput.addTextChangedListener(createTextWatcher(1))
        holder.maturityInput.addTextChangedListener(createTextWatcher(2))
        holder.foreignMatterInput.addTextChangedListener(createTextWatcher(3))
        holder.mechanicalDamageInput.addTextChangedListener(createTextWatcher(4))
        holder.sizeInput.addTextChangedListener(createTextWatcher(5))
        holder.pestDiseaseInput.addTextChangedListener(createTextWatcher(6))
        holder.mouldInput.addTextChangedListener(createTextWatcher(7))
        holder.other1Input.addTextChangedListener(createTextWatcher(8))
        holder.other2Input.addTextChangedListener(createTextWatcher(9))
    }

    private fun createTextWatcher(position: Int): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                onItemChangedListener?.invoke(position, s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    fun updateItems(newItems: List<CerealsDiv>) {
        items = newItems
        notifyDataSetChanged()
    }
}