package com.malevolentgods.fourtwenty.compat.minecolonies.service;

import com.malevolentgods.fourtwenty.block.GlassBongBlock;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.ColonistBuff;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Preference;
import com.malevolentgods.fourtwenty.domain.BongVariety;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Specialty bong sessions at the Dispensary. Level 5 residents who smoke have their at-the-shop
 * session at a specialty bong placed in the shop, picking the one that boosts their job's skills;
 * it costs one bud from the shop and gives a one-day skill buff ({@link BongVariety#skillBonus}).
 */
public final class BongSessions {
    public record Session(BlockPos pos, BongVariety variety) {}

    /** Specialty bongs placed inside the shop. */
    public static List<Session> bongsIn(DispensaryBuilding building) {
        var level = building.getColony().getWorld();
        var corners = building.getCorners();
        var found = new ArrayList<Session>();
        for (var pos : BlockPos.betweenClosed(corners.getA(), corners.getB())) {
            if (level.getBlockState(pos).getBlock() instanceof GlassBongBlock bong && bong.variety().specialty()) {
                found.add(new Session(pos.immutable(), bong.variety()));
            }
        }
        return found;
    }

    /** The job's primary and secondary skill names (nulls when unemployed). */
    public static String[] jobSkills(ICitizenData citizen) {
        var work = citizen.getWorkBuilding();
        if (work == null || citizen.getJob() == null) return new String[]{null, null};
        for (var module : work.getModulesByType(WorkerBuildingModule.class)) {
            if (module.getJobEntry() == citizen.getJob().getJobRegistryEntry()) {
                return new String[]{name(module.getPrimarySkill()), name(module.getSecondarySkill())};
            }
        }
        return new String[]{null, null};
    }

    private static String name(Skill skill) { return skill == null ? null : skill.name(); }

    /**
     * Offer a level 5 smoker a session at the best-matching specialty bong in the shop; takes one bud
     * from {@code sources} to pack it. Null when there's no bong, no bud, or the customer doesn't smoke.
     */
    public static Session offer(EntityCitizen citizen, DispensaryBuilding building, int serviceLevel, Preference preference,
                                List<IItemHandler> sources) {
        if (serviceLevel < 5 || (preference != Preference.SMOKE && preference != Preference.BOTH)) return null;
        var bongs = bongsIn(building);
        if (bongs.isEmpty()) return null;
        var skills = jobSkills(citizen.getCitizenData());
        var choice = BongVariety.choose(bongs.stream().map(Session::variety).toList(), skills[0], skills[1]);
        if (choice.isEmpty()) return null;
        var session = bongs.stream().filter(s -> s.variety() == choice.get()).findFirst().orElseThrow();
        for (var source : sources) {
            for (int slot = 0; slot < source.getSlots(); slot++) {
                if (source.getStackInSlot(slot).is(ModItems.WEED_BUD.get()) && !source.extractItem(slot, 1, false).isEmpty()) return session;
            }
        }
        return null;
    }

    /** Grant the session's one-day skill buff. */
    public static void grant(ServerLevel level, ICitizenData citizen, BongVariety variety) {
        var saved = DispensarySavedData.get(level);
        int bonus = variety.skillBonus(jobSkills(citizen)[0]);
        saved.setBuff(citizen.getColony().getID(), citizen.getUUID(), new ColonistBuff(variety, saved.time() + BongVariety.BUFF_TICKS, bonus));
        refresh(citizen, saved);
    }

    /** Keep the citizen's skill bonus in step with their saved buff; drops expired buffs. Every colony tick. */
    public static void refresh(ICitizenData citizen, DispensarySavedData saved) {
        var buff = saved.buff(citizen.getColony().getID(), citizen.getUUID());
        if (buff != null && !buff.active(saved.time())) {
            saved.setBuff(citizen.getColony().getID(), citizen.getUUID(), null);
            buff = null;
        }
        Map<Skill, Integer> wanted = Map.of();
        if (buff != null) {
            var bonus = new EnumMap<Skill, Integer>(Skill.class);
            for (var skill : buff.variety().skills) bonus.put(Skill.valueOf(skill), buff.bonus());
            wanted = bonus;
        }
        if (citizen.getCitizenSkillHandler() instanceof SkillBonusHolder holder && !holder.fourtwenty$skillBonus().equals(wanted)) {
            holder.fourtwenty$skillBonus(wanted);
        }
    }

    private BongSessions() {}
}
