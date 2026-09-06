package io.github.drcas.gentlemobs.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

public record RecipeEnabledCondition(String recipe) implements ICondition {
    public static final MapCodec<RecipeEnabledCondition> CODEC = Codec.STRING.fieldOf("recipe")
        .xmap(RecipeEnabledCondition::new, RecipeEnabledCondition::recipe);
    @Override public boolean test(IContext context) {
        var s = GentleMobsNeoForge.CONFIG.get();
        return switch (recipe) { case "nether-star" -> s.netherStar(); case "dragons-breath" -> s.dragonsBreath(); default -> false; };
    }
    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }
}
