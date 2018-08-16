package com.rainyalley.agent.runtime;

import java.util.Arrays;

public class MethodTracking {

    public static void info(String methodName, long startNanoTime, long endNanoTime, Object result, Object[] args){
        System.out.println(String.format("trcking %s %s %s %s %s", methodName, startNanoTime, endNanoTime, result, Arrays.toString(args)));
    }

    public static void error(String methodName, long startNanoTime, long endNanoTime, Throwable ex, Object[] args){
        System.out.println(String.format("trcking %s %s %s %s %s", methodName, startNanoTime, endNanoTime, ex, Arrays.toString(args)));
    }
}
