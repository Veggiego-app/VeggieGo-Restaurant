package com.veggiego.restaurant

import android.content.Context
import android.widget.Toast

object KotPrinter {

    fun print(

        context: Context,

        text: String

    ) {

        Toast.makeText(

            context,

            text,

            Toast.LENGTH_LONG

        ).show()
    }
}