package com.bitranger.track.java.transformer;

import com.bitranger.track.java.runtime.Configure;
import com.bitranger.track.java.utils.WarExploder;
import javassist.*;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author bin.zhang
 */
public class ClassTrackingTransformer implements ClassFileTransformer {

    private static final String DELIMITER = ";";

    private ClassPool pool = ClassPool.getDefault();

    private List<String> classPathList = new ArrayList<String>();
    private List<String> trackingPackageList = new ArrayList<String>();


    public void init(){
        try {
            File agentConfFile = Configure.getConfFile();
            if(agentConfFile == null){
                return;
            }
            Object classpathListStr = Configure.getConfValue("classpath");
            if(classpathListStr != null && classpathListStr.toString().length() > 0){
                classPathList.addAll(Arrays.asList(classpathListStr.toString().split(DELIMITER)));
            }
            Object trackingPackageListStr = Configure.getConfValue("tracking-package");
            if(trackingPackageListStr != null && trackingPackageListStr.toString().length() > 0){
                trackingPackageList.addAll(Arrays.asList(trackingPackageListStr.toString().split(DELIMITER)));
            }

            for (String classpath : classPathList) {
                if(classpath.endsWith(".war")){
                    System.out.println(String.format("ClassTrackingTransformer starting to explode %s", classpath));
                    WarExploder we = new WarExploder(classpath);
                    we.explode();
                    File dir = we.getExplodedFile();
                    System.out.println(String.format("ClassTrackingTransformer exploded to %s", dir.getAbsolutePath()));
                    File clses = locateFile(dir, "WEB-INF", "classes");
                    pool.appendClassPath(clses.getAbsolutePath());
                    File lib = locateFile(dir, "WEB-INF", "lib");
                    File[] jarList = lib.listFiles();
                    for (File jar : jarList) {
                        pool.appendClassPath(jar.getAbsolutePath());
                    }
                } else {
                    pool.appendClassPath(classpath);
                }
            }
            pool.importPackage("com.bitranger.track.java.runtime");

            System.out.println("ClassTrackingTransformer " + this);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        if(trackingPackageList.isEmpty()){
            return classfileBuffer;
        }

        className = className.replace("/", ".");
        boolean inPackageList = false;
        for (String agentPackage : trackingPackageList) {
            if(className.startsWith(agentPackage)){
                inPackageList = true;
                break;
            }
        }

        if(!inPackageList){
            return classfileBuffer;
        }


        try {
            CtClass ctclass = pool.getCtClass(className);

            if(ctclass.isInterface()){
                return classfileBuffer;
            }

            ctclass = resolveConstructor(ctclass);

            ctclass = resolveMethods(ctclass);

            return ctclass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
            return classfileBuffer;
        }
    }

    private CtClass resolveMethods(CtClass ctclass) throws CannotCompileException, NotFoundException {
        CtMethod[] declaredMethods = ctclass.getDeclaredMethods();

        for (CtMethod method : declaredMethods) {
            if (method.isEmpty()){
                continue;
            }
            String methodNameOriginal = method.getName();
            String methodNameImpl = method.getName()  + "$origin";
            method.setName(methodNameImpl);
            CtMethod generatedMethod = CtNewMethod.copy(method, methodNameOriginal, ctclass, (ClassMap)null);
            String returnType = method.getReturnType().getName();
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("long startTime = System.nanoTime();\n");
            sb.append("try{\n");

            if (!"void".equals(returnType)) {
                sb.append(returnType).append(" result = ");
            }
            sb.append(methodNameImpl).append("($$);\n");

            sb.append("long endTime = System.nanoTime();\n");

            if (!"void".equals(returnType)) {
                sb.append(String.format("com.bitranger.track.java.runtime.MethodTracking.getInstance().info(\"%s\", startTime, endTime, result, $args);", generatedMethod.getLongName()));
                sb.append("return result;\n");
            } else {
                sb.append(String.format("com.bitranger.track.java.runtime.MethodTracking.getInstance().info(\"%s\", startTime, endTime, \"void\", $args);", generatedMethod.getLongName()));
            }

            sb.append("} catch (Throwable ex){\n");
            sb.append("long endTime = System.nanoTime();\n");
            sb.append(String.format("com.bitranger.track.java.runtime.MethodTracking.getInstance().error(\"%s\", startTime, endTime, ex, $args);", generatedMethod.getLongName()));
            sb.append("throw ex;");
            sb.append("}");
            sb.append("}");
            generatedMethod.setBody(sb.toString());
            ctclass.addMethod(generatedMethod);

            System.out.println("ClassTrackingTransformer transformed " + generatedMethod.getLongName());
        }

        return ctclass;
    }

    private CtClass resolveConstructor(CtClass ctclass){
        return ctclass;
    }

    private static File locateFile(File dir, String... paths) {
        File cur = dir;
        outter: for (String p : paths) {
            File[] all = cur.listFiles();
            for (File f : all) {
                if (p.equals(f.getName())) {
                    cur = f;
                    continue outter;
                }
            }
            throw new RuntimeException("No path named '" + p + "' found in file: " + cur.getAbsolutePath());
        }
        return cur;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"@class\":\"com.bitranger.track.java.ClassTrackingTransformer\",");
        sb.append("\"@super\":\"").append(super.toString()).append("\",");
        sb.append("\"classPathList\":\"")
                .append(classPathList)
                .append("\"");
        sb.append(",\"trackingPackageList\":\"")
                .append(trackingPackageList)
                .append("\"");
        sb.append("}");
        return sb.toString();
    }
}
