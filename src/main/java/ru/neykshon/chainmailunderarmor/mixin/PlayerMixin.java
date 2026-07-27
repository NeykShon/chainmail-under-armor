package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.neykshon.chainmailunderarmor.util.ChainmailAttributes;

@Mixin(Player.class)
public abstract class PlayerMixin {

    /*
     * Пересчёт бонуса защиты от скрытой кольчуги каждый тик.
     * Дороже, чем точечный пересчёт только в местах, где меняется
     * Attachment или экипировка, зато не нужно помнить про вызов
     * в каждой точке мутации (AbstractContainerMenuMixin,
     * LivingEntityMixin, будущие точки) — ChainmailAttributes сам
     * сравнивает желаемое значение с текущим и ничего не делает,
     * если оно не изменилось, поэтому лишней нагрузки на сеть это
     * не создаёт.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void chainmailUnderArmor$recalculateArmorBonus(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (player.level().isClientSide()) {
            // Атрибуты — авторитетные на сервере, клиент получает
            // их через синхронизацию. Пересчёт на клиенте не нужен
            // и может конфликтовать с серверными значениями.
            return;
        }

        ChainmailAttributes.recalculate(player);
    }
}
