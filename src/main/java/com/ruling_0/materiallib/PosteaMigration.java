package com.ruling_0.materiallib;

import static net.minecraftforge.common.util.Constants.NBT.TAG_STRING;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.ChunkTransformContext;
import com.gtnewhorizons.postea.api.IDExtenderCompat;
import com.gtnewhorizons.postea.api.IVersionedTransformer;
import com.gtnewhorizons.postea.api.VersionedReplacementManager;
import com.gtnewhorizons.postea.api.VersionedStackTransformer;
import com.ruling_0.materiallib.api.MaterialIdTransitions;
import com.ruling_0.materiallib.api.OverrideSubstitutions;
import com.ruling_0.materiallib.api.ShapeBlock;
import com.ruling_0.materiallib.api.ShapeItem;
import com.ruling_0.materiallib.api.ShapeRegistry;
import com.ruling_0.materiallib.api.WorldMaterialIds;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/// Remaps shape block metadata and shape stack damage in saved data from the material id list version it was saved
/// under to the current one, through the world's [MaterialIdTransitions] chain. Blocks and stacks of a pair served by
/// a shape override then become the overriding item through [OverrideSubstitutions]. Covers chunks, player data and
/// mods' own world storage.
///
/// Data Postea never stamped is treated as list version 1. Loading data the chain cannot bring to the current version
/// fails.
///
/// Shape ids are cached until the next FML id remap, which can renumber them (see [#invalidateIds]).
public final class PosteaMigration extends VersionedStackTransformer {

    static final String KEY = MaterialLib.MODID + ":idList";

    private static volatile IntSet blockIds;
    private static volatile IntSet itemIds;
    private static volatile Set<String> itemNames;

    private PosteaMigration() {}

    /// Registers the transformer with Postea. Call in postInit, once shapes resolve. The system property
    /// `materiallib.qa.withoutPosteaMigration` skips registration, to test saves made without the transformer.
    public static void register() {
        if (Boolean.getBoolean("materiallib.qa.withoutPosteaMigration")) return;
        VersionedReplacementManager.register(new PosteaMigration());
    }

    /// Drops the cached shape ids and names, and the registry's override substitutions.
    public static void invalidateIds() {
        blockIds = null;
        itemIds = null;
        itemNames = null;
        ShapeRegistry.instance().invalidateOverrideSubstitutions();
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public int currentVersion() {
        return WorldMaterialIds.currentListVersion();
    }

    @Override
    public void transformChunk(ChunkTransformContext ctx) {
        Int2IntMap remap = remap(WorldMaterialIds.transitions(), storedOrBaseline(ctx.storedVersion()),
            currentVersion());
        OverrideSubstitutions substitutions = ShapeRegistry.instance().overrideSubstitutions();
        if (!remap.isEmpty() || !substitutions.isEmpty()) {
            IntSet blocks = blockIds();
            ctx.forEachBlock((x, y, z, id, meta) -> {
                if (!blocks.contains(id)) return;
                int result = remap.getOrDefault(meta, meta);
                if (result == MaterialIdTransitions.DELETE) {
                    ctx.setBlock(x, y, z, 0, 0);
                    return;
                }
                long substituted = substitutions.substituteBlock(id, result);
                if (substituted >= 0) {
                    ctx.setBlock(
                        x,
                        y,
                        z,
                        OverrideSubstitutions.blockId(substituted),
                        OverrideSubstitutions.metadata(substituted));
                }
                else if (result != meta) {
                    ctx.setBlock(x, y, z, id, result);
                }
            });
        }
        super.transformChunk(ctx);
    }

    @Override
    protected Consumer<NBTTagCompound> stackTransform(int storedVersion) {
        Int2IntMap remap = remap(WorldMaterialIds.transitions(), storedOrBaseline(storedVersion), currentVersion());
        OverrideSubstitutions substitutions = ShapeRegistry.instance().overrideSubstitutions();
        if (remap.isEmpty() && substitutions.isEmpty()) return null;
        IntSet items = itemIds();
        Set<String> names = itemNames();
        return stack -> {
            remapStack(stack, remap, items, names);
            substitutions.substituteStack(stack);
        };
    }

    /// Returns the list version to migrate from: `stored`, or 1 when it is [IVersionedTransformer#UNSTAMPED].
    static int storedOrBaseline(int stored) {
        return stored == IVersionedTransformer.UNSTAMPED ? 1 : stored;
    }

    /// Returns the remap from `from` to `to`, empty when they are equal. Throws [IllegalStateException] when
    /// `transitions` cannot compose the span.
    static Int2IntMap remap(MaterialIdTransitions transitions, int from, int to) {
        if (from == to) return Int2IntMaps.EMPTY_MAP;
        try {
            return transitions.remap(from, to);
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            throw new IllegalStateException(
                "Saved data stamped at material id list version " + from +
                    " cannot be brought to the world's version " +
                    to + ". Restore the world's materiallib directory that matches its chunks and player data.",
                e);
        }
    }

    /// Rewrites `stack`'s damage through `remap` when it is a shape stack. A string item id is looked up in
    /// `shapeItemNames` and a numeric one in `shapeItems`. A deleted index strips the item id.
    static void remapStack(NBTTagCompound stack, Int2IntMap remap, IntSet shapeItems, Set<String> shapeItemNames) {
        boolean shape = stack.hasKey("id", TAG_STRING) ? shapeItemNames.contains(stack.getString("id")) :
            shapeItems.contains(IDExtenderCompat.getItemStackID(stack));
        if (!shape) return;
        int damage = stack.getShort("Damage");
        int result = remap.getOrDefault(damage, damage);
        if (result == damage) return;
        if (result == MaterialIdTransitions.DELETE) {
            stack.removeTag("id");
            stack.removeTag("idExt");
        }
        else {
            stack.setShort("Damage", (short) result);
        }
    }

    private static IntSet blockIds() {
        IntSet ids = blockIds;
        if (ids == null) {
            ids = new IntOpenHashSet();
            for (ShapeBlock block : ShapeRegistry.instance().getBlockShapes()) {
                ids.add(Block.getIdFromBlock(block));
            }
            blockIds = ids;
        }
        return ids;
    }

    private static IntSet itemIds() {
        IntSet ids = itemIds;
        if (ids == null) {
            ids = new IntOpenHashSet();
            ShapeRegistry registry = ShapeRegistry.instance();
            for (ShapeItem item : registry.getItemShapes()) {
                ids.add(Item.getIdFromItem(item));
            }
            for (ShapeBlock block : registry.getBlockShapes()) {
                ids.add(Item.getIdFromItem(Item.getItemFromBlock(block)));
            }
            itemIds = ids;
        }
        return ids;
    }

    private static Set<String> itemNames() {
        Set<String> names = itemNames;
        if (names == null) {
            names = new HashSet<>();
            ShapeRegistry registry = ShapeRegistry.instance();
            for (ShapeItem item : registry.getItemShapes()) {
                names.add(Item.itemRegistry.getNameForObject(item));
            }
            for (ShapeBlock block : registry.getBlockShapes()) {
                names.add(Item.itemRegistry.getNameForObject(Item.getItemFromBlock(block)));
            }
            itemNames = names;
        }
        return names;
    }
}
