package ru.neykshon.chainmailunderarmor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import ru.neykshon.chainmailunderarmor.item.ModItems;

public final class ChainmailUtil {

    private ChainmailUtil() {
    }

    /**
     * Проверяет, является ли предмет кольчугой из нашего мода
     * или ванильной кольчугой Minecraft.
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
     * Проверяет, является ли предмет обычной бронёй,
     * то есть предметом, который можно надеть в один из
     * четырёх основных слотов брони.
     *
     * Кольчуга намеренно исключается.
     */
    public static boolean isArmor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        /*
         * Кольчуга считается отдельным типом предмета.
         * Она не должна проходить эту проверку,
         * поскольку для неё используется специальная логика.
         */
        if (isChainmail(stack)) {
            return false;
        }

        Item item = stack.getItem();

        /*
         * ArmorItem — стандартный способ определить,
         * что предмет является бронёй.
         *
         * Сюда попадут:
         * - кожаная броня
         * - медная броня
         * - железная броня
         * - золотая броня
         * - алмазная броня
         * - незеритовая броня
         * - черепаший панцирь
         * и другие ArmorItem.
         */
        return item instanceof ArmorItem;
    }

    /**
     * Проверяет, является ли кольчуга подходящей
     * для конкретного слота брони.
     *
     * Например:
     *
     * CHAINMAIL_HELMET -> HEAD
     * CHAINMAIL_CHESTPLATE -> CHEST
     * CHAINMAIL_LEGGINGS -> LEGS
     * CHAINMAIL_BOOTS -> FEET
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