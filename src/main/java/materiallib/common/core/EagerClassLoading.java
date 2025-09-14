package materiallib.common.core;

import net.minecraft.launchwrapper.Launch;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModClassLoader;
import cpw.mods.fml.common.discovery.ASMDataTable;
import materiallib.MaterialLib;
import materiallib.api.annotations.LoadedEagerly;

public class EagerClassLoading {

    public static void loadClassesEagerly() {
        final boolean jarjar = Launch.blackboard.getOrDefault("jarjar.rfbPluginLoaded", Boolean.FALSE) == Boolean.TRUE;

        if (!jarjar) {
            // Do the old style class discovery
            EagerLoadedClassDiscoverer.loadClasses();
        } else {
            // If JarJar's loaded, the ASMDataTable now includes interfaces in addition to Annotations, so use that
            // instead Includes a small change in behavior of no longer requiring the class to start with NEI and end
            // with Config; This could easily be changed if desired, but the previous behavior was likely to speed
            // things up by limiting the scope when looking at all of the jars and classes in the classpath.
            final ASMDataTable dataTable = MaterialLib.proxy.getAsmDataTable();

            final ModClassLoader modClassLoader = (ModClassLoader) Loader.instance().getModClassLoader();

            for (ASMDataTable.ASMData asmData : dataTable.getAll(LoadedEagerly.class.getName().replace('.', '/'))) {
                final String className = asmData.getClassName().replace('/', '.');

                try {
                    Class.forName(className, true, modClassLoader);
                    MaterialLib.LOG.info("Loaded class eagerly: {}", className);
                } catch (ClassNotFoundException e) {
                    MaterialLib.LOG.error("Failed to eagerly load class {}", className, e);
                }
            }
        }
    }
}
