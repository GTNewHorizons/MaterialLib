package materiallib;

import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import materiallib.api.lifecycle.MaterialLifecycles;
import materiallib.api.lifecycle.events.RegisterObjectsEvent;
import materiallib.api.lifecycle.events.RegisterOreDictEvent;
import materiallib.api.lifecycle.events.RegisterRecipesEvent;
import materiallib.api.material.IMaterial;
import materiallib.api.repository.IMaterialRepository;
import materiallib.common.core.EagerClassLoading;
import materiallib.test.TestMaterialList;

public class CommonProxy {

    private ASMDataTable asmDataTable;

    public void preInit(FMLPreInitializationEvent event) {
        asmDataTable = event.getAsmData();
        EagerClassLoading.loadClassesEagerly();

        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
    }

    public void init(FMLInitializationEvent event) {
        MaterialLifecycles.invoke(RegisterObjectsEvent.INSTANCE);
        MaterialLifecycles.invoke(RegisterOreDictEvent.INSTANCE);
    }

    public void postInit(FMLPostInitializationEvent event) {
        for (IMaterialRepository<?> repo : IMaterialRepository.REPOSITORIES.values()) {
            for (IMaterial material : repo.getMaterials()) {
                MaterialLifecycles.invoke(new RegisterRecipesEvent(repo, material));
            }
        }
    }

    public void complete(FMLLoadCompleteEvent event) {

    }

    public void serverStarting(FMLServerStartingEvent event) {}

    public ASMDataTable getAsmDataTable() {
        return asmDataTable;
    }
}
