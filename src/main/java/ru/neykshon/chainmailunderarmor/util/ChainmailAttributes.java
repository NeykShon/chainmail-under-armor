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

/**
 * =========================================================================
 * МЕХАНИКА ЗАЩИТЫ (v2 — пост-обработка урона вместо очков брони)
 * =========================================================================
 *
 * Раньше бонус кольчуги реализовывался как AttributeModifier на
 * {@code Attributes.ARMOR}, то есть складывался с обычной бронёй ДО
 * применения ванильной формулы урона. Проблема: формула капается на
 * 20 очках, и топовая броня уже у самого капа — то есть любые
 * дополнительные очки либо ничего не дают, либо (что хуже) резко
 * переводят игрока через порог капа, где урон почти обнуляется
 * (см. обсуждение бага — 27 ударов вместо ожидаемых +35%).
 *
 * Новая механика не трогает броню вообще. Она снимает фиксированный
 * процент с того урона, который УЖЕ прошёл через основную броню и
 * зачарования (то есть применяется к тому же float, который движок
 * готовится вычесть из здоровья). Это не масштабируется с тем,
 * насколько близка внешняя броня к капу, — то есть кольчуга
 * одинаково по проценту полезна что под кожей, что под незеритом,
 * и никогда не выталкивает игрока за какой-либо порог формулы.
 *
 * Сам перехват — в {@link ru.neykshon.chainmailunderarmor.mixin.LivingEntityMixin}
 * (@ModifyVariable на actuallyHurt). Этот класс отвечает за то,
 * *сколько* процентов снимать, и отдельно — за штраф к скорости.
 *
 * <pre>
 * Элемент             | Снижение урона | Штраф к скорости
 * Шлем                | +4%             | −3%
 * Нагрудник            | +8%             | −6%
 * Поножи              | +5%             | −4%
 * Ботинки             | +2%             | −1%
 * Итого (полный сет)  | +20%            | −14%
 * </pre>
 *
 * Условие: снижение никогда не опускает итоговый урон ниже 1 и
 * никогда не поднимает его выше исходного (см. LivingEntityMixin).
 *
 * ВАЖНО: пакет {@code net.minecraft.world.entity.ai.attributes}
 * (нужен только для штрафа к скорости, см. ниже) указан по
 * многолетнему устоявшемуся Mojmap-расположению и НЕ проверен
 * компиляцией под вашу 26.1 — сверьте через "Go to definition" от
 * {@code LivingEntity#getAttribute}, если IDE подсветит импорт.
 */
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

    /**
     * Суммарная доля снижения урона (0.0–0.20) от всех надетых сейчас
     * под бронёй деталей кольчуги. Вызывается из LivingEntityMixin
     * в момент получения урона — учитывает актуальное состояние
     * Attachment на этот самый момент, отдельного кэша не нужно.
     */
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

    /**
     * Пересчитывает и применяет штраф к скорости от надетой под
     * бронёй кольчуги. Дешёвая операция — безопасно вызывать хоть
     * каждый тик: если желаемое значение не изменилось с прошлого
     * раза, модификатор не трогаем.
     */
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
