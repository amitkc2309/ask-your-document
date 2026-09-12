package com.ayd.utils;

public class GenericUtils {
    @Deprecated
    public static String getConversationId(String userId, String chatSessionId){
        return userId + ":" + chatSessionId;
    }
}
