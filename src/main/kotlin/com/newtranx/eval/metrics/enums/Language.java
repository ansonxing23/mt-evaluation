package com.newtranx.eval.metrics.enums;

/**
 * @Author Anson
 * @Date 2022/2/24 11:01
 * @Version 1.0
 */
public enum Language {
    EN("en", "English"), FR("fr", "French"),
    DE("de", "German"), ES("es", "Spanish"),
    ZH("zh", "Chinese"), JA("ja", "Japanese"),
    KO("ko", "Korean"), UNSPECIFIED("Unspecified", "Unspecified");

    private String code;
    private String name;
    Language(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
