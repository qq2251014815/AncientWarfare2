package net.shadowmage.ancientwarfare.structure.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.shadowmage.ancientwarfare.core.util.RenderTools;

/*
 * Created by Olivier on 05/02/2015.
 */
public interface IBoxRenderer {
    public void renderBox(EntityPlayer player, ItemStack itemStack, float partialTick);

    public static final class Util {
        private Util() {
        }

        public static void renderBoundingBox(EntityPlayer player, BlockPos min, BlockPos max, float delta) {
            AxisAlignedBB bb = new AxisAlignedBB(min.x, min.y, min.z, max.x + 1, max.y + 1, max.z + 1);
            RenderTools.adjustBBForPlayerPos(bb, player, delta);
            RenderTools.drawOutlinedBoundingBox(bb, 1.f, 1.f, 1.f);
        }

        public static void renderBoundingBox(EntityPlayer player, BlockPos min, BlockPos max, float delta, float r, float g, float b) {
            AxisAlignedBB bb = new AxisAlignedBB(min.x, min.y, min.z, max.x + 1, max.y + 1, max.z + 1);
            RenderTools.adjustBBForPlayerPos(bb, player, delta);
            RenderTools.drawOutlinedBoundingBox(bb, r, g, b);
        }
    }
}
