package com.verygoodsecurity.vgscollect.view.text.validation.card

import com.verygoodsecurity.vgscollect.R

enum class CardType(val regex:String,
//                    val mask:String,
                    val resId:Int,
                    val rangeNumber:Array<Int>,
                    val rangeCVV:Array<Int>) {
    ELO(
        "^(4011(78|79)|43(1274|8935)|45(1416|7393|763(1|2))|50(4175|6699|67[0-7][0-9]|9000)|627780|63(6297|6368)|650(03([^4])|04([0-9])|05(0|1)|4(0[5-9]|3[0-9]|8[5-9]|9[0-9])|5([0-2][0-9]|3[0-8])|9([2-6][0-9]|7[0-8])|541|700|720|901)|651652|655000|655021)",
//        "#### #### #### ####",
        android.R.color.transparent,
        arrayOf(16),
        arrayOf(3)
    ),
    VISA_ELECTRON(
        "^4(026|17500|405|508|844|91[37])",
        R.drawable.visa_electron,
        arrayOf(16),
        arrayOf(3)
    ),
    MAESTRO(
        "^(5018|5020|5038|6304|6390[0-9]{2}|67[0-9]{4})",
        R.drawable.maestro,
        (12..16).toList().toTypedArray(),
        arrayOf(3)
    ),
    FORBRUGSFORENINGEN(
        "^600",
        android.R.color.transparent,
        arrayOf(16),
        arrayOf(3)
    ),
    DANKORT(
        "^5019",
        android.R.color.transparent,
        arrayOf(16),
        arrayOf(3)
    ),
    VISA(
        "^4",
        R.drawable.visa,
        arrayOf(13,16,19),
        arrayOf(3)
    ),
    MASTERCARD(
        "^(5[1-5][0-9]{4}|677189)|^(222[1-9]|2[3-6]\\d{2}|27[0-1]\\d|2720)([0-9]{2})",
        R.drawable.mastercard,
        arrayOf(16),
        arrayOf(3)
    ),
    AMERICAN_EXPRESS(
        "^3[47]",
        R.drawable.amex,
        arrayOf(15),
        arrayOf(4)
    ),
    HIPERCARD(
        "^(384100|384140|384160|606282|637095|637568|60(?!11))",
        android.R.color.transparent,
        (14..19).toList().toTypedArray(),
        arrayOf(3)
    ),
    DINCLUB(
        "^(36|38|30[0-5])",
        R.drawable.din_club,
        arrayOf(14),
        arrayOf(3)
    ),
    DISCOVER(
        "^(6011|65|64[4-9]|622)",
        R.drawable.discover,
        arrayOf(16),
        arrayOf(3)
    ),
    UNIONPAY(
        "^62",
        android.R.color.transparent,
        (16..19).toList().toTypedArray(),
        arrayOf(3)
    ),
    JCB(
        "^35",
        R.drawable.jcb,
        (16..19).toList().toTypedArray(),
        arrayOf(3)
    ),
    LASER(
        "^(6706|6771|6709|6304)",
        android.R.color.transparent,
        (16..19).toList().toTypedArray(),
        arrayOf(3)
    ),
    NONE(
        "^\$a",
        android.R.color.transparent,
        (0..19).toList().toTypedArray(),
        arrayOf(3,4)
    );
}