package net.aredd.firstmod.item.custom;

import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;

public class CubeSwordItem extends SwordItem {
    public CubeSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
}