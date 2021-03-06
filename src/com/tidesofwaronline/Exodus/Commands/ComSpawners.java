package com.tidesofwaronline.Exodus.Commands;

import org.bukkit.entity.Player;

import com.tidesofwaronline.Exodus.CustomEntity.Spawner.CustomEntitySpawnerIndex;
import com.tidesofwaronline.Exodus.Player.ExoPlayer;

public class ComSpawners extends Command {

	public ComSpawners(CommandPackage comPackage) {
		
		Player player = comPackage.getPlayer();
		
		if (!ExoPlayer.getExodusPlayer(player).isShowingSpawners()) {
			CustomEntitySpawnerIndex.showSpawners(player);
		} else {
			CustomEntitySpawnerIndex.hideSpawners(player);
		}
	}

}
