package com.bitranger.track.java;
import com.bitranger.track.java.transformer.ClassTrackingTransformer;

import java.lang.instrument.Instrumentation;

/**
 * @author bin.zhang
 */
public class Agent {

    public static void premain(String args, Instrumentation instrumentation){
        ClassTrackingTransformer transformer = new ClassTrackingTransformer();
        transformer.init();
        instrumentation.addTransformer(transformer);
    }
}
