package ru.neykshon.chainmailunderarmor.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
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
}