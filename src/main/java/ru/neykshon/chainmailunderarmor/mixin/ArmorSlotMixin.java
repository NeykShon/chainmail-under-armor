package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.neykshon.chainmailunderarmor.util.ChainmailUtil;
import ru.neykshon.chainmailunderarmor.attachment.ChainmailAttachment;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;
import ru.neykshon.chainmailunderarmor.accessor.ArmorSlotAccessor;

@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public abstract class ArmorSlotMixin implements ArmorSlotAccessor {

    @Shadow
    private LivingEntity owner;

    @Shadow
    private EquipmentSlot slot;

    @Override
    public LivingEntity chainmailUnderArmor$getOwner() {
        return this.owner;
    }

    @Override
    public EquipmentSlot chainmailUnderArmor$getSlot() {
        return this.slot;
    }

    @Inject(
            method = "setByPlayer",
            at = @At("HEAD")
    )
    private void chainmailUnderArmor$moveChainmailToAttachment(
            ItemStack itemStack,
            ItemStack previous,
            CallbackInfo ci
    ) {
        if (!(this.owner instanceof Player player)) {
            return;
        }

        /*
         * Проверяем, была ли в слоте кольчуга.
         *
         * Если это не кольчуга для данного слота,
         * ничего не делаем.
         */
        if (!ChainmailUtil.isChainmailForSlot(previous, this.slot)) {
            return;
        }

        /*
         * Если игрок устанавливает пустой предмет,
         * это обычное снятие предмета.
         *
         * Обработка снятия кольчуги будет выполняться
         * отдельно в AbstractContainerMenuMixin.
         */
        if (itemStack.isEmpty()) {
            return;
        }

        /*
         * Если вместо старой кольчуги надевается другая кольчуга,
         * кольчуга просто заменяется.
         *
         * Кольчуга никогда не должна находиться
         * одновременно в слоте и Attachment.
         */
        if (ChainmailUtil.isChainmail(itemStack)) {
            return;
        }

        /*
         * Если новый предмет не является бронёй,
         * ничего не делаем.
         */
        if (!ChainmailUtil.isArmor(itemStack)) {
            return;
        }

        ChainmailAttachment attachment =
                player.getAttachedOrCreate(ModAttachments.CHAINMAIL);

        /*
         * Если в Attachment уже есть кольчуга для этого слота,
         * не создаём вторую.
         */
        if (!attachment.get(this.slot).isEmpty()) {
            return;
        }

        /*
         * Перемещаем старую кольчугу из обычного слота
         * в Attachment.
         *
         * В результате:
         *
         * Было:
         *   Слот: кольчуга
         *   Attachment: пусто
         *
         * Стало:
         *   Слот: броня
         *   Attachment: кольчуга
         */
        ChainmailAttachment updated =
                attachment.with(
                        this.slot,
                        previous.copy()
                );

        player.setAttached(
                ModAttachments.CHAINMAIL,
                updated
        );
    }
}