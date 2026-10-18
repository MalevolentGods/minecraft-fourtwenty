package com.malevolentgods.fourtwenty.compat.minecolonies.service;

import com.minecolonies.api.entity.citizen.Skill;
import java.util.Map;

/** Implemented on MineColonies' skill handler by a mixin: temporary skill levels from a bong buff. */
public interface SkillBonusHolder {
    Map<Skill, Integer> fourtwenty$skillBonus();
    void fourtwenty$skillBonus(Map<Skill, Integer> bonus);
}
