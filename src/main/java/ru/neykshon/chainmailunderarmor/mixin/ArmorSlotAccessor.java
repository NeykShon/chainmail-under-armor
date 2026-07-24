package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public interface ArmorSlotAccessor {

    LivingEntity chainmailUnderArmor$getOwner();

    EquipmentSlot chainmailUnderArmor$getSlot();
}