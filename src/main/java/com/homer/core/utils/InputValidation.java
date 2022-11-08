package com.homer.core.utils;

import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.constants.Constants;
import com.homer.core.model.FeedbackType;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;

import java.text.Normalizer;
import java.time.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class InputValidation {
    private static final String BANK_NAME_REGEX = "^[A-Z\\s]{3,}$";
    private static final String BANK_NUMBER_REGEX = "^\\d{8,17}$";

    public static boolean isBankNameValid(String bankName, boolean isNullable) {
        if (bankName == null && isNullable) {
            return true;
        } else if (bankName == null) {
            return false;
        }
        Pattern pattern = Pattern.compile(BANK_NAME_REGEX);
        Matcher matcher = pattern.matcher(bankName);
        return matcher.matches();
    }

    public static boolean isBankNumberValid(String bankNumber, boolean isNullable) {
        if (bankNumber == null && isNullable) {
            return true;
        } else if (bankNumber == null) {
            return false;
        }
        Pattern pattern = Pattern.compile(BANK_NUMBER_REGEX);
        Matcher matcher = pattern.matcher(bankNumber);
        return matcher.matches();
    }

    public static String removeAccent(String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }
        str = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern normalPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        str = normalPattern
                .matcher(str)
                .replaceAll("");

        Pattern dLowerPattern = Pattern.compile("đ");
        str = dLowerPattern
                .matcher(str)
                .replaceAll("d");

        Pattern dUpperPattern = Pattern.compile("Đ");
        return dUpperPattern
                .matcher(str)
                .replaceAll("D");
    }
}
