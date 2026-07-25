package ru.neykshon.chainmailunderarmor.item;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

public class ModArmorMaterials {

    public static final ArmorMaterial CHAINMAIL = new ArmorMaterial(
            15,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 5,
                    ArmorType.LEGGINGS, 4,
                    ArmorType.BOOTS, 1
            ),
            12,
            SoundEvents.ARMOR_EQUIP_CHAIN,
            0.0F,
            0.0F,
            ItemTags.REPAIRS_CHAIN_ARMOR,
            EquipmentAssets.CHAINMAIL
    );

    public static void initialize() {
    }
}