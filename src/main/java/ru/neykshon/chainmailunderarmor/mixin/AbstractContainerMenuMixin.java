package ru.neykshon.chainmailunderarmor.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;
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

    @Unique
    private EquipmentSlot chainmailUnderArmor$pendingSlot;

    @Unique
    private ItemStack chainmailUnderArmor$pendingChainmail = ItemStack.EMPTY;

    @Unique
    private ItemStack chainmailUnderArmor$pendingArmorBefore = ItemStack.EMPTY;

    @Unique
    private int chainmailUnderArmor$pendingSlotIndex = -1;

    @Inject(
            method = "doClick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void chainmailUnderArmor$beforeClick(
            int slotIndex,
            int buttonNum,
            ContainerInput containerInput,
            Player player,
            CallbackInfo ci
    ) {
        // Сбрасываем состояние предыдущего клика на всякий случай.
        this.chainmailUnderArmor$pendingSlot = null;
        this.chainmailUnderArmor$pendingChainmail = ItemStack.EMPTY;
        this.chainmailUnderArmor$pendingArmorBefore = ItemStack.EMPTY;
        this.chainmailUnderArmor$pendingSlotIndex = -1;

        if (slotIndex < 0) {
            return;
        }

        Slot clickedSlot = this.getSlot(slotIndex);

        if (!(clickedSlot instanceof ArmorSlotAccessor armorSlotAccessor)) {
            // Обработку клика по обычному инвентарному слоту
            // (перенос брони из инвентаря поверх открытой кольчуги)
            // делаем отдельно.
            this.chainmailUnderArmor$handleEquipFromInventory(
                    slotIndex, clickedSlot, player, ci
            );
            return;
        }

        EquipmentSlot equipmentSlot =
                armorSlotAccessor.chainmailUnderArmor$getSlot();

        ChainmailAttachment attachment =
                player.getAttachedOrCreate(ModAttachments.CHAINMAIL);

        ItemStack attachedChainmail = attachment.get(equipmentSlot);
        ItemStack slotItem = clickedSlot.getItem();

        /*
         * =================================================================
         * САМОВОССТАНОВЛЕНИЕ №1: слот пуст, а Attachment — нет.
         * =================================================================
         *
         * Такое возможно, только если где-то раньше произошла
         * рассинхронизация (например, из-за необработанного типа
         * клика в старой версии мода). Возвращаем кольчугу туда,
         * где она должна быть, ПЕРЕД обработкой текущего клика —
         * иначе игрок будет заблокирован.
         */
        if (slotItem.isEmpty() && !attachedChainmail.isEmpty()) {

            ChainmailUnderArmor.LOGGER.warn(
                    "Attachment desync for {}: slot is empty but "
                            + "attachment holds {} — restoring it into the slot",
                    equipmentSlot, attachedChainmail
            );

            clickedSlot.set(attachedChainmail.copy());

            player.setAttached(
                    ModAttachments.CHAINMAIL,
                    attachment.with(equipmentSlot, ItemStack.EMPTY)
            );

            slotItem = clickedSlot.getItem();
            attachedChainmail = ItemStack.EMPTY;
            attachment = player.getAttachedOrCreate(ModAttachments.CHAINMAIL);
        }

        /*
         * =================================================================
         * САМОВОССТАНОВЛЕНИЕ №2: кольчуга видна в слоте и
         * дополнительно спрятана в Attachment одновременно.
         * =================================================================
         *
         * Физический слот считаем источником истины,
         * устаревшую запись в Attachment сбрасываем.
         */
        if (ChainmailUtil.isChainmailForSlot(slotItem, equipmentSlot)
                && !attachedChainmail.isEmpty()) {

            ChainmailUnderArmor.LOGGER.warn(
                    "Attachment desync for {}: chainmail present in both "
                            + "the slot and the attachment — clearing the "
                            + "stale attachment entry",
                    equipmentSlot
            );

            player.setAttached(
                    ModAttachments.CHAINMAIL,
                    attachment.with(equipmentSlot, ItemStack.EMPTY)
            );

            attachedChainmail = ItemStack.EMPTY;
        }

        /*
         * =================================================================
         * СЦЕНАРИЙ: НАДЕВАНИЕ БРОНИ ПОВЕРХ ОТКРЫТО ЛЕЖАЩЕЙ КОЛЬЧУГИ
         * =================================================================
         *
         * Ванильная механика этого не умеет: она просто поменяет
         * местами курсор и слот, и кольчуга уедет на курсор вместо
         * Attachment. Обрабатываем вручную, только когда действие
         * это однозначно позволяет сделать безопасно — обычный
         * ЛКМ/ПКМ (PICKUP) с бронёй на курсоре.
         */
        if (ChainmailUtil.isChainmailForSlot(slotItem, equipmentSlot)
                && attachedChainmail.isEmpty()
                && containerInput == ContainerInput.PICKUP) {

            ItemStack carried = this.getCarried();

            if (ChainmailUtil.isArmor(carried)) {

                ItemStack armorToWear = carried.copy();
                ItemStack chainmailToStore = slotItem.copy();

                player.setAttached(
                        ModAttachments.CHAINMAIL,
                        attachment.with(equipmentSlot, chainmailToStore)
                );

                clickedSlot.set(armorToWear);
                this.setCarried(ItemStack.EMPTY);

                ci.cancel();
                return;
            }
        }

        /*
         * =================================================================
         * ЗАЩИТА СПРЯТАННОЙ КОЛЬЧУГИ ОТ ЛЮБОГО ДРУГОГО ВЗАИМОДЕЙСТВИЯ
         * =================================================================
         *
         * В слоте лежит обычная броня, под ней в Attachment —
         * кольчуга. Мы просто прячем Attachment на время
         * ванильной обработки и
         * разбираемся с результатом в TAIL-инъекции.
         */
        if (ChainmailUtil.isArmor(slotItem) && !attachedChainmail.isEmpty()) {

            this.chainmailUnderArmor$pendingSlot = equipmentSlot;
            this.chainmailUnderArmor$pendingChainmail = attachedChainmail.copy();
            this.chainmailUnderArmor$pendingArmorBefore = slotItem.copy();
            this.chainmailUnderArmor$pendingSlotIndex = slotIndex;

            player.setAttached(
                    ModAttachments.CHAINMAIL,
                    attachment.with(equipmentSlot, ItemStack.EMPTY)
            );

            // НЕ отменяем клик — ванильная механика должна отработать
            // как для обычной брони, каким бы ни был тип клика.
        }
    }

    @Inject(method = "doClick", at = @At("TAIL"))
    private void chainmailUnderArmor$afterClick(
            int slotIndex,
            int buttonNum,
            ContainerInput containerInput,
            Player player,
            CallbackInfo ci
    ) {
        if (this.chainmailUnderArmor$pendingSlot == null) {
            return;
        }

        EquipmentSlot equipmentSlot = this.chainmailUnderArmor$pendingSlot;
        ItemStack chainmailToRestore = this.chainmailUnderArmor$pendingChainmail;
        ItemStack armorBefore = this.chainmailUnderArmor$pendingArmorBefore;
        int pendingSlotIndex = this.chainmailUnderArmor$pendingSlotIndex;

        this.chainmailUnderArmor$pendingSlot = null;
        this.chainmailUnderArmor$pendingChainmail = ItemStack.EMPTY;
        this.chainmailUnderArmor$pendingArmorBefore = ItemStack.EMPTY;
        this.chainmailUnderArmor$pendingSlotIndex = -1;

        Slot armorSlot = this.getSlot(pendingSlotIndex);

        if (!(armorSlot instanceof ArmorSlotAccessor accessor)
                || accessor.chainmailUnderArmor$getSlot() != equipmentSlot) {

            // Слот больше не тот, который мы прятали (не должно
            // происходить, но на всякий случай не теряем предмет).
            chainmailUnderArmor$returnChainmailSafely(
                    player, equipmentSlot, chainmailToRestore
            );
            return;
        }

        ItemStack slotItemAfter = armorSlot.getItem();

        ChainmailAttachment attachment =
                player.getAttachedOrCreate(ModAttachments.CHAINMAIL);

        if (slotItemAfter.isEmpty()) {

            /*
             * Броня покинула слот — не важно, каким образом
             * (снята на курсор, quick-move, выброшена через Q,
             * свапнута на хотбар). Спрятанная кольчуга занимает
             * освободившееся место.
             */
            armorSlot.set(chainmailToRestore);

            player.setAttached(
                    ModAttachments.CHAINMAIL,
                    attachment.with(equipmentSlot, ItemStack.EMPTY)
            );

        } else if (ItemStack.isSameItemSameComponents(slotItemAfter, armorBefore)
                && slotItemAfter.getCount() == armorBefore.getCount()) {

            /*
             * Ничего не изменилось — клик не привёл к перемещению
             * (например, инвентарь для quick-move был полон).
             * Возвращаем кольчугу обратно в Attachment как есть.
             */
            player.setAttached(
                    ModAttachments.CHAINMAIL,
                    attachment.with(equipmentSlot, chainmailToRestore)
            );

        } else if (ChainmailUtil.isArmor(slotItemAfter)) {

            /*
             * В слоте оказалась другая обычная броня (например,
             * свап с хотбаром). Кольчуга остаётся спрятанной уже
             * под ней.
             */
            player.setAttached(
                    ModAttachments.CHAINMAIL,
                    attachment.with(equipmentSlot, chainmailToRestore)
            );

        } else {

            /*
             * В слоте оказалось что-то, под чем прятать кольчугу
             * небезопасно (например, туда попала другая кольчуга).
             * Возвращаем старую кольчугу игроку напрямую, чтобы
             * не потерять предмет и не создать дубликат.
             */
            chainmailUnderArmor$returnChainmailSafely(
                    player, equipmentSlot, chainmailToRestore
            );
        }
    }

    /**
     * Сценарий Shift+ЛКМ по обычной броне В ИНВЕНТАРЕ, когда
     * в соответствующем ArmorSlot прямо сейчас открыто лежит
     * кольчуга. Ванильный quick-move в этом случае либо ничего
     * не сделает (слот занят), либо создаст рассинхронизацию,
     * поэтому обрабатываем вручную и полностью отменяем ванильный
     * клик.
     *
     * Это единственный сценарий, который в принципе не может
     * возникнуть на самом ArmorSlot — здесь клик происходит
     * по слоту обычного инвентаря, поэтому он не пересекается
     * с логикой в chainmailUnderArmor$beforeClick.
     */
    @Unique
    private void chainmailUnderArmor$handleEquipFromInventory(
            int slotIndex,
            Slot clickedSlot,
            Player player,
            CallbackInfo ci
    ) {
        ItemStack clickedItem = clickedSlot.getItem();

        if (clickedItem.isEmpty() || !ChainmailUtil.isArmor(clickedItem)) {
            return;
        }

        EquipmentSlot armorEquipmentSlot =
                ChainmailUtil.getArmorSlot(clickedItem);

        if (armorEquipmentSlot == null) {
            return;
        }

        // Ищем ArmorSlot соответствующего типа перебором. getSlot()
        // за пределами реального количества слотов конкретного меню
        // бросает исключение — это ожидаемо и просто обрывает перебор.
        Slot targetArmorSlot = null;

        for (int i = 0; i < 64; i++) {

            Slot possibleSlot;

            try {
                possibleSlot = this.getSlot(i);
            } catch (Exception ignored) {
                break;
            }

            if (possibleSlot instanceof ArmorSlotAccessor accessor
                    && accessor.chainmailUnderArmor$getSlot() == armorEquipmentSlot) {

                targetArmorSlot = possibleSlot;
                break;
            }
        }

        if (targetArmorSlot == null) {
            // Контейнер без слотов брони игрока — оставляем ванильное поведение.
            return;
        }

        ItemStack currentArmorSlotItem = targetArmorSlot.getItem();

        if (!ChainmailUtil.isChainmailForSlot(currentArmorSlotItem, armorEquipmentSlot)) {
            // В ArmorSlot нет открыто лежащей кольчуги — ванильный Shift+ЛКМ подходит.
            return;
        }

        ChainmailAttachment attachment =
                player.getAttachedOrCreate(ModAttachments.CHAINMAIL);

        ItemStack currentAttachedChainmail = attachment.get(armorEquipmentSlot);

        if (!currentAttachedChainmail.isEmpty()) {
            // Рассинхронизация: и в слоте, и в attachment есть кольчуга.
            // Слот — источник истины, чиним attachment вместо блокировки.
            ChainmailUnderArmor.LOGGER.warn(
                    "Attachment desync for {}: chainmail present in both "
                            + "the slot and the attachment during equip-from-"
                            + "inventory — clearing the stale attachment entry",
                    armorEquipmentSlot
            );
            currentAttachedChainmail = ItemStack.EMPTY;
        }

        player.setAttached(
                ModAttachments.CHAINMAIL,
                attachment.with(armorEquipmentSlot, currentArmorSlotItem.copy())
        );

        targetArmorSlot.set(clickedItem.copy());
        clickedSlot.set(ItemStack.EMPTY);

        // Мы уже полностью выполнили перемещение вручную — не даём
        // ванильному doClick() обработать тот же предмет повторно
        // (дюп/потеря/двойное перемещение).
        ci.cancel();
    }

    @Unique
    private static void chainmailUnderArmor$returnChainmailSafely(
            Player player,
            EquipmentSlot equipmentSlot,
            ItemStack chainmail
    ) {
        ChainmailUnderArmor.LOGGER.warn(
                "Could not reconcile hidden chainmail for {} after a click — "
                        + "dropping it near the player instead of losing "
                        + "or duplicating it",
                equipmentSlot
        );

        player.drop(chainmail, false, false);
    }
}
