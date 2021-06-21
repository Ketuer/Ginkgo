package net.ginkgo.server.core;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ScanUtil {
    /**
     * 从包package中获取所有的Class
     *
     * @param pack 包名称
     * @param recursive 是否获取子级包下类
     *
     * @return 类集合
     */
    public static Set<Class<?>> getClasses(String pack, boolean recursive) {
        String packageDirName = pack.replace('.', '/');
        try {
            Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    return findAndAddClassesInPackageByFile(pack, filePath, recursive);
                } else if ("jar".equals(protocol)) {
                    return findAndAddClassesInPackageByJar(packageDirName, url, recursive);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }

    /**
     * 获取Jar对应包内的所有类
     * @param packageDirName 包路径名称
     * @param url 路径
     * @param recursive 是否递归查找子包下的类
     *
     * @return classes
     */
    private static Set<Class<?>> findAndAddClassesInPackageByJar(String packageDirName, URL url, boolean recursive){
        Set<Class<?>> classes = new LinkedHashSet<>();
        try (JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile()){
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("/")) name = name.substring(1);
                if(name.startsWith(packageDirName) && name.endsWith(".class") && !entry.isDirectory()){
                    String path = name.substring(0, name.length() - 6).replace("/", ".");
                    if(recursive){
                        classes.add(Class.forName(path));
                    }else {
                        if(!name.substring(Math.min(packageDirName.length() + 1, name.length())).contains("/")){
                            classes.add(Class.forName(path));
                        }
                    }
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName 包名称
     * @param packagePath 包路径名称
     * @param recursive 是否递归查找子包下的类
     *
     * @return classes
     */
    public static Set<Class<?>> findAndAddClassesInPackageByFile(String packageName, String packagePath, boolean recursive) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) return classes;
        File[] dirFiles = dir.listFiles(file -> (recursive && file.isDirectory()) || (file.getName().endsWith(".class")));
        if(dirFiles == null) return classes;
        for (File file : dirFiles) {
            if (file.isDirectory()) {
                classes.addAll(findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive));
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return classes;
    }
}
