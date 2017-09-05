//TODO ftb lib / utils changes
package net.shadowmage.ancientwarfare.core.interop;

import com.feed_the_beast.ftbl.api.IForgePlayer;
import com.feed_the_beast.ftbl.api.NotificationId;
import com.feed_the_beast.ftbl.lib.Notification;
import com.feed_the_beast.ftbu.api.chunks.IClaimedChunk;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.gamedata.ChunkClaimWorkerThread;
import net.shadowmage.ancientwarfare.core.gamedata.ChunkClaims;
import net.shadowmage.ancientwarfare.core.gamedata.ChunkClaims.ChunkClaimInfo;
import net.shadowmage.ancientwarfare.core.gamedata.ChunkClaims.TownHallEntry;

import java.util.List;
import java.util.UUID;

public class InteropFtbu extends InteropFtbuDummy {
    
    public static final ChunkClaimWorkerThread WORKER_THREAD = new ChunkClaimWorkerThread();
    
    @Override
    public boolean areFriends(String player1, String player2) {
        return FriendsAPI.areFriends(player1, player2);
    }
    
    public boolean isFriendOfClient(UUID otherPlayer) { 
        return FriendsAPI.isClientFriend(otherPlayer);
    }
    
    @Override
    public void addClaim(World world, EntityLivingBase placer, int posX, int posY, int posZ) {
        addClaim(world, placer.getName(), posX, posY, posZ);
    }
    
    @Override
    public void addClaim(World world, String ownerName, int posX, int posY, int posZ) {
        LMPlayerServer p = LMWorldServer.inst.getPlayer(ownerName);
        if (p != null) {
            Chunk newChunkClaim = world.getChunkFromBlockCoords(posX, posZ);
            ChunkClaimInfo newChunkClaimInfo = new ChunkClaimInfo(newChunkClaim.xPosition, newChunkClaim.zPosition, world.provider.getDimension());
            ChunkClaims.get(world).addTownHallEntry(newChunkClaimInfo, new TownHallEntry(ownerName, posX, posY, posZ));
        } else {
            AncientWarfareCore.log.error("A chunk claim action was requested at " + posX + "x" + posY + "x" + posZ + " but the player name '" + ownerName + "' could not be found...");
        }
    }
    
    @Override
    public void notifyPlayer(TextFormatting titleColor, String ownerName, String title, TextComponentTranslation msg, List<TextComponentTranslation> hoverTextLines) {
        if (ownerName.isEmpty() || LMWorldServer.inst == null)
            return;
        
        LMPlayerServer p = LMWorldServer.inst.getPlayer(ownerName);
        
        if (p != null) {
            ITextComponent cc = new TextComponentTranslation(title);
            cc.getStyle().setColor(titleColor);

            Notification n = new Notification(new NotificationId(new ResourceLocation(AncientWarfareCore.modID, "claim_change"), p.getMCWorld().getTotalWorldTime(), cc, 6000);

            n.addText(cc);
            n.addText(msg);
            n.setTimer(6000);

            MouseAction mouse = new MouseAction();
            for(ITextComponent hoverTextLine : hoverTextLines)
                mouse.hover.add(hoverTextLine);
            n.setMouseAction(mouse);
            
            if (p.getPlayer() != null)
                FTBLib.notifyPlayer(p.getPlayer(), n);
        } else {
            // TODO
            System.out.println("Target player is not online!");
        }
    }
    
    @Override
    public IForgePlayer getChunkClaimOwner(int dimId, int chunkX, int chunkY) {
        IClaimedChunk claim = LMWorldServer.inst.claimedChunks.getChunk(dimId, chunkX, chunkY);
        if (claim == null)
            return null;
        return claim.getOwner();
    } 

    @Override
    public void startWorkerThread() {
        WORKER_THREAD.enable();
    };
    
    @Override
    public void stopWorkerThread() {
        WORKER_THREAD.disable();
    };
    
}
