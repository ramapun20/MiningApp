package com.example.miningapp.domain.usecase

object PhoneInputValidation {
    fun normalizePhoneNumber(phone: String): String? {
        if (!phone.matches(Regex("[+0-9\\s().-]+"))) return null
        val normalized = phone.replace(Regex("[\\s().-]"), "")
        val digits = normalized.removePrefix("+")
        return normalized.takeIf {
            normalized.startsWith("+") &&
                digits.length in 8..15 &&
                digits.firstOrNull()?.let { first -> first in '1'..'9' } == true
        }
    }

    fun verificationCodeError(code: String): String? =
        if (code.matches(Regex("\\d{6}"))) null else "Enter the 6-digit code."
}
