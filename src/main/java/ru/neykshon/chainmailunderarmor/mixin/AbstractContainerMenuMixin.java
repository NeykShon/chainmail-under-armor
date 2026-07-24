package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import ru.neykshon.chainmailunderarmor.attachment.ChainmailAttachment;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Shadow
    @Final
    protected List<Slot> slots;

    @Shadow
    public abstract ItemStack getCarried();

    @Shadow
    public abstract void setCarried(ItemStack stack);

    /**
     * Обрабатывает снятие обычной брони,
     * если под ней находится кольчуга.
     *
     * Результат:
     *
     * Было:
     *
     * Attachment:
     *     кольчуга
     *
     * ArmorSlot:
     *     броня
     *
     * Курсор:
     *     пусто
     *
     * После ЛКМ:
     *
     * Attachment:
     *     пусто
     *
     * ArmorSlot:
     *     кольчуга
     *
     * Курсор:
     *     броня
     */
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
         * Проверяем корректность индекса слота.
         */
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return;
        }

        /*
         * Обрабатываем только обычный PICKUP.
         *
         * QUICK_MOVE (Shift+ЛКМ) пока не трогаем.
         */
        if (containerInput != ContainerInput.PICKUP) {
            return;
        }

        /*
         * Только ЛКМ.
         *
         * ПКМ пока оставляем ванильной механике.
         */
        if (buttonNum != 0) {
            return;
        }

        Slot clickedSlot = this.slots.get(slotIndex);

        /*
         * ArmorSlot является package-private,
         * поэтому напрямую использовать instanceof ArmorSlot нельзя.
         *
         * Вместо этого используем наш accessor-интерфейс,
         * который реализуется ArmorSlotMixin.
         */
        if (!(clickedSlot instanceof ArmorSlotAccessor armorSlot)) {
            return;
        }

        /*
         * Получаем слот экипировки:
         *
         * HEAD
         * CHEST
         * LEGS
         * FEET
         */
        EquipmentSlot equipmentSlot =
                armorSlot.chainmailUnderArmor$getSlot();

        /*
         * Получаем предмет, который сейчас находится
         * в слоте брони.
         */
        ItemStack armor = clickedSlot.getItem();

        /*
         * Если слот пустой,
         * ванильная механика должна работать сама.
         */
        if (armor.isEmpty()) {
            return;
        }

        /*
         * Получаем Attachment игрока.
         */
        ChainmailAttachment attachment =
                player.getAttachedOrCreate(ModAttachments.CHAINMAIL);

        /*
         * Получаем кольчугу, находящуюся под бронёй.
         */
        ItemStack chainmail =
                attachment.get(equipmentSlot);

        /*
         * Если кольчуги под бронёй нет,
         * оставляем ванильное поведение.
         */
        if (chainmail.isEmpty()) {
            return;
        }

        /*
         * На курсоре должен находиться пустой предмет.
         *
         * Это соответствует обычному ЛКМ по предмету:
         *
         * Курсор: пусто
         * Слот: броня
         */
        if (!this.getCarried().isEmpty()) {
            return;
        }

        /*
         * Кладём броню на курсор.
         */
        this.setCarried(armor.copy());

        /*
         * Возвращаем кольчугу в обычный ArmorSlot.
         *
         * Используем set(), а не setByPlayer(),
         * чтобы ArmorSlotMixin не попытался
         * снова положить кольчугу в Attachment.
         */
        clickedSlot.set(chainmail.copy());

        /*
         * Удаляем кольчугу из Attachment.
         */
        ChainmailAttachment updated =
                attachment.with(
                        equipmentSlot,
                        ItemStack.EMPTY
                );

        player.setAttached(
                ModAttachments.CHAINMAIL,
                updated
        );

        /*
         * Полностью отменяем ванильную обработку клика.
         *
         * Иначе Minecraft дополнительно обработает
         * тот же самый клик и состояние слота может сломаться.
         */
        ci.cancel();
    }
}