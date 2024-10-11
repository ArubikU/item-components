package com.boyonk.itemcomponents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.bukkit.craftbukkit.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentPatch.SplitResult;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;

public class ItemComponentsManager {

    public static final Logger LOGGER = LoggerFactory.getLogger("Item Components");
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("item_components",
            "item_components");
    public static final String DIRECTORY = "item_components";

    private final Map<Holder.Reference<Item>, List<UnmergedComponents>> itemComponents = new HashMap<>();
    private final Map<TagKey<Item>, List<UnmergedComponents>> tagComponents = new HashMap<>();

    private static final Codec<List<ExtraCodecs.TagOrElementLocation>> TARGETS_CODEC = Codec.either(
            ExtraCodecs.TAG_OR_ELEMENT_ID,
            ExtraCodecs.TAG_OR_ELEMENT_ID.listOf())
            .xmap(
                    either -> either.map(List::of, Function.identity()),
                    entries -> entries.size() == 1 ? Either.left(entries.getFirst()) : Either.right(entries));
    private static final Codec<List<ResourceLocation>> PARENTS_CODEC = ResourceLocation.CODEC.listOf();
    private final Map<Item, DataComponentMap> itemMapCache = new HashMap<>();
    private final Map<Item, DataComponentPatch> itemChangesCache = new HashMap<>();

    protected ItemComponentsManager() {
    }

    public void reload(ResourceManager manager) {
        this.clear();

        Map<ResourceLocation, UnresolvedComponents> map = new HashMap<>();
        this.loadIntoMap(manager, map);
        new Resolver(map).resolve(this.itemComponents::put, this.tagComponents::put);
        ItemComponents.forEachStack(stack -> ((BaseComponentSetter) (Object) stack.getItem())
                .itemcomponents$setBaseComponents(stack.getItem().components()));
    }

