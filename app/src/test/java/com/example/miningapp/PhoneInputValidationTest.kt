package com.example.miningapp

import com.example.miningapp.domain.usecase.PhoneInputValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneInputValidationTest {
    @Test
    fun normalizesInternationalPhoneNumber() {
        assertEquals("+22212345678", PhoneInputValidation.normalizePhoneNumber("+222 12 34 56 78"))
    }

    @Test
    fun rejectsInvalidInternationalPhoneNumbers() {
        assertNull(PhoneInputValidation.normalizePhoneNumber("12345678"))
        assertNull(PhoneInputValidation.normalizePhoneNumber("+123"))
        assertNull(PhoneInputValidation.normalizePhoneNumber("+012345678"))
    }

    @Test
    fun validatesSixDigitVerificationCode() {
        assertEquals(
            "Enter the 6-digit code.",
            PhoneInputValidation.verificationCodeError("12345")
        )
        assertNull(PhoneInputValidation.verificationCodeError("123456"))
    }
}
