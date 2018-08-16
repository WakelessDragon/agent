package com.rainyalley.agent.runtime;

import com.rainyalley.agent.CustomizableThreadFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MethodTracking {

    private static String delimiter = "|";

    /**
     * 追踪模式
     * 0:最小性能影响,可能有数据丢失
     * 1:最完整的数据,可能对性能造成较大影响
     */
    private static int trackingMode = 0;

    /**
     * 队列大小
     */
    private static int trackingQueueSize = 999;

    /**
     * 每n行flush一次
     */
    private static int dataFlushInternal = 1000;

    private static int dataLineNum = 0;

    /**
     * 线程池
     */
    private static ThreadPoolExecutor TPE;

    private static File workDir = Util.getConfFile().getParentFile();
    private static BufferedWriter writer;

    static{
        Object trackingMode =  Util.getConfValue("tracking-mode");
        if(trackingMode!=null && !"".equals(trackingMode)){
            MethodTracking.trackingMode = Integer.valueOf(String.valueOf(trackingMode));
        }

        Object delimiter = Util.getConfValue("tracking-data-delimiter");
        if(delimiter!=null && !"".equals(delimiter)){
            MethodTracking.delimiter = String.valueOf(delimiter);
        }

        Object trackingQueueSize = Util.getConfValue("tracking-queue-size");
        if(trackingQueueSize!=null && !"".equals(trackingQueueSize)){
            MethodTracking.trackingQueueSize = Integer.valueOf(String.valueOf(trackingQueueSize));
        }

        Object dataFlushInternal = Util.getConfValue("tracking-data-flush-internal");
        if(dataFlushInternal!=null && !"".equals(dataFlushInternal)){
            MethodTracking.dataFlushInternal = Integer.valueOf(String.valueOf(dataFlushInternal));
        }

        LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>(MethodTracking.trackingQueueSize);

        RejectedExecutionHandler handler = null;
        if(MethodTracking.trackingMode == 1){
            handler = new ThreadPoolExecutor.CallerRunsPolicy();
        } else {
            handler = new ThreadPoolExecutor.DiscardPolicy();
        }

        TPE= new ThreadPoolExecutor(
                1,
                1,
                10L,
                TimeUnit.MILLISECONDS,
                taskQueue,
                new CustomizableThreadFactory("rainyalley-methodTracking-"),
                handler);


        File dataFile = new File(workDir.getPath() + File.separator + "data.txt");
        try {
            writer = new BufferedWriter(new FileWriter(dataFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(Arrays.asList(MethodTracking.delimiter, MethodTracking.trackingMode, MethodTracking.trackingQueueSize, MethodTracking.dataFlushInternal));
    }






    public static void info(final String methodName, final long startNanoTime, final long endNanoTime, final Object result, final Object[] args){
        TPE.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.write("INFO");
                    writer.write(delimiter);
                    writer.write(methodName);
                    writer.write(delimiter);
                    writer.write(String.valueOf(startNanoTime));
                    writer.write(delimiter);
                    writer.write(String.valueOf(endNanoTime));
                    writer.write(delimiter);
                    writer.write(String.valueOf(result));
                    writer.write(delimiter);
                    writer.write(Arrays.toString(args));
                    writer.newLine();

                    dataLineNum++;
                    flushIfRequire();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public static void error(final String methodName, final long startNanoTime, final long endNanoTime, final Throwable ex, final Object[] args){
        TPE.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.write("ERROR");
                    writer.write(delimiter);
                    writer.write(methodName);
                    writer.write(delimiter);
                    writer.write(String.valueOf(startNanoTime));
                    writer.write(delimiter);
                    writer.write(String.valueOf(endNanoTime));
                    writer.write(delimiter);
                    writer.write(ex.getMessage().replace("\n", "\\n"));
                    writer.write(delimiter);
                    writer.write(Arrays.toString(args));
                    writer.newLine();
                    dataLineNum++;

                    flushIfRequire();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void flushIfRequire() throws IOException{
        if(dataLineNum % dataFlushInternal == 0){
            writer.flush();
        }
    }
}
