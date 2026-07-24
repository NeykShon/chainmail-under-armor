package ru.neykshon.chainmailunderarmor;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainmailUnderArmor implements ModInitializer {

    public static final String MOD_ID = "chainmail-under-armor";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Chainmail Under Armor is initializing!");
    }
}