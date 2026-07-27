package ru.neykshon.chainmailunderarmor.util;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;
import ru.neykshon.chainmailunderarmor.attachment.ChainmailAttachment;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;

public class ChainmailAttributes {

    private static final Map<EquipmentSlot, Double> DAMAGE_REDUCTION_PERCENT =
            new EnumMap<>(EquipmentSlot.class);

    private static final Map<EquipmentSlot, Double> SPEED_PENALTY_PERCENT =
            new EnumMap<>(EquipmentSlot.class);

    static {
        DAMAGE_REDUCTION_PERCENT.put(EquipmentSlot.HEAD, 0.04);
        DAMAGE_REDUCTION_PERCENT.put(EquipmentSlot.CHEST, 0.08);
        DAMAGE_REDUCTION_PERCENT.put(EquipmentSlot.LEGS, 0.05);
        DAMAGE_REDUCTION_PERCENT.put(EquipmentSlot.FEET, 0.02);

        SPEED_PENALTY_PERCENT.put(EquipmentSlot.HEAD, -0.03);
        SPEED_PENALTY_PERCENT.put(EquipmentSlot.CHEST, -0.06);
        SPEED_PENALTY_PERCENT.put(EquipmentSlot.LEGS, -0.04);
        SPEED_PENALTY_PERCENT.put(EquipmentSlot.FEET, -0.01);
    }

    private static Identifier chainmailUnderArmor$speedModifierId(EquipmentSlot slot) {
        return ChainmailUnderArmor.id("chainmail_under_armor_speed_" + slot.getName());
    }

    public static double getDamageReductionFraction(Player player) {

        if (!player.hasAttached(ModAttachments.CHAINMAIL)) {
            return 0.0;
        }

        ChainmailAttachment attachment = player.getAttached(ModAttachments.CHAINMAIL);

        double total = 0.0;

        for (Map.Entry<EquipmentSlot, Double> entry : DAMAGE_REDUCTION_PERCENT.entrySet()) {
            if (!attachment.get(entry.getKey()).isEmpty()) {
                total += entry.getValue();
            }
        }

        return total;
    }

    public static void recalculate(Player player) {

        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (speedAttribute == null) {
            return;
        }

        if (!player.hasAttached(ModAttachments.CHAINMAIL)) {
            for (EquipmentSlot slot : SPEED_PENALTY_PERCENT.keySet()) {
                speedAttribute.removeModifier(chainmailUnderArmor$speedModifierId(slot));
            }
            return;
        }

        ChainmailAttachment attachment = player.getAttached(ModAttachments.CHAINMAIL);

        for (EquipmentSlot slot : SPEED_PENALTY_PERCENT.keySet()) {
            chainmailUnderArmor$applySpeedSlot(speedAttribute, attachment, slot);
        }
    }

    private static void chainmailUnderArmor$applySpeedSlot(
            AttributeInstance speedAttribute,
            ChainmailAttachment attachment,
            EquipmentSlot slot
    ) {
        Identifier id = chainmailUnderArmor$speedModifierId(slot);

        boolean chainmailWorn = !attachment.get(slot).isEmpty();

        double desired = chainmailWorn ? SPEED_PENALTY_PERCENT.get(slot) : 0.0;

        AttributeModifier existing = speedAttribute.getModifier(id);
        double current = existing == null ? 0.0 : existing.amount();

        if (Math.abs(current - desired) < 0.0001) {
            return;
        }

        if (existing != null) {
            speedAttribute.removeModifier(id);
        }

        if (desired != 0.0) {
            speedAttribute.addOrUpdateTransientModifier(
                    new AttributeModifier(
                            id, desired, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );
        }
    }
}
