package io.github.drcas.gentlemobs;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

public final class GentleRecipeManager {

    private final JavaPlugin plugin;

    private final NamespacedKey netherStarKey;
    private final NamespacedKey dragonsBreathKey;

    public GentleRecipeManager(
            JavaPlugin plugin
    ) {
        this.plugin = plugin;

        netherStarKey =
                new NamespacedKey(
                        plugin,
                        "nether_star"
                );

        dragonsBreathKey =
                new NamespacedKey(
                        plugin,
                        "dragons_breath"
                );
    }

    public void reloadRecipes() {

        removeRecipes();

        if (plugin.getConfig().getBoolean(
                "recipes.nether-star.enabled",
                true
        )) {
            registerNetherStarRecipe();
        }

        if (plugin.getConfig().getBoolean(
                "recipes.dragons-breath.enabled",
                true
        )) {
            registerDragonsBreathRecipe();
        }
    }

    public void removeRecipes() {

        plugin.getServer()
                .removeRecipe(netherStarKey);

        plugin.getServer()
                .removeRecipe(dragonsBreathKey);
    }

    private void registerNetherStarRecipe() {

        ItemStack result =
                new ItemStack(
                        Material.NETHER_STAR,
                        1
                );

        ShapedRecipe recipe =
                new ShapedRecipe(
                        netherStarKey,
                        result
                );

        recipe.shape(
                "NWN",
                "WSW",
                "NWN"
        );

        recipe.setIngredient(
                'N',
                Material.NETHERITE_INGOT
        );

        recipe.setIngredient(
                'W',
                Material.WITHER_SKELETON_SKULL
        );

        recipe.setIngredient(
                'S',
                Material.SOUL_SAND
        );

        boolean added =
                plugin.getServer()
                        .addRecipe(recipe);

        plugin.getLogger().info(
                "Nether Star recipe registered: " +
                        added
        );
    }

    private void registerDragonsBreathRecipe() {

        ItemStack result =
                new ItemStack(
                        Material.DRAGON_BREATH,
                        1
                );

        ShapelessRecipe recipe =
                new ShapelessRecipe(
                        dragonsBreathKey,
                        result
                );

        recipe.addIngredient(
                Material.ENDER_PEARL
        );

        recipe.addIngredient(
                Material.POPPED_CHORUS_FRUIT
        );

        recipe.addIngredient(
                new RecipeChoice.ExactChoice(
                        createMundanePotion()
                )
        );

        boolean added =
                plugin.getServer()
                        .addRecipe(recipe);

        plugin.getLogger().info(
                "Dragon's Breath recipe registered: " +
                        added
        );
    }

    private ItemStack createMundanePotion() {

        ItemStack potion =
                new ItemStack(
                        Material.POTION,
                        1
                );

        PotionMeta meta =
                (PotionMeta) potion.getItemMeta();

        meta.setBasePotionType(
                PotionType.MUNDANE
        );

        potion.setItemMeta(meta);

        return potion;
    }
}