package ru.neykshon.chainmailunderarmor.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import ru.neykshon.chainmailunderarmor.ChainmailUnderArmor;

public class ModAttachments {

    public static final AttachmentType<ChainmailAttachment> CHAINMAIL =
            AttachmentRegistry.createDefaulted(
                    ChainmailUnderArmor.id("chainmail"),
                    ChainmailAttachment::empty
            );

    public static void initialize() {
        ChainmailUnderArmor.LOGGER.info(
                "Registering attachments for " + ChainmailUnderArmor.MOD_ID
        );
    }
}