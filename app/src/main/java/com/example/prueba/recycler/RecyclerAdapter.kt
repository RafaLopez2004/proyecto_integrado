package com.example.prueba.recycler

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prueba.R
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup


class RecyclerAdapter : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    var data: MutableList<DataModel>  = ArrayList()
    lateinit var context:Context

    fun setUp(context: Context){
        this.context = context
    }

    fun addData(data : DataModel){
        this.data.add(data)
    }

    fun setArrayData(data : MutableList<DataModel>){
        this.data = data
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.history_card, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val day: TextView = view.findViewById(R.id.day)
        val inHour: TextView = view.findViewById(R.id.in_hour)
        val outHour: TextView = view.findViewById(R.id.out_hour)

        fun bind(data:DataModel){
            day.text = data.day
            inHour.text = data.chkin
            outHour.text = data.chkout
        }
    }

}