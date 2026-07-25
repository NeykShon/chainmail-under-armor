package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.gamerules.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        LivingEntity entity =
                (LivingEntity) (Object) this;

        /*
         * Нас интересуют только игроки.
         */
        if (!(entity instanceof Player player)) {
            return;
        }

        System.out.println(
                "[ChainmailUnderArmor] Processing death loot for player"
        );

        /*
         * =========================================================
         * KEEP INVENTORY
         * =========================================================
         *
         * При включённом keepInventory Attachment НЕ трогаем.
         *
         * Это важно: кольчуга должна сохраниться у игрока
         * вместе с обычной экипировкой.
         * =========================================================
         */

        if ((Boolean) level.getGameRules().get(
                GameRules.KEEP_INVENTORY
        )) {

            System.out.println(
                    "[ChainmailUnderArmor] KeepInventory = true, "
                            + "attached chainmail will be preserved"
            );

            return;
        }


        /*
         * =========================================================
         * ПОЛУЧАЕМ ATTACHMENT
         * =========================================================
         */

        if (!player.hasAttached(
                ModAttachments.CHAINMAIL
        )) {

            System.out.println(
                    "[ChainmailUnderArmor] No chainmail attachment"
            );

            return;
        }


        /*
         * Получаем текущее содержимое Attachment.
         *
         * ВАЖНО:
         * Сохраняем ссылку на текущее состояние до очистки.
         */
        ChainmailAttachment attachment =
                player.getAttached(
                        ModAttachments.CHAINMAIL
                );


        System.out.println(
                "[ChainmailUnderArmor] Attachment before death drop:"
        );

        System.out.println(
                "Helmet: "
                        + attachment.helmet()
        );

        System.out.println(
                "Chestplate: "
                        + attachment.chestplate()
        );

        System.out.println(
                "Leggings: "
                        + attachment.leggings()
        );

        System.out.println(
                "Boots: "
                        + attachment.boots()
        );


        /*
         * =========================================================
         * ДРОПАЕМ КОЛЬЧУГУ
         * =========================================================
         */

        chainmailUnderArmor$dropChainmail(
                player,
                attachment.helmet()
        );

        chainmailUnderArmor$dropChainmail(
                player,
                attachment.chestplate()
        );

        chainmailUnderArmor$dropChainmail(
                player,
                attachment.leggings()
        );

        chainmailUnderArmor$dropChainmail(
                player,
                attachment.boots()
        );


        /*
         * =========================================================
         * ОЧИЩАЕМ ATTACHMENT
         * =========================================================
         *
         * После смерти Attachment больше не должен содержать
         * предметы, которые уже были обработаны.
         * =========================================================
         */

        player.setAttached(
                ModAttachments.CHAINMAIL,
                ChainmailAttachment.empty()
        );

        System.out.println(
                "[ChainmailUnderArmor] Attachment cleared after death"
        );
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

        /*
         * Пустой слот пропускаем.
         */
        if (stack.isEmpty()) {
            return;
        }


        /*
         * =========================================================
         * CURSE OF VANISHING
         * =========================================================
         *
         * Vanishing должен уничтожить предмет при смерти.
         *
         * Поэтому такой предмет НЕ дропаем.
         *
         * Curse of Binding здесь специально НЕ проверяем.
         * Binding не должен влиять на дроп после смерти.
         * =========================================================
         */

        if (stack.has(DataComponents.ENCHANTMENTS)) {

            var enchantments =
                    stack.get(DataComponents.ENCHANTMENTS);

            if (enchantments != null
                    && enchantments.keySet().stream().anyMatch(
                    enchantment ->
                            enchantment.equals(
                                    Enchantments.VANISHING_CURSE
                            )
            )) {

                System.out.println(
                        "[ChainmailUnderArmor] Chainmail has "
                                + "Curse of Vanishing, destroying: "
                                + stack
                );

                return;
            }
        }


        /*
         * =========================================================
         * ОБЫЧНЫЙ ДРОП
         * =========================================================
         */

        System.out.println(
                "[ChainmailUnderArmor] Dropping attached chainmail: "
                        + stack
        );

        player.drop(
                stack.copy(),
                false,
                false
        );
    }
}