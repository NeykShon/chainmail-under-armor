package ru.neykshon.chainmailunderarmor.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;

public class ModAttachments {

    /*
     * copyOnDeath() убран: на практике он ломал соответствие между
     * моментом, когда LivingEntityMixin чистит Attachment в
     * dropAllDeathLoot(), и моментом, когда фреймворк копировал
     * его на нового ServerPlayer при респавне — из-за этого
     * "воскресала" уже сброшенная кольчуга и возникали дубли.
     * Без copyOnDeath новый игрок просто получает пустой Attachment
     * при респавне, что и требуется: если предметы уже задропались
     * в dropAllDeathLoot (или сохранены при keepInventory — см.
     * ниже), копировать через фреймворк уже нечего.
     *
     * persistent(CODEC) на практике переживает relog корректно.
     */
    public static final AttachmentType<ChainmailAttachment> CHAINMAIL =
            AttachmentRegistry.<ChainmailAttachment>builder()
                    .initializer(ChainmailAttachment::empty)
                    .persistent(ChainmailAttachment.CODEC)
                    .buildAndRegister(ChainmailUnderArmor.id("chainmail"));

    public static void initialize() {
        ChainmailUnderArmor.LOGGER.info(
                "Registering attachments for " + ChainmailUnderArmor.MOD_ID
        );
    }
}