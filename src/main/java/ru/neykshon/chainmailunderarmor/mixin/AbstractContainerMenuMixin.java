package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.neykshon.chainmailunderarmor.accessor.ArmorSlotAccessor;
import ru.neykshon.chainmailunderarmor.attachment.ChainmailAttachment;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Shadow
    public abstract Slot getSlot(int slotIndex);

    @Shadow
    public abstract ItemStack getCarried();

    @Shadow
    public abstract void setCarried(ItemStack stack);

    @Inject(
            method = "doClick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void chainmailUnderArmor$handleArmorPickup(
            int slotIndex,
            int buttonNum,
            ContainerInput containerInput,
            Player player,
            CallbackInfo ci
    ) {
        /*
         * Нас интересует только обычный ЛКМ.
         */
        if (containerInput != ContainerInput.PICKUP) {
            return;
        }

        if (buttonNum != 0) {
            return;
        }

        /*
         * Проверяем индекс.
         *
         * Отрицательный индекс используется Minecraft
         * для клика вне окна инвентаря.
         */
        if (slotIndex < 0) {
            return;
        }

        /*
         * Получаем слот через метод AbstractContainerMenu,
         * не обращаясь напрямую к полю slots.
         */
        Slot clickedSlot;

        try {
            clickedSlot = this.getSlot(slotIndex);
        } catch (IndexOutOfBoundsException ignored) {
            return;
        }

        /*
         * Проверяем, является ли слот ArmorSlot.
         *
         * Сам ArmorSlot package-private,
         * поэтому используем accessor.
         */
        if (!(clickedSlot instanceof ArmorSlotAccessor armorSlot)) {
            return;
        }

        /*
         * Получаем EquipmentSlot.
         */
        EquipmentSlot equipmentSlot =
                armorSlot.chainmailUnderArmor$getSlot();

        /*
         * Получаем броню, находящуюся непосредственно
         * в обычном слоте брони.
         */
        ItemStack armor = clickedSlot.getItem();

        if (armor.isEmpty()) {
            return;
        }

        /*
         * Получаем кольчугу из Attachment.
         */
        ChainmailAttachment attachment =
                player.getAttachedOrCreate(ModAttachments.CHAINMAIL);

        ItemStack chainmail =
                attachment.get(equipmentSlot);

        /*
         * Если под бронёй нет кольчуги,
         * оставляем стандартное поведение Minecraft.
         */
        if (chainmail.isEmpty()) {
            return;
        }

        /*
         * Если на курсоре уже что-то есть,
         * это не обычное снятие брони.
         *
         * Пока оставляем ванильную механику.
         */
        if (!this.getCarried().isEmpty()) {
            return;
        }

        /*
         * Кладём верхнюю броню на курсор.
         */
        this.setCarried(armor.copy());

        /*
         * Возвращаем кольчугу в обычный ArmorSlot.
         *
         * Используем set(), а не setByPlayer(),
         * чтобы ArmorSlotMixin не обработал её повторно.
         */
        clickedSlot.set(chainmail.copy());

        /*
         * Очищаем Attachment.
         */
        player.setAttached(
                ModAttachments.CHAINMAIL,
                attachment.with(
                        equipmentSlot,
                        ItemStack.EMPTY
                )
        );

        /*
         * Отменяем ванильную обработку клика.
         */
        ci.cancel();
    }
}