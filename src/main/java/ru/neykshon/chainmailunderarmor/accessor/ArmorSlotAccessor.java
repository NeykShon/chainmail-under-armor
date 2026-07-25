package ru.neykshon.chainmailunderarmor.accessor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public interface ArmorSlotAccessor {

    LivingEntity chainmailUnderArmor$getOwner();

    EquipmentSlot chainmailUnderArmor$getSlot();
}