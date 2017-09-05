package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableTile;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.inventory.InventoryBasic;
import net.shadowmage.ancientwarfare.core.item.ItemResearchBook;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nonnull;

public class TileEngineeringStation extends TileUpdatable implements IRotatableTile, IInventoryChangedListener {

    EnumFacing facing = EnumFacing.NORTH;
    ItemStack[] matrixShadow;

    public InventoryCrafting layoutMatrix;
    public InventoryCraftResult result;
    public InventoryBasic bookInventory;
    public InventoryBasic extraSlots;

    public TileEngineeringStation() {
        Container c = new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer var1) {
                return true;
            }

            @Override
            public void onCraftMatrixChanged(IInventory par1iInventory) {
                onInventoryChanged(null);
            }
        };
        layoutMatrix = new InventoryCrafting(c, 3, 3);
        matrixShadow = new ItemStack[layoutMatrix.getSizeInventory()];
        bookInventory = new InventoryBasic(1, this);
        result = new InventoryCraftResult();
        extraSlots = new InventoryBasic(18, this);
    }

    public String getCrafterName() {
        return ItemResearchBook.getResearcherName(bookInventory.getStackInSlot(0));
    }

    /*
     * called to shadow a copy of the input matrix, to know what to refill
     */
    public void preItemCrafted() {
        @Nonnull ItemStack stack;
        for (int i = 0; i < layoutMatrix.getSizeInventory(); i++) {
            stack = layoutMatrix.getStackInSlot(i);
            matrixShadow[i] = stack.isEmpty() ? null : stack.copy();
        }
    }

    public void onItemCrafted() {
        @Nonnull ItemStack layoutStack;
        for (int i = 0; i < layoutMatrix.getSizeInventory(); i++) {
            layoutStack = matrixShadow[i];
            if (layoutStack == null) {
                continue;
            }
            if (!layoutMatrix.getStackInSlot(i).isEmpty()) {
                continue;
            }
            layoutMatrix.setInventorySlotContents(i, InventoryTools.removeItems(extraSlots,  null, layoutStack, 1));
        }
    }

    private void onLayoutMatrixChanged() {
        this.result.setInventorySlotContents(0, AWCraftingManager.INSTANCE.findMatchingRecipe(layoutMatrix, world, getCrafterName()));
    }

    @Override
    public void onInventoryChanged(IInventory internal){
        onLayoutMatrixChanged();
        markDirty();
    }

    @Override
    public void setWorld(World world){
        super.setWorld(world);
        onLayoutMatrixChanged();
    }

    @Override
    protected void writeUpdateNBT(NBTTagCompound tag) {
        super.writeUpdateNBT(tag);
        tag.setInteger("facing", facing.ordinal());
    }

    @Override
    protected void handleUpdateNBT(NBTTagCompound tag) {
        super.handleUpdateNBT(tag);
        facing = EnumFacing.VALUES[tag.getInteger("facing")];
        BlockTools.notifyBlockUpdate(this);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        InventoryTools.readInventoryFromNBT(bookInventory, tag.getCompoundTag("bookInventory"));
        InventoryTools.readInventoryFromNBT(extraSlots, tag.getCompoundTag("extraInventory"));
        InventoryTools.readInventoryFromNBT(result, tag.getCompoundTag("resultInventory"));
        InventoryTools.readInventoryFromNBT(layoutMatrix, tag.getCompoundTag("layoutMatrix"));
        facing = EnumFacing.values()[tag.getInteger("facing")];
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        NBTTagCompound inventoryTag = new NBTTagCompound();
        InventoryTools.writeInventoryToNBT(bookInventory, inventoryTag);
        tag.setTag("bookInventory", inventoryTag);

        inventoryTag = new NBTTagCompound();
        InventoryTools.writeInventoryToNBT(extraSlots, inventoryTag);
        tag.setTag("extraInventory", inventoryTag);

        inventoryTag = new NBTTagCompound();
        InventoryTools.writeInventoryToNBT(result, inventoryTag);
        tag.setTag("resultInventory", inventoryTag);

        inventoryTag = new NBTTagCompound();
        InventoryTools.writeInventoryToNBT(layoutMatrix, inventoryTag);
        tag.setTag("layoutMatrix", inventoryTag);

        tag.setInteger("facing", facing.ordinal());
        return tag;
    }

    @Override
    public EnumFacing getPrimaryFacing() {
        return facing;
    }

    @Override
    public void setPrimaryFacing(EnumFacing face) {
        this.facing = face;
        BlockTools.notifyBlockUpdate(this);
    }

    public void onBlockBreak(){
        InventoryTools.dropInventoryInWorld(world, bookInventory, pos);
        InventoryTools.dropInventoryInWorld(world, extraSlots, pos);
        InventoryTools.dropInventoryInWorld(world, layoutMatrix, pos);
    }
}
