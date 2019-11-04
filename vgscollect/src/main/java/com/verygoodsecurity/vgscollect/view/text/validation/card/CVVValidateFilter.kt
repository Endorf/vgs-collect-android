package com.verygoodsecurity.vgscollect.view.text.validation.card

import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import java.util.regex.Pattern

class CVVValidateFilter: InputFilter {
    override fun filter(
        source : CharSequence?,
        start : Int,
        end: Int,
        dest : Spanned?,
        dstart : Int,
        dend: Int
    ): CharSequence {
        for (i in start until end) {
            val checkMe = source.toString()
            val pattern = Pattern.compile("[1234567890]*")
            val matcher = pattern.matcher(checkMe)
            val valid = matcher.matches()
            return if (valid) {
                source?:""
            } else {
                ""
            }
        }
        return ""
    }
}