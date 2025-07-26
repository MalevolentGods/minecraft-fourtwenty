package com.malevolentgods.fourtwenty.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CraftingExperienceManager {
    private static final Map<UUID, CraftingSkillData> playerSkills = new HashMap<>();
    
    // Experience thresholds for each level (exponential growth)
    private static final int[] LEVEL_THRESHOLDS = {
        0,      // Level 0 (Novice)
        100,    // Level 1 (Apprentice)
        250,    // Level 2 (Skilled)
        500,    // Level 3 (Expert)
        1000,   // Level 4 (Master)
        2000,   // Level 5 (Grandmaster)
        3500,   // Level 6 (Legendary)
        5500,   // Level 7 (Mythical)
        8000,   // Level 8 (Divine)
        12000   // Level 9 (Transcendent)
    };
    
    private static final String[] LEVEL_NAMES = {
        "Novice", "Apprentice", "Skilled", "Expert", "Master",
        "Grandmaster", "Legendary", "Mythical", "Divine", "Transcendent"
    };
    
    /**
     * Get or create skill data for a player
     */
    public static CraftingSkillData getSkillData(Player player) {
        UUID playerId = player.getUUID();
        return playerSkills.computeIfAbsent(playerId, k -> new CraftingSkillData());
    }
    
    /**
     * Award experience to a player for crafting
     */
    public static void awardExperience(Player player, int baseExperience) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        CraftingSkillData skillData = getSkillData(player);
        int oldLevel = skillData.getLevel();
        
        skillData.addExperience(baseExperience);
        int newLevel = skillData.getLevel();
        
        // Check for level up
        if (newLevel > oldLevel) {
            onLevelUp(serverPlayer, oldLevel, newLevel);
        }
        
        // Save to player data
        saveSkillData(serverPlayer, skillData);
    }
    
    /**
     * Handle level up notifications and rewards
     */
    private static void onLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        // Send congratulations message
        Component levelName = Component.literal(LEVEL_NAMES[Math.min(newLevel, LEVEL_NAMES.length - 1)])
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        
        Component message = Component.translatable("message.fourtwenty.crafting.level_up", levelName)
            .withStyle(ChatFormatting.GREEN);
        
        player.sendSystemMessage(message);
        
        // Play level up sound
        player.playNotifySound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 
            net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
        
        // Award bonus items for milestone levels
        if (newLevel == 3) { // Expert level
            player.getInventory().add(new net.minecraft.world.item.ItemStack(
                com.malevolentgods.fourtwenty.registry.ModItems.WEED_GRINDER.get(), 1));
            player.sendSystemMessage(Component.translatable("message.fourtwenty.crafting.reward.grinder")
                .withStyle(ChatFormatting.YELLOW));
        } else if (newLevel == 5) { // Master level
            player.getInventory().add(new net.minecraft.world.item.ItemStack(
                com.malevolentgods.fourtwenty.registry.ModItems.WEED_BUD.get(), 5));
            player.sendSystemMessage(Component.translatable("message.fourtwenty.crafting.reward.buds")
                .withStyle(ChatFormatting.YELLOW));
        }
    }
    
    /**
     * Calculate efficiency bonus based on player level
     */
    public static float getLevelEfficiencyBonus(Player player) {
        CraftingSkillData skillData = getSkillData(player);
        int level = skillData.getLevel();
        
        // 5% bonus per level, max 50% at level 10
        return Math.min(level * 0.05f, 0.5f);
    }
    
    /**
     * Calculate speed bonus based on player level
     */
    public static float getLevelSpeedBonus(Player player) {
        CraftingSkillData skillData = getSkillData(player);
        int level = skillData.getLevel();
        
        // 10% speed increase per level, max 100% at level 10
        return Math.min(level * 0.1f, 1.0f);
    }
    
    /**
     * Save skill data to player NBT
     */
    public static void saveSkillData(ServerPlayer player, CraftingSkillData skillData) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound("fourtwenty");
        
        CompoundTag skillTag = new CompoundTag();
        skillTag.putInt("experience", skillData.getExperience());
        skillTag.putInt("totalCrafts", skillData.getTotalCrafts());
        
        modData.put("crafting_skill", skillTag);
        playerData.put("fourtwenty", modData);
    }
    
    /**
     * Load skill data from player NBT
     */
    public static void loadSkillData(ServerPlayer player) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound("fourtwenty");
        
        if (modData.contains("crafting_skill")) {
            CompoundTag skillTag = modData.getCompound("crafting_skill");
            CraftingSkillData skillData = new CraftingSkillData();
            skillData.setExperience(skillTag.getInt("experience"));
            skillData.setTotalCrafts(skillTag.getInt("totalCrafts"));
            
            playerSkills.put(player.getUUID(), skillData);
        }
    }
    
    /**
     * Get experience needed for next level
     */
    public static int getExperienceForNextLevel(int currentLevel) {
        if (currentLevel >= LEVEL_THRESHOLDS.length - 1) {
            return -1; // Max level reached
        }
        return LEVEL_THRESHOLDS[currentLevel + 1];
    }
    
    /**
     * Get level from experience amount
     */
    public static int getLevelFromExperience(int experience) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (experience >= LEVEL_THRESHOLDS[i]) {
                return i;
            }
        }
        return 0;
    }
    
    /**
     * Skill data container class
     */
    public static class CraftingSkillData {
        private int experience = 0;
        private int totalCrafts = 0;
        
        public int getExperience() { return experience; }
        public void setExperience(int experience) { this.experience = experience; }
        
        public int getTotalCrafts() { return totalCrafts; }
        public void setTotalCrafts(int totalCrafts) { this.totalCrafts = totalCrafts; }
        
        public void addExperience(int amount) {
            this.experience += amount;
            this.totalCrafts++;
        }
        
        public int getLevel() {
            return getLevelFromExperience(experience);
        }
        
        public String getLevelName() {
            int level = getLevel();
            return LEVEL_NAMES[Math.min(level, LEVEL_NAMES.length - 1)];
        }
        
        public int getExperienceToNextLevel() {
            int nextLevelExp = getExperienceForNextLevel(getLevel());
            return nextLevelExp == -1 ? 0 : nextLevelExp - experience;
        }
        
        public float getProgressToNextLevel() {
            int currentLevel = getLevel();
            if (currentLevel >= LEVEL_THRESHOLDS.length - 1) return 1.0f;
            
            int currentLevelExp = LEVEL_THRESHOLDS[currentLevel];
            int nextLevelExp = LEVEL_THRESHOLDS[currentLevel + 1];
            int progressExp = experience - currentLevelExp;
            int totalExpNeeded = nextLevelExp - currentLevelExp;
            
            return (float) progressExp / totalExpNeeded;
        }
    }
}
