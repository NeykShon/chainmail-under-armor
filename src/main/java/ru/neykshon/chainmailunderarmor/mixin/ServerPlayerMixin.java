package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;

/**
 * Тестами (см. переписку по багрепорту) подтверждено, что сама
 * смена gamemode НЕ портит и НЕ теряет Attachment — данные
 * переживают setGameMode() без изменений в обе стороны.
 * Настоящий источник рассинхронизации — необработанные типы
 * кликов в инвентарном меню (см. AbstractContainerMenuMixin).
 *
 * Раньше здесь стоял отладочный код с System.out.println на
 * каждую смену режима — он не нёс функциональной нагрузки и
 * засорял консоль сервера в проде. Оставляем только компактный
 * debug-трейс (не виден в консоли по умолчанию, при необходимости
 * включается настройкой уровня логирования).
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(
            method = "setGameMode",
            at = @At("RETURN")
    )
    private void chainmailUnderArmor$afterGameModeChange(
            GameType mode,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        ChainmailUnderArmor.LOGGER.debug(
                "{} switched game mode to {}, chainmail attachment present: {}",
                player.getScoreboardName(), mode,
                player.hasAttached(ModAttachments.CHAINMAIL)
        );
    }
}
