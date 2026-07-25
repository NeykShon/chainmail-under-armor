package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.neykshon.chainmailunderarmor.accessor.ArmorSlotAccessor;

@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public abstract class ArmorSlotMixin implements ArmorSlotAccessor {

    @Shadow
    private LivingEntity owner;

    @Shadow
    private EquipmentSlot slot;

    @Override
    public LivingEntity chainmailUnderArmor$getOwner() {
        return this.owner;
    }

    @Override
    public EquipmentSlot chainmailUnderArmor$getSlot() {
        return this.slot;
    }
}