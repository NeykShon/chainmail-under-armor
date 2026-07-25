package ru.neykshon.chainmailunderarmor.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public record ChainmailAttachment(
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots
) {

    /*
     * ВАЖНО: без Codec attachment регистрируется как непостоянный
     * (не сохраняется в NBT) и не переносится на новый инстанс
     * ServerPlayer при респавне/релогине/переходе между мирами.
     * ItemStack.OPTIONAL_CODEC используется вместо ItemStack.CODEC,
     * т.к. поля могут быть ItemStack.EMPTY.
     *
     * Имя и сигнатура ItemStack.OPTIONAL_CODEC заявлены по общей
     * практике API в современных версиях — сверьте по автокомплиту
     * в IDE под вашу 26.1, если что-то не совпадёт, имя могло
     * смениться.
     */
    public static final Codec<ChainmailAttachment> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ItemStack.OPTIONAL_CODEC.fieldOf("helmet").forGetter(ChainmailAttachment::helmet),
                    ItemStack.OPTIONAL_CODEC.fieldOf("chestplate").forGetter(ChainmailAttachment::chestplate),
                    ItemStack.OPTIONAL_CODEC.fieldOf("leggings").forGetter(ChainmailAttachment::leggings),
                    ItemStack.OPTIONAL_CODEC.fieldOf("boots").forGetter(ChainmailAttachment::boots)
            ).apply(instance, ChainmailAttachment::new)
    );

    public static ChainmailAttachment empty() {
        return new ChainmailAttachment(
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY
        );
    }

    public ItemStack get(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> helmet;
            case CHEST -> chestplate;
            case LEGS -> leggings;
            case FEET -> boots;
            default -> ItemStack.EMPTY;
        };
    }

    public ChainmailAttachment with(
            EquipmentSlot slot,
            ItemStack stack
    ) {
        return switch (slot) {
            case HEAD -> new ChainmailAttachment(
                    stack,
                    chestplate,
                    leggings,
                    boots
            );

            case CHEST -> new ChainmailAttachment(
                    helmet,
                    stack,
                    leggings,
                    boots
            );

            case LEGS -> new ChainmailAttachment(
                    helmet,
                    chestplate,
                    stack,
                    boots
            );

            case FEET -> new ChainmailAttachment(
                    helmet,
                    chestplate,
                    leggings,
                    stack
            );

            default -> this;
        };
    }
}