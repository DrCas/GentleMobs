package io.github.drcas.gentlemobs.neoforge;
import io.github.drcas.gentlemobs.GentleMode;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class GentleConfigTest {
    @TempDir Path dir;
    @Test void savesAndReloadsWithoutLosingRecipeOrFleeOptions() throws Exception {
        Path path = dir.resolve("gentlemobs.json");
        Files.writeString(path, """
            {"mode":"NEUTRAL","flee":{"distance":20,"speed":1.8,"duration-ticks":100},
             "mob-overrides":{"ZOMBIE":"VANILLA","some_mod:guard":"PASSIVE"},
             "recipes":{"nether-star":{"enabled":false}}}
            """);
        var config = new GentleConfig(path); config.load();
        config.setMode(GentleMode.PASSIVE);
        config.override("minecraft:ghast", GentleMode.NEUTRAL);
        var reloaded = new GentleConfig(path); reloaded.load();
        assertEquals(20, reloaded.get().distance());
        assertEquals(100, reloaded.get().duration());
        assertFalse(reloaded.get().netherStar());
        assertEquals(GentleMode.VANILLA, reloaded.get().overrides().get("minecraft:zombie"));
        assertEquals(GentleMode.NEUTRAL, reloaded.get().overrides().get("minecraft:ghast"));
        reloaded.override("some_mod:guard", null);
        assertFalse(reloaded.get().overrides().containsKey("some_mod:guard"));
    }
    @Test void invalidReloadKeepsLastKnownGoodSettingsAndFile() throws Exception {
        Path path = dir.resolve("gentlemobs.json");
        var config = new GentleConfig(path); config.load(); config.setMode(GentleMode.VANILLA);
        for (String invalid : new String[]{"{", "{\"mode\":\"typo\"}", "{\"flee\":{\"speed\":-1}}", "{\"flee\":{\"duration-ticks\":2.5}}", "{\"recipes\":{\"nether-star\":{\"enabled\":\"false\"}}}"}) {
            Files.writeString(path, invalid);
            assertThrows(IOException.class, config::load);
            assertEquals(GentleMode.VANILLA, config.get().mode());
            assertEquals(invalid, Files.readString(path));
        }
    }
}
