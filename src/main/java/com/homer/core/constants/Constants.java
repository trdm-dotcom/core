package com.homer.core.constants;

public class Constants {
    public static String REDIS_KEY_POST = "cache_post";
    public static String REDIS_KEY_WATCHLIST = "cache_watchlist";
    public static String REDIS_KEY_USERINFO = "cache_userinfo";
    public static String REDIS_KEY_TOKEN = "cache_token";
    public static Integer DEFAULT_FETCH_COUNT = 20;
    public static Integer DEFAULT_OFFSET = 0;
    public static String OBJECT_NOT_FOUND = "OBJECT_NOT_FOUND";
    public static String USER_HADNT_BEEN_VERIFIED = "USER_HADNT_BEEN_VERIFIED";
    public static String FROM_TIME_MUST_BE_BEFORE_OR_EQUAL_TO_TO_TIME = "FROM_TIME_MUST_BE_BEFORE_OR_EQUAL_TO_TO_TIME";
    public static String FROM_TIME_MUST_BE_AFTER_OR_EQUAL_TO_CURRENT_TIME = "FROM_TIME_MUST_BE_BEFORE_OR_EQUAL_TO_CURRENT_TIME";
    public static String TO_DATE_MUST_BE_AFTER_OR_EQUAL_TO_CURRENT_TIME = "TO_DATE_MUST_BE_AFTER_OR_EQUAL_TO_CURRENT_TIME";
    public static String CREATE_FAILED = "CREATE_FAILED";
    public static String MODIFY_FAILED = "MODIFY_FAILED";
    public static String DELETE_FAILED = "DELETE_FAILED";
    public static String REJECT_FAILED = "REJECT_FAILED";
    public static String CREATE_SUCCESS = "CREATE_SUCCESS";
    public static String MODIFY_SUCCESS = "MODIFY_SUCCESS";
    public static String DELETE_SUCCESS = "DELETE_SUCCESS";
    public static String REJECT_SUCCESS = "REJECT_SUCCESS";
    public static String ALREADY_EXISTS = "ALREADY_EXISTS";
}
