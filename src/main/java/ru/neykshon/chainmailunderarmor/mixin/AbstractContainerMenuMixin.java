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

    @Shadow
    public abstract ItemStack quickMoveStack(
            Player player,
            int slotIndex
    );

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
         * ПРОВЕРКА ИНДЕКСА
         * =========================================================
         */

        if (slotIndex < 0) {
            return;
        }


        /*
         * =========================================================
         * ПОЛУЧАЕМ НАЖАТЫЙ СЛОТ
         * =========================================================
         */

        Slot clickedSlot = this.getSlot(slotIndex);


        /*
         * =========================================================
         * SHIFT + ЛКМ
         * =========================================================
         *
         * Здесь обрабатываем два случая:
         *
         * 1. Shift+ЛКМ по обычной броне из инвентаря,
         *    когда в соответствующем ArmorSlot находится
         *    кольчуга.
         *
         *    Было:
         *
         *    ArmorSlot:
         *        кольчуга
         *
         *    Attachment:
         *        пусто
         *
         *    Inventory:
         *        броня
         *
         *    После:
         *
         *    ArmorSlot:
         *        броня
         *
         *    Attachment:
         *        кольчуга
         *
         *    Inventory:
         *        пусто
         *
         *
         * 2. Shift+ЛКМ по обычной броне, которая уже находится
         *    в ArmorSlot и под которой есть кольчуга.
         *
         *    В этом случае броня снимается в инвентарь,
         *    а кольчуга возвращается в ArmorSlot.
         *
         * =========================================================
         */

        if (containerInput == ContainerInput.QUICK_MOVE
                && buttonNum == 0) {

            /*
             * -----------------------------------------------------
             * СЛУЧАЙ A
             *
             * Shift+ЛКМ непосредственно по ArmorSlot.
             *
             * Это снятие верхней брони обратно в инвентарь.
             * -----------------------------------------------------
             */

            if (clickedSlot instanceof ArmorSlotAccessor armorSlotAccessor) {

                EquipmentSlot equipmentSlot =
                        armorSlotAccessor.chainmailUnderArmor$getSlot();

                ItemStack armorInSlot =
                        clickedSlot.getItem();

                if (!armorInSlot.isEmpty()
                        && ChainmailUtil.isArmor(armorInSlot)) {

                    ChainmailAttachment attachment =
                            player.getAttachedOrCreate(
                                    ModAttachments.CHAINMAIL
                            );

                    ItemStack attachedChainmail =
                            attachment.get(equipmentSlot);

                    /*
                     * Если под бронёй нет кольчуги,
                     * используем стандартную механику Minecraft.
                     */
                    if (attachedChainmail.isEmpty()) {
                        return;
                    }

                    /*
                     * Сохраняем кольчугу.
                     */
                    ItemStack chainmailToRestore =
                            attachedChainmail.copy();

                    /*
                     * Временно убираем кольчугу
                     * из Attachment.
                     *
                     * Это нужно, чтобы ванильный
                     * quickMoveStack() корректно снял броню.
                     */
                    player.setAttached(
                            ModAttachments.CHAINMAIL,
                            attachment.with(
                                    equipmentSlot,
                                    ItemStack.EMPTY
                            )
                    );

                    /*
                     * Запоминаем броню.
                     */
                    ItemStack armorBefore =
                            armorInSlot.copy();

                    /*
                     * Выполняем ванильный Shift+ЛКМ.
                     *
                     * Броня должна отправиться
                     * в инвентарь игрока.
                     */
                    this.quickMoveStack(
                            player,
                            slotIndex
                    );

                    /*
                     * Проверяем результат.
                     */
                    boolean armorWasMoved =
                            clickedSlot.getItem().isEmpty()
                                    && !armorBefore.isEmpty();

                    if (armorWasMoved) {

                        /*
                         * Броня успешно ушла в инвентарь.
                         *
                         * Возвращаем кольчугу
                         * в ArmorSlot.
                         */
                        clickedSlot.set(
                                chainmailToRestore
                        );

                    } else {

                        /*
                         * Броня не смогла переместиться.
                         *
                         * Возвращаем кольчугу
                         * обратно в Attachment.
                         */
                        player.setAttached(
                                ModAttachments.CHAINMAIL,
                                attachment.with(
                                        equipmentSlot,
                                        chainmailToRestore
                                )
                        );
                    }

                    /*
                     * Ванильный doClick() больше ничего
                     * делать не должен.
                     */
                    ci.cancel();
                    return;
                }
            }


            /*
             * -----------------------------------------------------
             * СЛУЧАЙ B
             *
             * Shift+ЛКМ по обычной броне в инвентаре.
             *
             * Если в соответствующем ArmorSlot находится
             * кольчуга, переносим её в Attachment,
             * а броню ставим поверх неё.
             * -----------------------------------------------------
             */

            ItemStack clickedItem =
                    clickedSlot.getItem();

            /*
             * Нас интересует только обычная броня.
             *
             * Кольчуга здесь не обрабатывается.
             */
            if (clickedItem.isEmpty()
                    || !ChainmailUtil.isArmor(clickedItem)) {

                return;
            }

            /*
             * Определяем, в какой ArmorSlot должна попасть
             * эта броня.
             */
            EquipmentSlot armorEquipmentSlot =
                    ChainmailUtil.getArmorSlot(clickedItem);

            if (armorEquipmentSlot == null) {
                return;
            }


            /*
             * Ищем ArmorSlot соответствующего типа.
             *
             * Здесь мы не используем this.slots,
             * поэтому Shadow для slots не нужен.
             *
             * В большинстве случаев это будет стандартный
             * InventoryMenu игрока.
             *
             * Диапазон 0..45 соответствует стандартному
             * инвентарю игрока:
             *
             * 0-8   — хотбар
             * 9-35  — основной инвентарь
             * 36-39 — ArmorSlot
             * 40    — offhand
             *
             * Однако мы всё равно проверяем через Accessor,
             * поэтому жёстко полагаться на номер слота
             * не будем.
             */

            Slot targetArmorSlot = null;

            for (int i = 0; i < 46; i++) {

                Slot possibleSlot;

                try {
                    possibleSlot = this.getSlot(i);
                } catch (Exception ignored) {
                    continue;
                }

                if (possibleSlot instanceof ArmorSlotAccessor accessor
                        && accessor.chainmailUnderArmor$getSlot()
                        == armorEquipmentSlot) {

                    targetArmorSlot = possibleSlot;
                    break;
                }
            }

            /*
             * ArmorSlot не найден.
             *
             * Это может быть контейнер,
             * в котором нет слотов брони игрока.
             *
             * Оставляем ванильное поведение.
             */
            if (targetArmorSlot == null) {
                return;
            }


            /*
             * Получаем предмет,
             * который сейчас находится в ArmorSlot.
             */
            ItemStack currentArmorSlotItem =
                    targetArmorSlot.getItem();


            /*
             * Получаем Attachment.
             */
            ChainmailAttachment quickMoveAttachment =
                    player.getAttachedOrCreate(
                            ModAttachments.CHAINMAIL
                    );


            /*
             * Получаем кольчугу из Attachment.
             */
            ItemStack currentAttachedChainmail =
                    quickMoveAttachment.get(
                            armorEquipmentSlot
                    );


            /*
             * Нас интересует только состояние:
             *
             * ArmorSlot:
             *     кольчуга
             *
             * Attachment:
             *     пусто
             *
             * Inventory:
             *     обычная броня
             */
            if (!ChainmailUtil.isChainmailForSlot(
                    currentArmorSlotItem,
                    armorEquipmentSlot
            )) {

                /*
                 * В ArmorSlot нет кольчуги.
                 *
                 * Оставляем ванильный Shift+ЛКМ.
                 */
                return;
            }

            /*
             * Дополнительная защита от дублирования.
             *
             * Если Attachment уже содержит кольчугу,
             * мы не должны создавать вторую.
             */
            if (!currentAttachedChainmail.isEmpty()) {

                /*
                 * Ситуация некорректная:
                 *
                 * ArmorSlot:
                 *     кольчуга
                 *
                 * Attachment:
                 *     кольчуга
                 *
                 * Поэтому ничего не делаем.
                 */
                ci.cancel();
                return;
            }


            /*
             * =====================================================
             * ПЕРЕНОСИМ КОЛЬЧУГУ В ATTACHMENT
             * =====================================================
             */

            ChainmailAttachment updatedAttachment =
                    quickMoveAttachment.with(
                            armorEquipmentSlot,
                            currentArmorSlotItem.copy()
                    );

            player.setAttached(
                    ModAttachments.CHAINMAIL,
                    updatedAttachment
            );


            /*
             * =====================================================
             * СТАВИМ БРОНЮ В ARMOR SLOT
             * =====================================================
             */

            targetArmorSlot.set(
                    clickedItem.copy()
            );


            /*
             * =====================================================
             * УДАЛЯЕМ БРОНЮ ИЗ ИСХОДНОГО СЛОТА
             * =====================================================
             */

            clickedSlot.set(
                    ItemStack.EMPTY
            );


            /*
             * =====================================================
             * ОТМЕНЯЕМ ВАНИЛЬНЫЙ QUICK_MOVE
             * =====================================================
             *
             * Мы уже полностью выполнили перемещение вручную.
             *
             * Если не отменить ванильный doClick(),
             * он попытается обработать тот же предмет повторно.
             *
             * Это может привести к:
             *
             * - дюпу;
             * - потере предмета;
             * - повторному перемещению;
             * - неправильной синхронизации.
             * =====================================================
             */

            ci.cancel();
            return;
        }


        /*
         * =========================================================
         * ВСЕ ОСТАЛЬНЫЕ ТИПЫ КЛИКА
         * =========================================================
         *
         * Нас интересует только:
         *
         * - обычный ЛКМ;
         * - Shift+ЛКМ.
         *
         * Всё остальное передаём ванильной механике.
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
         * ЕСЛИ СЛОТ НЕ ARMOR SLOT
         * =========================================================
         */

        if (!(clickedSlot instanceof ArmorSlotAccessor armorSlot)) {
            return;
        }


        /*
         * =========================================================
         * ОПРЕДЕЛЯЕМ СЛОТ БРОНИ
         * =========================================================
         */

        EquipmentSlot equipmentSlot =
                armorSlot.chainmailUnderArmor$getSlot();


        /*
         * =========================================================
         * ПОЛУЧАЕМ ПРЕДМЕТЫ
         * =========================================================
         */

        ItemStack slotItem =
                clickedSlot.getItem();

        ItemStack carried =
                this.getCarried();


        /*
         * =========================================================
         * ПОЛУЧАЕМ ATTACHMENT
         * =========================================================
         */

        ChainmailAttachment attachment =
                player.getAttachedOrCreate(
                        ModAttachments.CHAINMAIL
                );


        ItemStack attachedChainmail =
                attachment.get(
                        equipmentSlot
                );


        /*
         * =========================================================
         * СЦЕНАРИЙ №1
         *
         * СНЯТИЕ БРОНИ С КОЛЬЧУГОЙ ПОД НЕЙ
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
                && ChainmailUtil.isArmor(slotItem)
                && !attachedChainmail.isEmpty()
                && carried.isEmpty()) {

            /*
             * Кладём броню на курсор.
             */
            this.setCarried(
                    slotItem.copy()
            );


            /*
             * Возвращаем кольчугу
             * в ArmorSlot.
             */
            clickedSlot.set(
                    attachedChainmail.copy()
            );


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
             * Отменяем ванильную обработку.
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
             * в Attachment.
             */
            player.setAttached(
                    ModAttachments.CHAINMAIL,
                    attachment.with(
                            equipmentSlot,
                            slotItem.copy()
                    )
            );


            /*
             * Устанавливаем броню
             * в ArmorSlot.
             */
            clickedSlot.set(
                    carried.copy()
            );


            /*
             * Очищаем курсор.
             */
            this.setCarried(
                    ItemStack.EMPTY
            );


            /*
             * Отменяем ванильный клик.
             */
            ci.cancel();
            return;
        }


        /*
         * =========================================================
         * СЦЕНАРИЙ №3
         *
         * ЗАПРЕТ НАДЕВАНИЯ КОЛЬЧУГИ ПОВЕРХ БРОНИ,
         * ЕСЛИ В ATTACHMENT УЖЕ ЕСТЬ КОЛЬЧУГА
         * =========================================================
         */

        if (!slotItem.isEmpty()
                && ChainmailUtil.isArmor(slotItem)
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
         * СЦЕНАРИЙ №4
         *
         * ЗАПРЕТ НАДЕВАНИЯ КОЛЬЧУГИ В ПУСТОЙ ARMOR SLOT,
         * ЕСЛИ В ATTACHMENT УЖЕ ЕСТЬ КОЛЬЧУГА
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
         * ВСЕ ОСТАЛЬНЫЕ СЛУЧАИ
         *
         * Передаём ванильной механике.
         * =========================================================
         */
    }
}