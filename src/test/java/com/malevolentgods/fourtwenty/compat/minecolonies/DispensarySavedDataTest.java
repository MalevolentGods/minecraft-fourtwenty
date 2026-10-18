package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Affinity;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Preference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DispensarySavedDataTest {
    private static final UUID CITIZEN = UUID.fromString("00000000-0000-0000-0000-000000000042");

    private static CompoundTag v1() {
        var citizen = new CompoundTag();
        citizen.putInt("colony", 1); citizen.putUUID("citizen", CITIZEN);
        citizen.putBoolean("eligible", true); citizen.putLong("dueAt", 900); citizen.putLong("graceUntil", 2_000);
        citizen.putLong("satisfiedUntil", 900); citizen.putLong("healthUntil", 900); citizen.putLong("lastConsumedAt", 100);
        citizen.putLong("revision", 3);
        var list = new ListTag(); list.add(citizen);
        var root = new CompoundTag();
        root.putInt("version", 1); root.putLong("time", 500); root.put("citizens", list);
        return root;
    }

    @Test void versionOneSavesLoadWithEveryoneUnexposed() {
        var data = DispensarySavedData.load(v1(), null);
        assertEquals(500, data.time());
        assertEquals(3, data.citizen(1, CITIZEN).revision(), "demand records are kept");
        assertEquals(Affinity.UNEXPOSED, data.affinity(1, CITIZEN), "exposure log starts as N/A");
    }

    @Test void affinityRoundTripsInVersionTwo() {
        var data = DispensarySavedData.load(v1(), null);
        var rolled = new Affinity(400, Preference.EDIBLE, 2, 5, -1, 450);
        data.updateAffinity(1, CITIZEN, previous -> rolled);
        var saved = data.save(new CompoundTag(), null);
        assertEquals(3, saved.getInt("version"));
        var reloaded = DispensarySavedData.load(saved, null);
        assertEquals(rolled, reloaded.affinity(1, CITIZEN));
        assertEquals(Affinity.UNEXPOSED, reloaded.affinity(1, UUID.randomUUID()));
    }

    @Test void forceDueMakesACravingCitizenDueAndInvalidatesOldCompletions() {
        var data = DispensarySavedData.load(v1(), null);   // dueAt 900, clock 500
        var before = data.citizen(1, CITIZEN);
        assertFalse(before.due(500));
        assertTrue(data.forceDue(1, CITIZEN));
        var after = data.citizen(1, CITIZEN);
        assertTrue(after.due(500));
        assertEquals(before.revision() + 1, after.revision());
        assertEquals(before.graceUntil(), after.graceUntil(), "grace is kept");
        assertFalse(data.forceDue(1, UUID.randomUUID()), "no craving, nothing to force");
    }

    @Test void specialtyBuffsRoundTripAndOlderSavesHaveNone() {
        var data = DispensarySavedData.load(v1(), null);
        assertNull(data.buff(1, CITIZEN));
        var buff = new com.malevolentgods.fourtwenty.compat.minecolonies.domain.ColonistBuff(
                com.malevolentgods.fourtwenty.domain.BongVariety.FORGE, 24_500, 10);
        data.setBuff(1, CITIZEN, buff);
        var reloaded = DispensarySavedData.load(data.save(new CompoundTag(), null), null);
        assertEquals(buff, reloaded.buff(1, CITIZEN));
        reloaded.setBuff(1, CITIZEN, null);
        assertNull(reloaded.buff(1, CITIZEN));
        assertThrows(IllegalArgumentException.class, () -> new com.malevolentgods.fourtwenty.compat.minecolonies.domain.ColonistBuff(
                com.malevolentgods.fourtwenty.domain.BongVariety.GLASS, 1, 5), "the plain glass bong gives no buff");
    }

    @Test void futureVersionsAndBrokenLogsAreRejected() {
        var future = v1(); future.putInt("version", 4);
        assertThrows(IllegalArgumentException.class, () -> DispensarySavedData.load(future, null));
        var missingLog = v1(); missingLog.putInt("version", 2);
        assertThrows(IllegalArgumentException.class, () -> DispensarySavedData.load(missingLog, null));
        var data = DispensarySavedData.load(v1(), null);
        data.updateAffinity(1, CITIZEN, previous -> new Affinity(400, Preference.SMOKE, 3, 3, -1, -1));
        var saved = data.save(new CompoundTag(), null);
        ((CompoundTag) saved.getList("affinity", 10).get(0)).putLong("exposedAt", 9_999);
        assertThrows(IllegalArgumentException.class, () -> DispensarySavedData.load(saved, null), "exposure after the save clock");
    }
}
