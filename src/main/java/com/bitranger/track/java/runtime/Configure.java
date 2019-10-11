package com.bitranger.track.java.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Properties;

public class Configure {

    private static final String javaAgentPrefix = "-javaagent:";

    public static File getConfFile(){
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        for (String arg : arguments) {
            if (!arg.startsWith(javaAgentPrefix)) {
                continue;
            }

            String agentJarPath = arg.substring(javaAgentPrefix.length());
            String currDir = agentJarPath.substring(0, agentJarPath.lastIndexOf(File.separator));
            String agentConfPath = currDir + File.separator + "bitranger-java-track.properties";
            File confFile = new File(agentConfPath);
            if(confFile.exists()){
                return confFile;
            }
        }
        return null;
    }

    public static Object getConfValue(String key){
        File confFile = getConfFile();
        if(confFile == null){
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(confFile));
            Properties properties = new Properties();

            properties.load(reader);

            return properties.get(key);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
