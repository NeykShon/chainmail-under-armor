package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.gamerules.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;
import ru.neykshon.chainmailunderarmor.attachment.ChainmailAttachment;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;
import ru.neykshon.chainmailunderarmor.util.ChainmailAttributes;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /*
     * =====================================================================
     * БОНУС ЗАЩИТЫ ОТ КОЛЬЧУГИ — ПОСТ-ОБРАБОТКА УРОНА
     * =====================================================================
     *
     * actuallyHurt(DamageSource, float) вызывается, когда основная броня
     * и зачарования (Protection и т.п.) уже отработали — здесь float
     * amount это именно тот урон, который "прошёл сквозь основную
     * броню", как и описано в задаче. Снимаем с него ещё процент
     * от надетых деталей кольчуги.
     *
     * @ModifyVariable требует, чтобы у хендлера параметром была
     * ТОЛЬКО изменяемая переменная (здесь — float) — попытка
     * захватить ещё и DamageSource вторым параметром не соответствует
     * ожидаемой сигнатуре (это и была ошибка IntelliJ). Поэтому
     * DamageSource запоминаем отдельно: лёгкий @Inject на публичном
     * hurt(DamageSource, float), который вызывается раньше и всегда
     * ведёт к actuallyHurt() в рамках того же вызова, сохраняет его
     * в @Unique-поле — @ModifyVariable потом просто его читает.
     *
     * ВАЖНО: имена/сигнатуры hurt/actuallyHurt соответствуют
     * устоявшейся на протяжении многих версий схеме — не проверены
     * компиляцией под вашу 26.1. Если IDE подсветит несовпадение,
     * найдите (1) публичный метод получения урона, вызывающий
     * actuallyHurt, и (2) сам actuallyHurt — тот, что применяется
     * ПОСЛЕ учёта брони/зачарований и ДО отнятия здоровья.
     */

    @Unique
    private DamageSource chainmailUnderArmor$pendingDamageSource;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void chainmailUnderArmor$captureDamageSource(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        this.chainmailUnderArmor$pendingDamageSource = source;
    }

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
    private float chainmailUnderArmor$reduceDamageAfterArmor(float amount) {

        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof Player player)) {
            return amount;
        }

        DamageSource source = this.chainmailUnderArmor$pendingDamageSource;

        if (source != null && source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            // Если урон в принципе не проходит через броню (голод,
            // /kill, и т.п.), кольчуге тоже нечего снимать.
            return amount;
        }

        if (!player.hasAttached(ModAttachments.CHAINMAIL)) {
            return amount;
        }

        double reductionFraction = ChainmailAttributes.getDamageReductionFraction(player);

        if (reductionFraction <= 0.0) {
            return amount;
        }

        float reduced = (float) (amount * (1.0 - reductionFraction));

        // ТЗ: кольчуга не может опустить урон ниже 1 — но и не
        // поднимает его до 1, если он изначально был меньше
        // (не превращаем царапину в полноценный удар).
        return Math.min(amount, Math.max(1.0F, reduced));
    }

    @Inject(
            method = "dropAllDeathLoot",
            at = @At("HEAD")
    )
    private void chainmailUnderArmor$dropAttachedChainmail(
            ServerLevel level,
            DamageSource source,
            CallbackInfo ci
    ) {

        LivingEntity entity = (LivingEntity) (Object) this;

        /*
         * Нас интересуют только игроки.
         */
        if (!(entity instanceof Player player)) {
            return;
        }

        ChainmailUnderArmor.LOGGER.debug(
                "Processing death loot for player {}", player.getName().getString()
        );

        /*
         * =========================================================
         * KEEP INVENTORY
         * =========================================================
         *
         * При включённом keepInventory Attachment НЕ трогаем.
         * Кольчуга должна сохраниться у игрока вместе
         * с обычной экипировкой.
         * =========================================================
         */

        if ((Boolean) level.getGameRules().get(GameRules.KEEP_INVENTORY)) {

            ChainmailUnderArmor.LOGGER.debug(
                    "KeepInventory = true, attached chainmail will be preserved"
            );

            return;
        }

        /*
         * =========================================================
         * ПОЛУЧАЕМ ATTACHMENT
         * =========================================================
         */

        if (!player.hasAttached(ModAttachments.CHAINMAIL)) {
            return;
        }

        ChainmailAttachment attachment =
                player.getAttached(ModAttachments.CHAINMAIL);

        ChainmailUnderArmor.LOGGER.debug(
                "Attachment before death drop: helmet={}, chestplate={}, "
                        + "leggings={}, boots={}",
                attachment.helmet(), attachment.chestplate(),
                attachment.leggings(), attachment.boots()
        );

        /*
         * =========================================================
         * ДРОПАЕМ КОЛЬЧУГУ
         * =========================================================
         */

        chainmailUnderArmor$dropChainmail(player, attachment.helmet());
        chainmailUnderArmor$dropChainmail(player, attachment.chestplate());
        chainmailUnderArmor$dropChainmail(player, attachment.leggings());
        chainmailUnderArmor$dropChainmail(player, attachment.boots());

        /*
         * =========================================================
         * ОЧИЩАЕМ ATTACHMENT
         * =========================================================
         */

        player.setAttached(
                ModAttachments.CHAINMAIL,
                ChainmailAttachment.empty()
        );

        ChainmailUnderArmor.LOGGER.debug("Attachment cleared after death");
    }

    /*
     * =========================================================
     * ОБРАБОТКА ОДНОГО ПРЕДМЕТА
     * =========================================================
     */

    private static void chainmailUnderArmor$dropChainmail(
            Player player,
            ItemStack stack
    ) {

        if (stack.isEmpty()) {
            return;
        }

        /*
         * =========================================================
         * CURSE OF VANISHING
         * =========================================================
         *
         * Vanishing должен уничтожить предмет при смерти,
         * поэтому такой предмет НЕ дропаем.
         *
         * ВАЖНО: enchantments.keySet() возвращает
         * Set<Holder<Enchantment>>, а Enchantments.VANISHING_CURSE —
         * это ResourceKey<Enchantment>. Сравнивать их через equals()
         * нельзя (это гарантированно false для любого чара) —
         * нужно Holder#is(ResourceKey). Раньше здесь стояла
         * проверка через equals(), из-за которой Curse of Vanishing
         * никогда не срабатывал и кольчуга дропалась всегда.
         *
         * Curse of Binding здесь намеренно НЕ проверяется — Binding
         * не должен влиять на дроп после смерти.
         * =========================================================
         */

        if (stack.has(DataComponents.ENCHANTMENTS)) {

            ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);

            if (enchantments != null
                    && enchantments.keySet().stream()
                    .anyMatch(holder -> holder.is(Enchantments.VANISHING_CURSE))) {

                ChainmailUnderArmor.LOGGER.debug(
                        "Chainmail has Curse of Vanishing, destroying: {}", stack
                );

                return;
            }
        }

        /*
         * =========================================================
         * ОБЫЧНЫЙ ДРОП
         * =========================================================
         */

        ChainmailUnderArmor.LOGGER.debug("Dropping attached chainmail: {}", stack);

        player.drop(stack.copy(), false, false);
    }
}
