package ludo.lwjgl3;

import java.io.File;
import java.util.ArrayList;

public class StartupHelper {
    public static boolean startNewJvmIfRequired() {
        // Check if we're running on Mac/JVM 9+ with the required arguments
        String[] jvmArgs = getJvmArgs();
        if (jvmArgs != null) {
            try {
                String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                String classpath = System.getProperty("java.class.path");
                String className = Lwjgl3Launcher.class.getName();

                ArrayList<String> command = new ArrayList<>();
                command.add(javaBin);
                command.addAll(java.util.Arrays.asList(jvmArgs));
                command.add("-cp");
                command.add(classpath);
                command.add(className);

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                builder.redirectError(ProcessBuilder.Redirect.INHERIT);
                builder.start();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static String[] getJvmArgs() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("mac")) return null;

        if (!System.getProperty("java.version").startsWith("1.")) {
            return new String[] {
                "-XstartOnFirstThread",
                "-Djava.awt.headless=true"
            };
        }
        return null;
    }
}
