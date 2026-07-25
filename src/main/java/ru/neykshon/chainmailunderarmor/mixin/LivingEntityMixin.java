package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.gamerules.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;
import ru.neykshon.chainmailunderarmor.attachment.ChainmailAttachment;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

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
