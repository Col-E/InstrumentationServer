package software.coley.instrument.util;

public class JavaVersion {

    private static int majorVersion = -1;

    static {
        String javaVersion = System.getProperty("java.version");
        // parse a Java version string to an integer of major version like 7, 8, 9, 10, ...
        // javaVersion should be something like "1.7.0_25"
        String[] version = javaVersion.split("\\.");
        if (version.length > 2) {
            majorVersion = Integer.parseInt(version[0]);
            if (majorVersion == 1) {
                majorVersion = Integer.parseInt(version[1]);
            }
        }
        try {
            majorVersion = Integer.parseInt(javaVersion);
        } catch (NumberFormatException e) {
            // ignore
        }
    }

    public static int getMajorVersion() {
        return majorVersion;
    }
}