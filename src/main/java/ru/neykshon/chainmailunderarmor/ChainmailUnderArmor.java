package ru.neykshon.chainmailunderarmor;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.neykshon.chainmailunderarmor.item.ModItems;
import ru.neykshon.chainmailunderarmor.attachment.ModAttachments;

public class ChainmailUnderArmor implements ModInitializer {

    public static final String MOD_ID = "chainmail-under-armor";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ModAttachments.initialize();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}