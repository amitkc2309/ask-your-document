package com.ayd.utils;

public class GenericUtils {
    @Deprecated
    public static String getConversationId(String username, String chatSessionId){
        return username + ":" + chatSessionId;
    }
}
