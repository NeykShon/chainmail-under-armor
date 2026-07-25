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
import ru.neykshon.chainmailunderarmor.util.ChainmailUtil;

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
    private void chainmailUnderArmor$handleArmorSlot(
            int slotIndex,
            int buttonNum,
            ContainerInput containerInput,
            Player player,
            CallbackInfo ci
    ) {

        /*
         * =========================================================
         * ПРОВЕРКА ИНДЕКСА СЛОТА
         * =========================================================
         */

        if (slotIndex < 0) {
            return;
        }


        /*
         * =========================================================
         * ОБРАБАТЫВАЕМ ТОЛЬКО ОБЫЧНЫЙ ЛКМ
         *
         * Shift+ЛКМ и остальные типы кликов пока оставляем
         * ванильной механике.
         * =========================================================
         */

        if (containerInput != ContainerInput.PICKUP) {
            return;
        }

        if (buttonNum != 0) {
            return;
        }


        /*
         * =========================================================
         * ПОЛУЧАЕМ НАЖАТЫЙ СЛОТ
         * =========================================================
         */

        Slot clickedSlot = this.getSlot(slotIndex);


        /*
         * ArmorSlot является package-private,
         * поэтому напрямую использовать его нельзя.
         *
         * Используем наш accessor.
         */

        if (!(clickedSlot instanceof ArmorSlotAccessor armorSlot)) {
            return;
        }


        /*
         * =========================================================
         * ОПРЕДЕЛЯЕМ СЛОТ БРОНИ
         *
         * HEAD
         * CHEST
         * LEGS
         * FEET
         * =========================================================
         */

        EquipmentSlot equipmentSlot =
                armorSlot.chainmailUnderArmor$getSlot();


        /*
         * =========================================================
         * ПОЛУЧАЕМ ТЕКУЩИЙ ПРЕДМЕТ В ARMOR SLOT
         * =========================================================
         */

        ItemStack slotItem = clickedSlot.getItem();


        /*
         * =========================================================
         * ПОЛУЧАЕМ ПРЕДМЕТ НА КУРСОРЕ
         * =========================================================
         */

        ItemStack carried = this.getCarried();


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
         * ПОЛУЧАЕМ КОЛЬЧУГУ ИЗ ATTACHMENT
         *
         * Если здесь находится кольчуга,
         * значит в ArmorSlot сейчас должна находиться
         * обычная броня поверх неё.
         * =========================================================
         */

        ItemStack attachedChainmail =
                attachment.get(equipmentSlot);


        /*
         * =========================================================
         * СЦЕНАРИЙ №1
         *
         * СНЯТИЕ БРОНИ, КОГДА ПОД НЕЙ ЕСТЬ КОЛЬЧУГА
         *
         * Было:
         *
         * ArmorSlot:
         *     броня
         *
         * Attachment:
         *     кольчуга
         *
         * Cursor:
         *     пусто
         *
         * После:
         *
         * ArmorSlot:
         *     кольчуга
         *
         * Attachment:
         *     пусто
         *
         * Cursor:
         *     броня
         * =========================================================
         */

        if (!slotItem.isEmpty()
                && !attachedChainmail.isEmpty()
                && carried.isEmpty()
                && ChainmailUtil.isArmor(slotItem)) {

            /*
             * Сохраняем броню на курсор.
             */
            this.setCarried(
                    slotItem.copy()
            );


            /*
             * Возвращаем кольчугу
             * из Attachment в ArmorSlot.
             *
             * Используем set(), а не setByPlayer().
             *
             * Это важно:
             *
             * setByPlayer() может снова вызвать
             * нашу логику ArmorSlotMixin.
             *
             * set() просто устанавливает предмет.
             */
            clickedSlot.set(
                    attachedChainmail.copy()
            );


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
             * Полностью отменяем ванильный клик.
             */
            ci.cancel();
            return;
        }


        /*
         * =========================================================
         * СЦЕНАРИЙ №2
         *
         * НАДЕВАНИЕ БРОНИ ПОВЕРХ КОЛЬЧУГИ
         *
         * Было:
         *
         * ArmorSlot:
         *     кольчуга
         *
         * Attachment:
         *     пусто
         *
         * Cursor:
         *     броня
         *
         * После:
         *
         * ArmorSlot:
         *     броня
         *
         * Attachment:
         *     кольчуга
         *
         * Cursor:
         *     пусто
         *
         * =========================================================
         */

        if (!slotItem.isEmpty()
                && ChainmailUtil.isChainmailForSlot(
                slotItem,
                equipmentSlot
        )
                && ChainmailUtil.isArmor(carried)
                && attachedChainmail.isEmpty()) {


            /*
             * Перемещаем кольчугу
             * из ArmorSlot в Attachment.
             */
            ChainmailAttachment updated =
                    attachment.with(
                            equipmentSlot,
                            slotItem.copy()
                    );


            player.setAttached(
                    ModAttachments.CHAINMAIL,
                    updated
            );


            /*
             * Устанавливаем броню
             * непосредственно в ArmorSlot.
             */
            clickedSlot.set(
                    carried.copy()
            );


            /*
             * Убираем броню с курсора.
             */
            this.setCarried(
                    ItemStack.EMPTY
            );


            /*
             * Отменяем ванильный клик.
             *
             * Это предотвращает стандартную замену:
             *
             * кольчуга -> броня
             *
             * при которой кольчуга попала бы
             * на курсор и могла бы привести к дюпу.
             */
            ci.cancel();
            return;
        }


        /*
         * =========================================================
         * СЦЕНАРИЙ №3
         *
         * ЗАПРЕТ НАДЕВАНИЯ КОЛЬЧУГИ ПОВЕРХ БРОНИ,
         * ЕСЛИ ПОД БРОНЁЙ УЖЕ ЕСТЬ КОЛЬЧУГА.
         *
         * Было:
         *
         * ArmorSlot:
         *     броня
         *
         * Attachment:
         *     кольчуга
         *
         * Cursor:
         *     новая кольчуга
         *
         * Результат:
         *
         * НИЧЕГО НЕ ПРОИСХОДИТ.
         *
         * Это предотвращает ситуацию,
         * когда кольчуга из Attachment
         * и новая кольчуга могут одновременно
         * существовать в одной позиции.
         * =========================================================
         */

        if (!slotItem.isEmpty()
                && ChainmailUtil.isArmor(slotItem)
                && !attachedChainmail.isEmpty()
                && ChainmailUtil.isChainmailForSlot(
                carried,
                equipmentSlot
        )) {

            /*
             * Ничего не меняем.
             *
             * Просто отменяем ванильную обработку.
             */
            ci.cancel();
            return;
        }


        /*
         * =========================================================
         * СЦЕНАРИЙ №4
         *
         * ЗАПРЕТ НАДЕВАНИЯ КОЛЬЧУГИ В ПУСТОЙ СЛОТ,
         * ЕСЛИ В ATTACHMENT УЖЕ ЕСТЬ КОЛЬЧУГА.
         *
         * Это защита от потенциального дюпа,
         * если каким-либо образом возникнет состояние:
         *
         * ArmorSlot:
         *     пусто
         *
         * Attachment:
         *     кольчуга
         *
         * Cursor:
         *     новая кольчуга
         *
         * Результат:
         *
         * НИЧЕГО НЕ ПРОИСХОДИТ.
         * =========================================================
         */

        if (slotItem.isEmpty()
                && !attachedChainmail.isEmpty()
                && ChainmailUtil.isChainmailForSlot(
                carried,
                equipmentSlot
        )) {

            ci.cancel();
            return;
        }


        /*
         * =========================================================
         * СЦЕНАРИЙ №5
         *
         * ЗАПРЕТ НАДЕВАНИЯ ВТОРОЙ КОЛЬЧУГИ
         * В СЛОТ, ГДЕ УЖЕ ЕСТЬ КОЛЬЧУГА.
         *
         * Обычно этот сценарий обработает ваниль,
         * но оставляем явную проверку.
         *
         * Было:
         *
         * ArmorSlot:
         *     кольчуга
         *
         * Attachment:
         *     пусто
         *
         * Cursor:
         *     кольчуга
         *
         * Ванильная механика заменяет кольчугу
         * другой кольчугой.
         *
         * Это разрешено.
         *
         * Поэтому здесь ничего не отменяем.
         */


        /*
         * =========================================================
         * ВСЕ ОСТАЛЬНЫЕ СЛУЧАИ
         *
         * Передаём обработку ванильной механике.
         *
         * Например:
         *
         * - обычная броня -> обычная броня
         * - снятие обычной брони без кольчуги
         * - надевание брони в пустой слот
         * - надевание кольчуги в пустой слот
         * - смена кольчуги на кольчугу
         *
         * =========================================================
         */
    }
}