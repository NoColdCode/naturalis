package dev.naturalis.item;

import dev.naturalis.util.MorphDataUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class FilledEchoVialItem extends Item {

    public FilledEchoVialItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Component mobName = MorphDataUtil.getMobDisplayName(stack);
        String mobIdText = MorphDataUtil.getMobId(stack);
        if (mobName != null) {
            tooltip.add(Component.translatable("tooltip.naturalis.contains", mobName));
        }
        if (mobIdText != null && !mobIdText.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.naturalis.mob_id", mobIdText)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
