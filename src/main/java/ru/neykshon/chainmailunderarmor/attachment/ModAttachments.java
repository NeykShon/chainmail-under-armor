package ru.neykshon.chainmailunderarmor.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;

public class ModAttachments {

    /*
     * ВАЖНО: createDefaulted(...) без builder/persistent создаёт
     * НЕПОСТОЯННЫЙ attachment — он не сохраняется в NBT и не
     * копируется на новый инстанс ServerPlayer при респавне или
     * релогине. Для attachment'а, представляющего реально надетый
     * предмет, это утечка данных при первом же relog/смерти без
     * keepInventory (LivingEntityMixin явно чистит его при смерти,
     * но между смертью и релогином/переходом в другое измерение
     * окно для потери всё ещё есть).
     *
     * Ниже — builder-вариант с persistent(CODEC) и копированием
     * на респавн. Название билдер-методов ниже основано на общей
     * практике Fabric API последних лет и НЕ проверено компиляцией
     * под вашу 26.1 (у меня нет доступа к её Maven-репозиторию).
     * Проверьте автокомплит в IDE — если метод называется иначе,
     * логика (persistent codec + copy-on-respawn) должна остаться
     * той же.
     */

    public static final AttachmentType<ChainmailAttachment> CHAINMAIL =
            AttachmentRegistry.<ChainmailAttachment>builder()
                    .initializer(ChainmailAttachment::empty)
                    .persistent(ChainmailAttachment.CODEC)
                    .buildAndRegister(
                            ChainmailUnderArmor.id("chainmail")
                    );

//    public static final AttachmentType<ChainmailAttachment> CHAINMAIL =
//            AttachmentRegistry.<ChainmailAttachment>builder()
//                    .initializer(ChainmailAttachment::empty)
//                    .persistent(ChainmailAttachment.CODEC)
//                    .copyOnDeath()
//                    .buildAndRegister(ChainmailUnderArmor.id("chainmail"));

    public static void initialize() {
        ChainmailUnderArmor.LOGGER.info(
                "Registering attachments for " + ChainmailUnderArmor.MOD_ID
        );
    }
}