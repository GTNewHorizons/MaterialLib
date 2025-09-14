package materiallib.test;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import materiallib.MaterialLib;
import materiallib.api.enums.OrePrefix;
import materiallib.api.annotations.LoadedEagerly;
import materiallib.api.items.StandardMaterialMetaItem;
import materiallib.api.material.IMaterial;
import materiallib.api.material.metadata.MaterialColorMetaKey;
import materiallib.api.rendering.IItemTexture;
import materiallib.api.util.ImmutableColor;
import materiallib.client.rendering.ItemTexture;

@LoadedEagerly
public class TestMetaItem extends StandardMaterialMetaItem<TestMaterial> {

    private IIcon gear, ingot;

    public static final TestMetaItem INSTANCE = new TestMetaItem();

    public TestMetaItem() {
        super("testmetaitem", Arrays.asList(OrePrefix.gearGt, OrePrefix.ingot), TestRepository.INSTANCE, 0);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);

        gear = register.registerIcon(MaterialLib.MODID + ":" + "gearGt");
        ingot = register.registerIcon(MaterialLib.MODID + ":" + "ingot");
    }

    @Override
    protected IItemTexture[] getTextures(IMaterial material, OrePrefix prefix) {
        IIcon icon = switch (prefix) {
            case gearGt -> gear;
            case ingot -> ingot;
            default -> null;
        };

        ImmutableColor color = material.getMeta(MaterialColorMetaKey.INSTANCE);

        return new IItemTexture[] {
            new ItemTexture(ignored -> icon, ignored -> color),
        };
    }
}
