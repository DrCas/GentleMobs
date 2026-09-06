package io.github.drcas.gentlemobs.neoforge;
import io.github.drcas.gentlemobs.GentleMode;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class BehaviorPolicyTest {
    @Test void onlyExplicitVanillaHostilesAreIncludedByDefault() {
        for (GentleMode global : GentleMode.values()) {
            for (String excluded : new String[]{"cobblemon:pokemon", "othermod:zombie", "minecraft:cow", "minecraft:wolf", "minecraft:iron_golem", "minecraft:creaking"})
                assertEquals(GentleMode.VANILLA, BehaviorPolicy.mode(excluded, global, Map.of()), excluded);
            assertEquals(global, BehaviorPolicy.mode("minecraft:zoglin", global, Map.of()));
        }
    }
    @Test void overridesOptInExactlyOneIdAndTakePrecedence() {
        var overrides = Map.of("othermod:zombie", GentleMode.NEUTRAL, "minecraft:wither", GentleMode.VANILLA);
        assertEquals(GentleMode.NEUTRAL, BehaviorPolicy.mode("othermod:zombie", GentleMode.PASSIVE, overrides));
        assertEquals(GentleMode.VANILLA, BehaviorPolicy.mode("cobblemon:pokemon", GentleMode.PASSIVE, overrides));
        assertEquals(GentleMode.VANILLA, BehaviorPolicy.mode("minecraft:wither", GentleMode.PASSIVE, overrides));
    }
    @Test void modesRespectEngagement() {
        assertTrue(BehaviorPolicy.blocksPlayers(GentleMode.PASSIVE, true));
        assertTrue(BehaviorPolicy.blocksPlayers(GentleMode.NEUTRAL, false));
        assertFalse(BehaviorPolicy.blocksPlayers(GentleMode.NEUTRAL, true));
        assertFalse(BehaviorPolicy.blocksPlayers(GentleMode.VANILLA, false));
    }
}
