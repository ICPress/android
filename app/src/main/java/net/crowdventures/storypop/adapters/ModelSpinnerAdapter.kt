package net.crowdventures.storypop.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import net.crowdventures.storypop.R
import net.crowdventures.storypop.models.GroqModel

class ModelSpinnerAdapter(
    context: Context,
    private val models: List<GroqModel>
) : ArrayAdapter<GroqModel>(context, android.R.layout.simple_spinner_item, models) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_spinner_model,
            parent,
            false
        )
        val model = getItem(position)
        view.findViewById<TextView>(R.id.model_name).text = model?.displayName
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_spinner_model_dropdown,
            parent,
            false
        )
        val model = getItem(position)
        view.findViewById<TextView>(R.id.model_name).text = model?.displayName
        view.findViewById<TextView>(R.id.model_id).text = model?.id
        view.findViewById<TextView>(R.id.model_limits).text = "${model?.rpm} RPM • ${model?.tpm} TPM"
        return view
    }
}