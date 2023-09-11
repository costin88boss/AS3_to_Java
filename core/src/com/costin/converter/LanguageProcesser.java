package com.costin.converter;

import java.util.ArrayList;

public class LanguageProcesser {

    private static String s;

    public static ArrayList<String> process(String as) {
        // warning: this program assumes that the scripts to be converted are not broken.
        s = as;
        return Lexer.process(s);
    }

    public static String postProcess(String java) {
        s = java;
        return Lexer.postProcess(s);
    }
}
