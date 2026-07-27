package ru.neykshon.chainmailunderarmor.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import ru.neykshon.chainmailunderarmor.item.ModItems;

public final class ChainmailUtil {

    private ChainmailUtil() {
    }

    /**
     * Проверяет, является ли предмет кольчугой
     * из нашего мода или ванильной кольчугой Minecraft.
     */
    public static boolean isChainmail(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.is(ModItems.CHAINMAIL_HELMET)
                || stack.is(ModItems.CHAINMAIL_CHESTPLATE)
                || stack.is(ModItems.CHAINMAIL_LEGGINGS)
                || stack.is(ModItems.CHAINMAIL_BOOTS)
                || stack.is(Items.CHAINMAIL_HELMET)
                || stack.is(Items.CHAINMAIL_CHESTPLATE)
                || stack.is(Items.CHAINMAIL_LEGGINGS)
                || stack.is(Items.CHAINMAIL_BOOTS);
    }

    /**
     * Проверяет, является ли предмет обычной бронёй.
     *
     * Кольчуга намеренно исключается.
     *
     * В Minecraft 26.1.1 броня определяется через компонент
     * DataComponents.EQUIPPABLE.
     */
    public static boolean isArmor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        /*
         * Кольчуга обрабатывается отдельной механикой.
         */
        if (isChainmail(stack)) {
            return false;
        }

        /*
         * Предметы с компонентом EQUIPPABLE могут быть
         * экипируемыми предметами, поэтому дополнительно
         * проверяем слот.
         *
         * Нам нужны только четыре основных слота брони.
         */
        if (!stack.has(DataComponents.EQUIPPABLE)) {
            return false;
        }

        var equippable = stack.get(DataComponents.EQUIPPABLE);

        if (equippable == null) {
            return false;
        }

        EquipmentSlot slot = equippable.slot();

        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }

    /**
     * Проверяет, является ли кольчуга подходящей
     * для конкретного слота брони.
     */
    public static boolean isChainmailForSlot(
            ItemStack stack,
            EquipmentSlot slot
    ) {
        if (stack.isEmpty()) {
            return false;
        }

        return switch (slot) {
            case HEAD ->
                    stack.is(ModItems.CHAINMAIL_HELMET)
                            || stack.is(Items.CHAINMAIL_HELMET);

            case CHEST ->
                    stack.is(ModItems.CHAINMAIL_CHESTPLATE)
                            || stack.is(Items.CHAINMAIL_CHESTPLATE);

            case LEGS ->
                    stack.is(ModItems.CHAINMAIL_LEGGINGS)
                            || stack.is(Items.CHAINMAIL_LEGGINGS);

            case FEET ->
                    stack.is(ModItems.CHAINMAIL_BOOTS)
                            || stack.is(Items.CHAINMAIL_BOOTS);

            default -> false;
        };
    }

    /**
     * Возвращает слот, для которого предназначена кольчуга.
     *
     * Если предмет не является кольчугой,
     * возвращает null.
     */
    public static EquipmentSlot getChainmailSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        if (stack.is(ModItems.CHAINMAIL_HELMET)
                || stack.is(Items.CHAINMAIL_HELMET)) {
            return EquipmentSlot.HEAD;
        }

        if (stack.is(ModItems.CHAINMAIL_CHESTPLATE)
                || stack.is(Items.CHAINMAIL_CHESTPLATE)) {
            return EquipmentSlot.CHEST;
        }

        if (stack.is(ModItems.CHAINMAIL_LEGGINGS)
                || stack.is(Items.CHAINMAIL_LEGGINGS)) {
            return EquipmentSlot.LEGS;
        }

        if (stack.is(ModItems.CHAINMAIL_BOOTS)
                || stack.is(Items.CHAINMAIL_BOOTS)) {
            return EquipmentSlot.FEET;
        }

        return null;
    }

    public static EquipmentSlot getArmorSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        Item item = stack.getItem();

        // HEAD
        if (item == Items.LEATHER_HELMET
                || item == Items.CHAINMAIL_HELMET
                || item == Items.COPPER_HELMET
                || item == Items.IRON_HELMET
                || item == Items.GOLDEN_HELMET
                || item == Items.DIAMOND_HELMET
                || item == Items.NETHERITE_HELMET
                || item == Items.TURTLE_HELMET) {

            return EquipmentSlot.HEAD;
        }

        // CHEST
        if (item == Items.LEATHER_CHESTPLATE
                || item == Items.CHAINMAIL_CHESTPLATE
                || item == Items.COPPER_CHESTPLATE
                || item == Items.IRON_CHESTPLATE
                || item == Items.GOLDEN_CHESTPLATE
                || item == Items.DIAMOND_CHESTPLATE
                || item == Items.NETHERITE_CHESTPLATE) {

            return EquipmentSlot.CHEST;
        }

        // LEGS
        if (item == Items.LEATHER_LEGGINGS
                || item == Items.CHAINMAIL_LEGGINGS
                || item == Items.COPPER_LEGGINGS
                || item == Items.IRON_LEGGINGS
                || item == Items.GOLDEN_LEGGINGS
                || item == Items.DIAMOND_LEGGINGS
                || item == Items.NETHERITE_LEGGINGS) {

            return EquipmentSlot.LEGS;
        }

        // FEET
        if (item == Items.LEATHER_BOOTS
                || item == Items.CHAINMAIL_BOOTS
                || item == Items.COPPER_BOOTS
                || item == Items.IRON_BOOTS
                || item == Items.GOLDEN_BOOTS
                || item == Items.DIAMOND_BOOTS
                || item == Items.NETHERITE_BOOTS) {

            return EquipmentSlot.FEET;
        }

        return null;
    }
}