package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ru.neykshon.chainmailunderarmor.attachment.ChainmailAttachment;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(
            method = "setGameMode",
            at = @At("HEAD")
    )
    private void chainmailUnderArmor$beforeGameModeChange(
            GameType mode,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayer player =
                (ServerPlayer) (Object) this;

        System.out.println(
                "[ChainmailUnderArmor] ===== GAME MODE CHANGE ====="
        );

        System.out.println(
                "[ChainmailUnderArmor] Current mode: "
                        + player.gameMode()
        );

        System.out.println(
                "[ChainmailUnderArmor] New mode: "
                        + mode
        );

        System.out.println(
                "[ChainmailUnderArmor] Attachment exists BEFORE: "
                        + player.hasAttached(
                        ModAttachments.CHAINMAIL
                )
        );

        if (player.hasAttached(ModAttachments.CHAINMAIL)) {

            ChainmailAttachment attachment =
                    player.getAttached(
                            ModAttachments.CHAINMAIL
                    );

            System.out.println(
                    "[ChainmailUnderArmor] BEFORE:"
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
        }
    }


    @Inject(
            method = "setGameMode",
            at = @At("RETURN")
    )
    private void chainmailUnderArmor$afterGameModeChange(
            GameType mode,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayer player =
                (ServerPlayer) (Object) this;

        System.out.println(
                "[ChainmailUnderArmor] setGameMode result: "
                        + cir.getReturnValue()
        );

        System.out.println(
                "[ChainmailUnderArmor] Attachment exists AFTER: "
                        + player.hasAttached(
                        ModAttachments.CHAINMAIL
                )
        );

        if (player.hasAttached(ModAttachments.CHAINMAIL)) {

            ChainmailAttachment attachment =
                    player.getAttached(
                            ModAttachments.CHAINMAIL
                    );

            System.out.println(
                    "[ChainmailUnderArmor] AFTER:"
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
        }

        System.out.println(
                "[ChainmailUnderArmor] ==========================="
        );
    }
}