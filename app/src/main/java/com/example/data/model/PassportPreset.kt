package com.example.data.model

enum class SizeUnit(val label: String) {
    PX("px"),
    MM("mm"),
    CM("cm"),
    INCH("in")
}

data class PassportPreset(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val widthMm: Float,
    val heightMm: Float,
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int = 300,
    val maxKb: Int? = null,
    val instructionsEn: String = "",
    val instructionsHi: String = ""
) {
    companion object {
        val ALL_PRESETS = listOf(
            PassportPreset(
                id = "in_passport",
                nameEn = "India Passport",
                nameHi = "भारत पासपोर्ट",
                widthMm = 35f,
                heightMm = 45f,
                widthPx = 413,
                heightPx = 531,
                dpi = 300,
                maxKb = 100,
                instructionsEn = "35x45 mm, white background, 70-80% face coverage",
                instructionsHi = "35x45 मिमी, सफेद बैकग्राउंड, 70-80% चेहरा"
            ),
            PassportPreset(
                id = "in_visa_oci",
                nameEn = "India Visa / OCI",
                nameHi = "भारत वीजा / ओसीआई",
                widthMm = 51f,
                heightMm = 51f,
                widthPx = 600,
                heightPx = 600,
                dpi = 300,
                maxKb = 200,
                instructionsEn = "2x2 inch (51x51 mm), square, light plain background",
                instructionsHi = "2x2 इंच (51x51 मिमी), चौकोर, हल्का बैकग्राउंड"
            ),
            PassportPreset(
                id = "in_pan_aadhaar",
                nameEn = "PAN Card / Aadhaar",
                nameHi = "पैन कार्ड / आधार",
                widthMm = 25f,
                heightMm = 35f,
                widthPx = 295,
                heightPx = 413,
                dpi = 300,
                maxKb = 50,
                instructionsEn = "2.5x3.5 cm, standard Indian identity size",
                instructionsHi = "2.5x3.5 सेमी, मानक भारतीय आईडी आकार"
            ),
            PassportPreset(
                id = "us_passport_visa",
                nameEn = "US Passport / Visa",
                nameHi = "यूएस पासपोर्ट / वीजा",
                widthMm = 50.8f,
                heightMm = 50.8f,
                widthPx = 600,
                heightPx = 600,
                dpi = 300,
                maxKb = 240,
                instructionsEn = "2x2 inch (600x600 px), white/off-white background, head 1-1 3/8 inch",
                instructionsHi = "2x2 इंच (600x600 पिक्सल), सफेद बैकग्राउंड"
            ),
            PassportPreset(
                id = "uk_passport",
                nameEn = "UK Passport",
                nameHi = "यूके पासपोर्ट",
                widthMm = 35f,
                heightMm = 45f,
                widthPx = 413,
                heightPx = 531,
                dpi = 300,
                instructionsEn = "35x45 mm, light grey or cream background",
                instructionsHi = "35x45 मिमी, हल्का ग्रे या क्रीम बैकग्राउंड"
            ),
            PassportPreset(
                id = "schengen_visa",
                nameEn = "Schengen Visa (Europe)",
                nameHi = "शेंगेन वीजा (यूरोप)",
                widthMm = 35f,
                heightMm = 45f,
                widthPx = 413,
                heightPx = 531,
                dpi = 300,
                instructionsEn = "35x45 mm, light grey background, head 32-36 mm",
                instructionsHi = "35x45 मिमी, हल्का ग्रे बैकग्राउंड"
            ),
            PassportPreset(
                id = "china_visa",
                nameEn = "China Visa",
                nameHi = "चीन वीजा",
                widthMm = 33f,
                heightMm = 48f,
                widthPx = 390,
                heightPx = 567,
                dpi = 300,
                instructionsEn = "33x48 mm, pure white background, head width 15-22 mm",
                instructionsHi = "33x48 मिमी, शुद्ध सफेद बैकग्राउंड"
            ),
            PassportPreset(
                id = "canada_passport",
                nameEn = "Canada Passport",
                nameHi = "कनाडा पासपोर्ट",
                widthMm = 50f,
                heightMm = 70f,
                widthPx = 591,
                heightPx = 827,
                dpi = 300,
                instructionsEn = "50x70 mm, white background, face length 31-36 mm",
                instructionsHi = "50x70 मिमी, सफेद बैकग्राउंड"
            ),
            PassportPreset(
                id = "australia_passport",
                nameEn = "Australia Passport",
                nameHi = "ऑस्ट्रेलिया पासपोर्ट",
                widthMm = 35f,
                heightMm = 45f,
                widthPx = 413,
                heightPx = 531,
                dpi = 300,
                instructionsEn = "35x45 mm, plain light grey/white background",
                instructionsHi = "35x45 मिमी, सादा हल्का बैकग्राउंड"
            ),
            PassportPreset(
                id = "gulf_visa",
                nameEn = "Gulf Countries (UAE/Saudi)",
                nameHi = "खाड़ी देश (यूएई / सऊदी)",
                widthMm = 40f,
                heightMm = 60f,
                widthPx = 472,
                heightPx = 709,
                dpi = 300,
                instructionsEn = "40x60 mm, white background",
                instructionsHi = "40x60 मिमी, सफेद बैकग्राउंड"
            ),
            PassportPreset(
                id = "gov_exam_200",
                nameEn = "Exam Form (SSC / UPSC / RRB)",
                nameHi = "सरकारी परीक्षा फॉर्म (200x230)",
                widthMm = 35f,
                heightMm = 40f,
                widthPx = 200,
                heightPx = 230,
                dpi = 150,
                maxKb = 50,
                instructionsEn = "200x230 px, file size 20KB - 50KB, optional name & date",
                instructionsHi = "200x230 पिक्सल, साइज 20KB से 50KB, नाम और तारीख पट्टी"
            ),
            PassportPreset(
                id = "exam_sign_thumb",
                nameEn = "Exam Signature / Thumb",
                nameHi = "परीक्षा हस्ताक्षर / अंगूठा",
                widthMm = 35f,
                heightMm = 20f,
                widthPx = 140,
                heightPx = 60,
                dpi = 150,
                maxKb = 20,
                instructionsEn = "140x60 px or 100x120 px, file size 10KB - 20KB",
                instructionsHi = "हस्ताक्षर / अंगूठा निशान, 10KB - 20KB"
            )
        )
    }
}
