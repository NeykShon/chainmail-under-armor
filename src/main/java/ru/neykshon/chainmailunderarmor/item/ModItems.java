package ru.neykshon.chainmailunderarmor.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;

import java.util.function.Function;

public class ModItems {

    public static final Item CHAINMAIL_HELMET = register(
            "chainmail_helmet",
            properties -> properties.humanoidArmor(
                    ModArmorMaterials.CHAINMAIL,
                    ArmorType.HELMET
            )
    );

    public static final Item CHAINMAIL_CHESTPLATE = register(
            "chainmail_chestplate",
            properties -> properties.humanoidArmor(
                    ModArmorMaterials.CHAINMAIL,
                    ArmorType.CHESTPLATE
            )
    );

    public static final Item CHAINMAIL_LEGGINGS = register(
            "chainmail_leggings",
            properties -> properties.humanoidArmor(
                    ModArmorMaterials.CHAINMAIL,
                    ArmorType.LEGGINGS
            )
    );

    public static final Item CHAINMAIL_BOOTS = register(
            "chainmail_boots",
            properties -> properties.humanoidArmor(
                    ModArmorMaterials.CHAINMAIL,
                    ArmorType.BOOTS
            )
    );

    private static Item register(
            String name,
            Function<Item.Properties, Item.Properties> factory
    ) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                ChainmailUnderArmor.id(name)
        );

        Item.Properties properties = factory.apply(
                new Item.Properties().setId(itemKey)
        );

        return Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new Item(properties)
        );
    }

    public static void initialize() {
        ChainmailUnderArmor.LOGGER.info(
                "Registering items for " + ChainmailUnderArmor.MOD_ID
        );
    }
}