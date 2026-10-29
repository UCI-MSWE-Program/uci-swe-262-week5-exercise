import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Lists every class in a jar file (sorted alphabetically) and reports, for each
 * class, the number of declared public, private, protected, and static methods
 * as well as the number of declared fields.
 */
public class JarClasses {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java JarClasses <jar-file>");
            System.exit(1);
        }

        File jarFile = new File(args[0]);
        if (!jarFile.isFile()) {
            System.err.println("Not a file: " + jarFile);
            System.exit(1);
        }

        // Collect the fully-qualified names of every class in the jar.
        List<String> classNames = new ArrayList<>();
        try (JarFile jarArchive = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jarArchive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (!entryName.endsWith(".class")) {
                    continue;
                }
                // Skip multi-release copies and any other metadata classes.
                if (entryName.startsWith("META-INF/")) {
                    continue;
                }
                if (entryName.endsWith("module-info.class")
                        || entryName.endsWith("package-info.class")) {
                    continue;
                }

                String className = entryName
                        .substring(0, entryName.length() - ".class".length())
                        .replace('/', '.');
                classNames.add(className);
            }
        }

        Collections.sort(classNames);

        // Load the jar so reflection can inspect the classes inside it.
        URL[] jarUrls = { jarFile.toURI().toURL() };
        try (URLClassLoader loader =
                new URLClassLoader(jarUrls, ClassLoader.getPlatformClassLoader())) {
            for (String className : classNames) {
                try {
                    System.out.println(describe(loader.loadClass(className)));
                } catch (ClassNotFoundException | LinkageError | SecurityException exception) {
                    // Silently skip classes that cannot be loaded.
                }
            }
        }
    }

    private static String describe(Class<?> loadedClass) {
        int publicCount = 0;
        int privateCount = 0;
        int protectedCount = 0;
        int staticCount = 0;
        for (Method method : loadedClass.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers)) {
                publicCount++;
            }
            if (Modifier.isPrivate(modifiers)) {
                privateCount++;
            }
            if (Modifier.isProtected(modifiers)) {
                protectedCount++;
            }
            if (Modifier.isStatic(modifiers)) {
                staticCount++;
            }
        }
        int fieldCount = loadedClass.getDeclaredFields().length;
        return String.format(
                "----------%s----------%n"
                        + "  Public methods: %d%n"
                        + "  Private methods: %d%n"
                        + "  Protected methods: %d%n"
                        + "  Static methods: %d%n"
                        + "  Fields: %d",
                loadedClass.getName(), publicCount, privateCount, protectedCount, staticCount,
                fieldCount);
    }
}
