package com.verygoodsecurity.vgscollect.view.card.validation.card

import com.verygoodsecurity.vgscollect.view.card.validation.MuttableValidator
import com.verygoodsecurity.vgscollect.view.card.validation.VGSValidator

/** @suppress */
class CardNumberValidator() : MuttableValidator {
    private val validators = mutableListOf<VGSValidator>()

    override fun clearRules() {
        validators.clear()
    }

    override fun addRule(validator: VGSValidator) {
        validators.add(validator)
    }

    override fun isValid(content: String?): Boolean {
        var isValid = true
        for(checkSumValidator in validators) {
            isValid = isValid && checkSumValidator.isValid(content)
        }
        return isValid
    }
}