    private void loadIntoMap(ResourceManager manager, Map<ResourceLocation, UnresolvedComponents> map) {
        FileToIdConverter finder = FileToIdConverter.json(ItemComponentsManager.DIRECTORY);

        for (Entry<ResourceLocation, Resource> entry : finder.listMatchingResources(manager).entrySet()) {
            ResourceLocation resourcePath = entry.getKey();
            Resource resource = entry.getValue();

            ResourceLocation resourceId = finder.fileToId(resourcePath);
            try {
                if (resource != null) {
                    try (BufferedReader reader = resource.openAsReader()) {
                        int priority;
                        List<ResourceLocation> parents = new ArrayList<>();
                        List<ExtraCodecs.TagOrElementLocation> targets = new ArrayList<>();
                        DataComponentPatch changes = DataComponentPatch.EMPTY;

                        JsonElement json = JsonParser.parseReader(reader);
                        JsonObject object = JsonHelper.getObjectOrNull(json.getAsJsonObject(), "item_components");

                        var temp = JsonHelper.getPrimitiveOrNull(object, "priority");
                        if (temp == null)
                            priority = 0;
                        else
                            priority = temp.getAsInt();
                        if (object.has("parents")) {
                            parents = PARENTS_CODEC
                                    .decode(JsonOps.INSTANCE, JsonHelper.getOrCreateObject(object, "parents"))
                                    .getOrThrow(JsonSyntaxException::new).getFirst();
                        }
                        if (object.has("targets")) {
                            targets = TARGETS_CODEC
                                    .decode(JsonOps.INSTANCE, JsonHelper.getOrCreateObject(object, "targets"))
                                    .getOrThrow(JsonSyntaxException::new).getFirst();
                        }
                        if (object.has("components")) {
                            changes = DataComponentPatch.CODEC
                                    .decode(JsonOps.INSTANCE, JsonHelper.getOrCreateObject(object, "components"))
                                    .getOrThrow(JsonSyntaxException::new).getFirst();
                        }

                        map.put(resourcePath,
                                new UnresolvedComponents(resourcePath, priority, targets, parents, changes));
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Couldn't read components {} from namespace {} in data pack {}, {}", resourceId,
                        resourcePath, resource.sourcePackId(),
                        exception);
            }
        }
    }

    protected void clear() {
        this.itemComponents.clear();
        this.tagComponents.clear();
        this.itemMapCache.clear();
        this.itemChangesCache.clear();
    }

    public final DataComponentMap getMap(Item item, DataComponentMap base) {
        DataComponentMap cached = this.itemMapCache.get(item);
        if (cached != null)
            return cached;

        DataComponentPatch changes = this.getChanges(item);

        DataComponentMap result = PatchedDataComponentMap.fromPatch(base, changes);
        this.itemMapCache.put(item, result);
        return result;
    }

    public final DataComponentPatch getChanges(Item item) {
        DataComponentPatch cached = this.itemChangesCache.get(item);
        if (cached != null)
            return cached;

        Reference<Item> entry = MinecraftServer.getDefaultRegistryAccess().registryOrThrow(Registries.ITEM)
                .createIntrusiveHolder(item);
        DataComponentPatch.Builder builder = DataComponentPatch.builder();

        Stream.concat(
                this.itemComponents.getOrDefault(entry, List.of()).stream(),
                entry.tags().map(tag -> this.tagComponents.getOrDefault(tag, List.of()))
                        .flatMap(Collection::stream))
                .sorted(Comparator.comparingInt(UnmergedComponents::priority))
                .map(UnmergedComponents::components)
                .flatMap(Collection::stream)
                .forEachOrdered(changes -> {
                    SplitResult pair = changes.split();
                    pair.added().forEach(builder::set);
                    pair.removed().forEach(builder::remove);
                });

        DataComponentPatch result = builder.build();
        this.itemChangesCache.put(item, result);
        return result;
    }

    private static class Resolver {
        private final Map<ResourceLocation, UnresolvedComponents> unresolved;
        private final Map<ResourceLocation, UnmergedComponents> resolved = new HashMap<>();
        private final Set<ResourceLocation> toResolve = new HashSet<>();

        Resolver(Map<ResourceLocation, UnresolvedComponents> unresolved) {
            this.unresolved = unresolved;
        }

        public void resolve(BiConsumer<Holder.Reference<Item>, List<UnmergedComponents>> itemAdder,
                BiConsumer<TagKey<Item>, List<UnmergedComponents>> tagAdder) {
            Map<ExtraCodecs.TagOrElementLocation, List<UnmergedComponents>> unmerged = new HashMap<>();

            for (Map.Entry<ResourceLocation, UnresolvedComponents> entry : this.unresolved.entrySet()) {
                try {
                    ResourceLocation id = entry.getKey();
                    UnresolvedComponents unresolved = entry.getValue();
                    if (unresolved.targets().isEmpty())
                        continue;

                    UnmergedComponents resolved = this.getOrResolve(id);
                    unresolved.targets().forEach(
                            target -> unmerged.computeIfAbsent(target, (k) -> new ArrayList<>(1)).add(resolved));
                } catch (Exception e) {
                    LOGGER.error("Failed to load {}", entry.getKey(), e);
                }
            }

            for (Map.Entry<ExtraCodecs.TagOrElementLocation, List<UnmergedComponents>> entry : unmerged.entrySet()) {
                ExtraCodecs.TagOrElementLocation id = entry.getKey();
                List<UnmergedComponents> list = entry.getValue();
                if (id.tag()) {
                    tagAdder.accept(TagKey.create(Registries.ITEM, id.id()), list);
                } else {
                    if (MinecraftServer.getDefaultRegistryAccess().registry(Registries.ITEM).get()
                            .containsKey(id.id())) {
                        itemAdder.accept(MinecraftServer.getDefaultRegistryAccess().registry(Registries.ITEM).get()
                                .get(id.id()).builtInRegistryHolder(), list);
                    }
                }
            }
        }

        UnmergedComponents getOrResolve(ResourceLocation id) throws Exception {
            if (this.resolved.containsKey(id))
                return this.resolved.get(id);

            if (this.toResolve.contains(id)) {
                throw new IllegalStateException("Circular reference while loading " + id);
            }
            this.toResolve.add(id);
            UnresolvedComponents unresolved = this.unresolved.get(id);
            if (unresolved == null)
                throw new FileNotFoundException(id.toString());

            List<DataComponentPatch> components = new ArrayList<>();
            for (ResourceLocation parentId : unresolved.parents()) {
                try {
                    components.addAll(this.getOrResolve(parentId).components());
                } catch (Exception e) {
                    LOGGER.error("Unable to resolve parent {} referenced from {}", parentId, id, e);
                }
            }
            components.add(unresolved.components());
            UnmergedComponents resolved = new UnmergedComponents(unresolved.resourceId(), unresolved.priority(),
                    components);

            this.resolved.put(id, resolved);
            this.toResolve.remove(id);

            return resolved;
        }
    }

    record UnresolvedComponents(ResourceLocation resourceId, int priority,
            List<ExtraCodecs.TagOrElementLocation> targets, List<ResourceLocation> parents,
            DataComponentPatch components) {
    }

    record UnmergedComponents(ResourceLocation resourceId, int priority, List<DataComponentPatch> components) {
    }
}
