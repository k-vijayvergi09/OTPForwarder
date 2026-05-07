package com.samsung.android.otpforwarder.core.common.validation

/**
 * Lightweight phone number validator that does not require a network call or a
 * full libphonenumber dependency.
 *
 * Rules (based on ITU-T E.164):
 *  - Allowed formatting characters (stripped before analysis): spaces, dashes,
 *    dots, parentheses, and a leading `+`.
 *  - After stripping: must consist entirely of digits.
 *  - Length: 7–15 digits (minimum for local/national numbers; E.164 maximum).
 *
 * Examples that pass:
 *   "+91 98765 43210"  → "919876543210"  (12 digits)
 *   "+1 (555) 867-5309" → "15558675309" (11 digits)
 *   "9876543210"        → "9876543210"  (10 digits)
 *
 * Examples that fail:
 *   "123"      → too short (3 digits)
 *   "abc-1234" → non-digit characters remain after stripping
 *   ""         → blank
 */
object PhoneNumberValidator {

    private val ALLOWED_FORMAT_CHARS = Regex("[\\s().+\\-]")

    /**
     * Returns `true` if [number] is a plausibly valid phone number.
     */
    fun isValid(number: String): Boolean {
        val digits = number.trim().replace(ALLOWED_FORMAT_CHARS, "")
        return digits.length in 7..15 && digits.all { it.isDigit() }
    }

    /**
     * Returns a human-readable error message if [number] is invalid, or
     * `null` if the number looks fine.
     */
    fun errorOrNull(number: String): String? {
        val trimmed = number.trim()
        if (trimmed.isBlank()) return "Phone number cannot be empty"

        val digits = trimmed.replace(ALLOWED_FORMAT_CHARS, "")
        if (!digits.all { it.isDigit() }) return "Only digits, spaces, +, -, (, ) are allowed"
        if (digits.length < 7)  return "Number too short (minimum 7 digits)"
        if (digits.length > 15) return "Number too long (maximum 15 digits)"
        return null
    }
}
