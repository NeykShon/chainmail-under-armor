package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.neykshon.chainmailunderarmor.attachment.ChainmailAttachment;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(
            method = "dropEquipment",
            at = @At("TAIL")
    )
    private void chainmailUnderArmor$dropAttachedChainmail(
            ServerLevel level,
            CallbackInfo ci
    ) {
        Player player = (Player) (Object) this;

        /*
         * =========================================================
         * KEEP INVENTORY
         * =========================================================
         *
         * Если включён keepInventory,
         * кольчуга должна сохраниться в Attachment.
         *
         * Поэтому ничего не дропаем
         * и Attachment не очищаем.
         */

        if ((Boolean) level.getGameRules().get(
                GameRules.KEEP_INVENTORY
        )) {
            return;
        }


        /*
         * =========================================================
         * ПОЛУЧАЕМ ATTACHMENT
         * =========================================================
         */

        ChainmailAttachment attachment =
                player.getAttachedOrCreate(
                        ModAttachments.CHAINMAIL
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
         * После смерти кольчуга больше не должна находиться
         * внутри Attachment.
         *
         * Это предотвращает повторный дроп
         * и возможный дюп.
         */

        player.setAttached(
                ModAttachments.CHAINMAIL,
                ChainmailAttachment.empty()
        );
    }


    /*
     * =============================================================
     * ДРОП ОДНОЙ КОЛЬЧУГИ
     * =============================================================
     */

    private static void chainmailUnderArmor$dropChainmail(
            Player player,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }

        /*
         * Используем существующий метод Player.drop().
         *
         * false — предмет не выбрасывается случайным образом.
         * false — предмет не считается выброшенным из руки.
         */

        player.drop(
                stack.copy(),
                false,
                false
        );
    }
}