package com.newtranx.eval.enum

/**
 * @Author: anson
 * @Date: 2022/1/30 4:55 PM
 */
enum class Language(
    val code: String,
    val value: String
) {
    EN("en", "English"), FR("fr", "French"),
    DE("de", "German"), ES("es", "Spanish"),
    ZH("zh", "Chinese"), JA("ja", "Japanese"),
    KO("ko", "Korean"), UNSPECIFIED("Unspecified", "Unspecified")
}