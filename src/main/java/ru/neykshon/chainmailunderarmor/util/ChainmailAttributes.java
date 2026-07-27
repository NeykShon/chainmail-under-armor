package ru.neykshon.chainmailunderarmor.util;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;
import ru.neykshon.chainmailunderarmor.attachment.ChainmailAttachment;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;

/**
 * Бонус защиты от кольчуги, надетой под основной доспех
 * (значения — из README):
 *
 * <pre>
 * Элемент             | Без брони сверху | С бронёй сверху
 * Койф                | +2               | +1
 * Кольчуга            | +5               | +3
 * Кольчужные поножи   | +4               | +2
 * Кольчужные ботинки  | +1               | +1
 * </pre>
 *
 * Значения совпадают с ванильными armor points кольчужного
 * комплекта — то есть "без брони сверху" кольчуга под одеждой
 * защищает ровно как обычная надетая кольчуга, а с бронёй сверху
 * даёт уменьшенный бонус (баланс — суммарно меньше, чем два
 * отдельных комплекта).
 *
 * Реализовано через постоянно поддерживаемый в актуальном
 * состоянии AttributeModifier с фиксированным id на
 * {@code Attributes.ARMOR} — по одному модификатору на слот,
 * а не через переопределение формулы расчёта брони, чтобы
 * бонус корректно учитывался везде, где движок читает атрибут
 * (HUD, расчёт урона, другие моды).
 *
 * ВАЖНО: пакет {@code net.minecraft.world.entity.ai.attributes}
 * указан по многолетнему устоявшемуся Mojmap-расположению и НЕ
 * проверен компиляцией под вашу 26.1 — если IDE подсветит один
 * из импортов ниже, сверьте актуальный путь через "Go to
 * definition" от {@code LivingEntity#getAttribute}. Сама идея
 * (id-based AttributeModifier с операцией ADD_VALUE) должна
 * остаться верной.
 */
public class ChainmailAttributes {

    private record Bonus(double alone, double underArmor) {
    }

    private static final Map<EquipmentSlot, Bonus> BONUSES = new EnumMap<>(EquipmentSlot.class);

    static {
        BONUSES.put(EquipmentSlot.HEAD, new Bonus(2.0, 1.0));
        BONUSES.put(EquipmentSlot.CHEST, new Bonus(5.0, 3.0));
        BONUSES.put(EquipmentSlot.LEGS, new Bonus(4.0, 2.0));
        BONUSES.put(EquipmentSlot.FEET, new Bonus(1.0, 1.0));
    }

    private static Identifier chainmailUnderArmor$modifierId(EquipmentSlot slot) {
        return ChainmailUnderArmor.id("chainmail_under_armor_" + slot.getName());
    }

    /**
     * Пересчитывает и применяет модификаторы защиты от скрытой
     * кольчуги для всех 4 слотов брони. Дешёвая операция —
     * безопасно вызывать хоть каждый тик: если желаемое значение
     * не изменилось с прошлого раза, модификатор не трогаем.
     */
    public static void recalculate(Player player) {

        AttributeInstance armorAttribute = player.getAttribute(Attributes.ARMOR);

        if (armorAttribute == null) {
            return;
        }

        if (!player.hasAttached(ModAttachments.CHAINMAIL)) {
            // Нечего носить под бронёй — снимаем все наши модификаторы,
            // если они почему-то остались (например, после /kill
            // и обнуления Attachment).
            for (EquipmentSlot slot : BONUSES.keySet()) {
                armorAttribute.removeModifier(chainmailUnderArmor$modifierId(slot));
            }
            return;
        }

        ChainmailAttachment attachment = player.getAttached(ModAttachments.CHAINMAIL);

        for (EquipmentSlot slot : BONUSES.keySet()) {
            chainmailUnderArmor$applySlot(player, armorAttribute, attachment, slot);
        }
    }

    private static void chainmailUnderArmor$applySlot(
            Player player,
            AttributeInstance armorAttribute,
            ChainmailAttachment attachment,
            EquipmentSlot slot
    ) {
        Identifier id = chainmailUnderArmor$modifierId(slot);

        ItemStack hiddenChainmail = attachment.get(slot);

        double desired = 0.0;

        if (!hiddenChainmail.isEmpty()) {

            Bonus bonus = BONUSES.get(slot);
            ItemStack outerArmor = player.getItemBySlot(slot);

            desired = outerArmor.isEmpty() ? bonus.alone() : bonus.underArmor();
        }

        AttributeModifier existing = armorAttribute.getModifier(id);
        double current = existing == null ? 0.0 : existing.amount();

        if (current == desired) {
            // Уже актуально — не трогаем (избегаем лишней сети/пересчётов).
            return;
        }

        if (existing != null) {
            armorAttribute.removeModifier(id);
        }

        if (desired > 0.0) {
            armorAttribute.addOrUpdateTransientModifier(
                    new AttributeModifier(id, desired, AttributeModifier.Operation.ADD_VALUE)
            );
        }
    }
}
