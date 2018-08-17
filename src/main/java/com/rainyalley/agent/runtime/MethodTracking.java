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

    private static MethodTracking instance = new MethodTracking();

    /**
     * 分隔符
     */
    private  String delimiter = "|";

    private String leftQuote = "";

    private String rightQuote = "";

    /**
     * 运行模式
     * 0:最小性能影响,可能有数据丢失
     * 1:最完整的数据,可能对性能造成较大影响
     */
    private  int trackingMode = 0;

    /**
     * 队列大小
     */
    private  int queueSize = 999;

    /**
     * 每n行flush一次
     */
    private  int flushInternal = 1000;

    /**
     * 每行数据的字节数
     */
    private int bytesPerLine = 500;

    /**
     * 线程池
     */
    private  ThreadPoolExecutor tpe;

    /**
     * 数据writer
     */
    private  BufferedWriter dataWriter;

    /**
     * 已记录的数据行数
     */
    private  int dataLineNum = 0;


    public MethodTracking() {

        Object trackingModeStr =  Util.getConfValue("tracking-mode");
        if(trackingModeStr!=null && !"".equals(trackingModeStr)){
            trackingMode = Integer.valueOf(String.valueOf(trackingModeStr));
        }

        Object trackingDataDelimiterStr = Util.getConfValue("tracking-data-delimiter");
        if(trackingDataDelimiterStr!=null && !"".equals(trackingDataDelimiterStr)){
            delimiter = String.valueOf(trackingDataDelimiterStr);
        }

        Object leftQuoteStr = Util.getConfValue("tracking-data-left-quote");
        if(leftQuoteStr!=null && !"".equals(leftQuoteStr)){
            leftQuote = String.valueOf(leftQuoteStr);
        }
        
        Object rightQuoteStr = Util.getConfValue("tracking-data-right-quote");
        if(rightQuoteStr!=null && !"".equals(rightQuoteStr)){
            rightQuote = String.valueOf(rightQuoteStr);
        }

        Object trackingQueueSizeStr = Util.getConfValue("tracking-queue-size");
        if(trackingQueueSizeStr!=null && !"".equals(trackingQueueSizeStr)){
            queueSize = Integer.valueOf(String.valueOf(trackingQueueSizeStr));
        }

        Object trackingDataFlushInternalStr = Util.getConfValue("tracking-data-flush-internal");
        if(trackingDataFlushInternalStr!=null && !"".equals(trackingDataFlushInternalStr)){
            flushInternal = Integer.valueOf(String.valueOf(trackingDataFlushInternalStr));
        }

        LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>(queueSize);

        RejectedExecutionHandler rejectedExecutionHandler = null;
        if(trackingMode == 1){
            rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        } else {
            rejectedExecutionHandler = new ThreadPoolExecutor.DiscardPolicy();
        }

        tpe = new ThreadPoolExecutor(
                1,
                1,
                10L,
                TimeUnit.MILLISECONDS,
                taskQueue,
                new CustomizableThreadFactory("rainyalley-methodTracking-"),
                rejectedExecutionHandler);


        File workDir = Util.getConfFile().getParentFile();
        File dataFile = new File(workDir.getPath() + File.separator + "data.txt");
        try {
            int bufferSize = bytesPerLine * flushInternal;
            dataWriter = new BufferedWriter(new FileWriter(dataFile), bufferSize);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    dataWriter.close();
                    System.out.println("MethodTracking shutting down");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println(this);
    }

    public  void info(final String methodName, final long startNanoTime, final long endNanoTime, final Object result, final Object[] args){
        final String currThreadName = Thread.currentThread().getName();
        tpe.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    dataWriter.write(leftQuote);
                    dataWriter.write("INFO");
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(String.valueOf(startNanoTime));
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(String.valueOf(endNanoTime));
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(String.valueOf(currThreadName));
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(methodName);
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(replaceLineBreak(Arrays.toString(args)));
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(replaceLineBreak(String.valueOf(result)));
                    dataWriter.write(rightQuote);
                    dataWriter.newLine();
                    dataLineNum++;
                    flushIfRequire();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public  void error(final String methodName, final long startNanoTime, final long endNanoTime, final Throwable ex, final Object[] args){
        final String currThreadName = Thread.currentThread().getName();
        tpe.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    dataWriter.write(leftQuote);
                    dataWriter.write("ERROR");
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(String.valueOf(startNanoTime));
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(String.valueOf(endNanoTime));
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(String.valueOf(currThreadName));
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(methodName);
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(replaceLineBreak(Arrays.toString(args)));
                    dataWriter.write(rightQuote);
                    dataWriter.write(delimiter);
                    dataWriter.write(leftQuote);
                    dataWriter.write(replaceLineBreak(ex.getMessage()));
                    dataWriter.write(rightQuote);
                    dataWriter.newLine();
                    dataLineNum++;
                    flushIfRequire();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private  void flushIfRequire() throws IOException{
        if(dataLineNum % flushInternal == 0){
            dataWriter.flush();
        }
    }

    private String replaceLineBreak(String text){
        if(text == null){
            return "null";
        }
        return text.replace("\n", "\\n");
    }

    public static MethodTracking getInstance(){
        return instance;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"@class\":\"com.rainyalley.agent.runtime.MethodTracking\",");
        sb.append("\"@super\":\"").append(super.toString()).append("\",");
        sb.append("\"delimiter\":\"")
                .append(delimiter)
                .append("\"");
        sb.append(",\"trackingMode\":\"")
                .append(trackingMode)
                .append("\"");
        sb.append(",\"queueSize\":\"")
                .append(queueSize)
                .append("\"");
        sb.append(",\"flushInternal\":\"")
                .append(flushInternal)
                .append("\"");
        sb.append(",\"dataLineNum\":\"")
                .append(dataLineNum)
                .append("\"");
        sb.append(",\"tpe\":\"")
                .append(tpe)
                .append("\"");
        sb.append("}");
        return sb.toString();
    }
}
