package com.example.farmdatapod.utils


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.example.farmdatapod.R


class DialogUtils(private val context: Context) {
    fun buildLoadingDialog(): AlertDialog {
        val builder = AlertDialog.Builder(context)
        val customView = LayoutInflater.from(context).inflate(R.layout.layout_dialog_loading, null)
        builder.setView(customView)

        val alertDialog = builder.create()

        // Set a transparent background
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Make sure it's not cancelable by clicking outside the dialog
        alertDialog.setCanceledOnTouchOutside(false)

        return alertDialog
    }


}