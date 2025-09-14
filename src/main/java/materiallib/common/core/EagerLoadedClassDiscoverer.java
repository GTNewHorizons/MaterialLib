package materiallib.common.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import com.google.common.collect.ImmutableList;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModClassLoader;
import cpw.mods.fml.relauncher.CoreModManager;
import materiallib.MaterialLib;
import materiallib.api.annotations.LoadedEagerly;

@SuppressWarnings("unused")
public class EagerLoadedClassDiscoverer {

    private static final ModClassLoader modClassLoader = (ModClassLoader) Loader.instance().getModClassLoader();

    private static final String EAGER_LOADING = "L" + LoadedEagerly.class.getName().replace('.', '/') + ";";

    public static void loadClasses() {
        findClasspathMods();
    }

    private static void checkAddClass(String resource) {
        try {
            String className = resource.replace(".class", "").replace("\\", ".").replace("/", ".");

            byte[] bytes = Launch.classLoader.getClassBytes(className);
            if (bytes == null) return;

            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(bytes);
            reader.accept(node, ClassReader.SKIP_CODE);

            AnnotationNode annotation = null;

            if (node.visibleAnnotations != null) {
                for (var a : node.visibleAnnotations) {
                    if (a.desc.equals(EAGER_LOADING)) {
                        annotation = a;
                        break;
                    }
                }
            }

            if (annotation == null && node.invisibleAnnotations != null) {
                for (var a : node.invisibleAnnotations) {
                    if (a.desc.equals(EAGER_LOADING)) {
                        annotation = a;
                        break;
                    }
                }
            }

            if (annotation != null) {
                loadClass(className);
            }
        } catch (IOException e) {
            MaterialLib.LOG.error("Unable to load class: {}", resource, e);
        }
    }

    private static void loadClass(String className) {
        try {
            Class.forName(className, true, modClassLoader);
            MaterialLib.LOG.info("Loaded class eagerly: {}", className);
        } catch (Throwable t) {
            MaterialLib.LOG.error("Unable to load class: {}", className, t);
        }
    }

    private static void findClasspathMods() {
        List<String> knownLibraries = ImmutableList.<String>builder().addAll(modClassLoader.getDefaultLibraries())
            .addAll(CoreModManager.getLoadedCoremods()).build();

        File[] minecraftSources = modClassLoader.getParentSources();
        HashSet<String> searchedSources = new HashSet<>();

        for (File minecraftSource : minecraftSources) {
            if (!searchedSources.add(minecraftSource.getAbsolutePath())) continue;

            if (minecraftSource.isFile()) {
                if (!knownLibraries.contains(minecraftSource.getName())) {
                    FMLLog.fine(
                        "Found a minecraft related file at %s, checking for eagerly loaded classes",
                        minecraftSource.getAbsolutePath());
                    try {
                        readFromZipFile(minecraftSource);
                    } catch (Exception e) {
                        MaterialLib.LOG.error(
                            "Failed to scan {}, the zip file is invalid",
                            minecraftSource.getAbsolutePath(),
                            e);
                    }
                }
            } else if (minecraftSource.isDirectory()) {
                FMLLog.fine(
                    "Found a minecraft related directory at %s, checking for eagerly loaded classes",
                    minecraftSource.getAbsolutePath());

                Path root = minecraftSource.toPath();

                try (Stream<Path> paths = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
                    paths.forEach(path -> {
                        if (Files.isRegularFile(path)) {
                            checkAddClass(root.relativize(path).toString());
                        }
                    });
                } catch (IOException e) {
                    MaterialLib.LOG.error("Failed to scan {}", root, e);
                }
            }
        }
    }

    private static void readFromZipFile(File file) throws IOException {
        try (ZipFile zipFile = new ZipFile(file))
        {
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

            while (zipEntries.hasMoreElements())
            {
                ZipEntry zipentry = zipEntries.nextElement();

                if (zipentry.isDirectory()) continue;

                String fullname = zipentry.getName().replace('\\', '/');

                int pos = fullname.lastIndexOf('/');

                String name = pos == -1 ? fullname : fullname.substring(pos + 1);

                checkAddClass(fullname);
            }
        }
    }
}
