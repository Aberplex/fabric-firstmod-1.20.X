package net.aredd.firstmod.block.recipe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeCodecs;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CubeCraftRecipe implements CraftingRecipe {
    final int width;
    final int height;
    final DefaultedList<Ingredient> ingredients;
    final ItemStack result;
    final String group;
    final CraftingRecipeCategory category;
    final boolean showNotification;

    public CubeCraftRecipe(String group, CraftingRecipeCategory category, int width, int height, DefaultedList<Ingredient> ingredients, ItemStack result, boolean showNotification) {
        this.group = group;
        this.category = category;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.showNotification = showNotification;
    }

    public CubeCraftRecipe(String group, CraftingRecipeCategory category, int width, int height, DefaultedList<Ingredient> ingredients, ItemStack result) {
        this(group, category, width, height, ingredients, result, true);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializer.CUBE_CRAFT_STATION;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public CraftingRecipeCategory getCategory() {
        return this.category;
    }

    @Override
    public ItemStack getResult(DynamicRegistryManager registryManager) {
        return this.result;
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public boolean showNotification() {
        return this.showNotification;
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= this.width && height >= this.height;
    }

    @Override
    public boolean matches(RecipeInputInventory recipeInputInventory, World world) {
        for (int i = 0; i <= recipeInputInventory.getWidth() - this.width; ++i) {
            for (int j = 0; j <= recipeInputInventory.getHeight() - this.height; ++j) {
                if (this.matchesPattern(recipeInputInventory, i, j, true)) {
                    return true;
                }
                if (!this.matchesPattern(recipeInputInventory, i, j, false)) continue;
                return true;
            }
        }
        return false;
    }

    private boolean matchesPattern(RecipeInputInventory inv, int offsetX, int offsetY, boolean flipped) {
        for (int i = 0; i < inv.getWidth(); ++i) {
            for (int j = 0; j < inv.getHeight(); ++j) {
                int k = i - offsetX;
                int l = j - offsetY;
                Ingredient ingredient = Ingredient.EMPTY;
                if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
                    ingredient = flipped ? this.ingredients.get(this.width - k - 1 + l * this.width) : this.ingredients.get(k + l * this.width);
                }
                if (ingredient.test(inv.getStack(i + j * inv.getWidth()))) continue;
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack craft(RecipeInputInventory recipeInputInventory, DynamicRegistryManager dynamicRegistryManager) {
        return this.getResult(dynamicRegistryManager).copy();
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    @VisibleForTesting
    static String[] removePadding(List<String> pattern) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;
        for (int m = 0; m < pattern.size(); ++m) {
            String string = pattern.get(m);
            i = Math.min(i, findFirstSymbol(string));
            int n = findLastSymbol(string);
            j = Math.max(j, n);
            if (n < 0) {
                if (k == m) {
                    ++k;
                }
                ++l;
                continue;
            }
            l = 0;
        }
        if (pattern.size() == l) {
            return new String[0];
        }
        String[] strings = new String[pattern.size() - l - k];
        for (int o = 0; o < strings.length; ++o) {
            strings[o] = pattern.get(o + k).substring(i, j + 1);
        }
        return strings;
    }

    @Override
    public boolean isEmpty() {
        DefaultedList<Ingredient> defaultedList = this.getIngredients();
        return defaultedList.isEmpty() || defaultedList.stream().filter(ingredient -> !ingredient.isEmpty()).anyMatch(ingredient -> ingredient.getMatchingStacks().length == 0);
    }

    private static int findFirstSymbol(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') ++i;
        return i;
    }

    private static int findLastSymbol(String pattern) {
        int i = pattern.length() - 1;
        while (i >= 0 && pattern.charAt(i) == ' ') --i;
        return i;
    }

    public static class Serializer
            implements RecipeSerializer<CubeCraftRecipe> {
        static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().flatXmap(rows -> {
            if (rows.size() > 3) {
                return DataResult.error(() -> "Invalid pattern: too many rows, 3 is maximum");
            }
            if (rows.isEmpty()) {
                return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
            }
            int i = rows.get(0).length();
            for (String string : rows) {
                if (string.length() > 3) {
                    return DataResult.error(() -> "Invalid pattern: too many columns, 3 is maximum");
                }
                if (i == string.length()) continue;
                return DataResult.error(() -> "Invalid pattern: each row must be the same width");
            }
            return DataResult.success(rows);
        }, DataResult::success);
        static final Codec<String> KEY_ENTRY_CODEC = Codec.STRING.flatXmap(keyEntry -> {
            if (keyEntry.length() != 1) {
                return DataResult.error(() -> "Invalid key entry: '" + keyEntry + "' is an invalid symbol (must be 1 character only).");
            }
            if (" ".equals(keyEntry)) {
                return DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.");
            }
            return DataResult.success(keyEntry);
        }, DataResult::success);

        private static final Codec<CubeCraftRecipe> CODEC = RawCubeCraftingRecipe.CODEC.flatXmap(recipe -> {
            String[] strings = removePadding(recipe.pattern);
            int i = strings[0].length();
            int j = strings.length;
            DefaultedList<Ingredient> defaultedList = DefaultedList.ofSize(i * j, Ingredient.EMPTY);
            HashSet<String> set = Sets.newHashSet(recipe.key.keySet());
            for (int k = 0; k < strings.length; ++k) {
                String string = strings[k];
                for (int l = 0; l < string.length(); ++l) {
                    String string2 = string.substring(l, l + 1);
                    Ingredient ingredient = string2.equals(" ") ? Ingredient.EMPTY : recipe.key.get(string2);
                    if (ingredient == null) {
                        return DataResult.error(() -> "Pattern references symbol '" + string2 + "' but it's not defined in the key");
                    }
                    set.remove(string2);
                    defaultedList.set(l + i * k, ingredient);
                }
            }
            if (!set.isEmpty()) {
                return DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + set);
            }
            CubeCraftRecipe shapedRecipe = new CubeCraftRecipe(recipe.group, recipe.category, i, j, defaultedList, recipe.result, recipe.showNotification);
            return DataResult.success(shapedRecipe);
        }, recipe -> {
            throw new NotImplementedException("Serializing CubeCraftRecipe is not implemented yet.");
        });

        @Override
        public Codec<CubeCraftRecipe> codec() {
            return CODEC;
        }

        @Override
        public CubeCraftRecipe read(PacketByteBuf packetByteBuf) {
            int width = packetByteBuf.readVarInt();
            int height = packetByteBuf.readVarInt();
            String group = packetByteBuf.readString();
            CraftingRecipeCategory craftingRecipeCategory = packetByteBuf.readEnumConstant(CraftingRecipeCategory.class);
            DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(width * height, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromPacket(packetByteBuf));
            ItemStack result = packetByteBuf.readItemStack();
            boolean showNotification = packetByteBuf.readBoolean();
            return new CubeCraftRecipe(group, craftingRecipeCategory, width, height, ingredients, result, showNotification);
        }

        @Override
        public void write(PacketByteBuf packetByteBuf, CubeCraftRecipe shapedRecipe) {
            packetByteBuf.writeVarInt(shapedRecipe.width);
            packetByteBuf.writeVarInt(shapedRecipe.height);
            packetByteBuf.writeString(shapedRecipe.group);
            packetByteBuf.writeEnumConstant(shapedRecipe.category);
            for (Ingredient ingredient : shapedRecipe.ingredients) {
                ingredient.write(packetByteBuf);
            }
            packetByteBuf.writeItemStack(shapedRecipe.result);
            packetByteBuf.writeBoolean(shapedRecipe.showNotification);
        }

        record RawCubeCraftingRecipe(String group, CraftingRecipeCategory category, Map<String, Ingredient> key, List<String> pattern, ItemStack result, boolean showNotification) {
            public static final Codec<RawCubeCraftingRecipe> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(Codecs.createStrictOptionalFieldCodec(Codec.STRING,
                                                    "group", "")
                                            .forGetter(recipe -> recipe.group),
                                    (CraftingRecipeCategory.CODEC.fieldOf("category"))
                                            .orElse(CraftingRecipeCategory.MISC)
                                            .forGetter(recipe -> recipe.category),
                                    (Codecs.strictUnboundedMap(KEY_ENTRY_CODEC, Ingredient.DISALLOW_EMPTY_CODEC)
                                            .fieldOf("key")).forGetter(recipe -> recipe.key),
                                    (PATTERN_CODEC.fieldOf("pattern"))
                                            .forGetter(recipe -> recipe.pattern),
                                    (RecipeCodecs.CRAFTING_RESULT.fieldOf("result"))
                                            .forGetter(recipe -> recipe.result),
                                    Codecs.createStrictOptionalFieldCodec(Codec.BOOL, "show_notification", true)
                                            .forGetter(recipe -> recipe.showNotification))
                            .apply(instance, RawCubeCraftingRecipe::new));
        }
    }
}