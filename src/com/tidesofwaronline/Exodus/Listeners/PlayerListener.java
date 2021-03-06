package com.tidesofwaronline.Exodus.Listeners;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import com.tidesofwaronline.Exodus.Exodus;
import com.tidesofwaronline.Exodus.CustomEntity.CustomEntityHandler;
import com.tidesofwaronline.Exodus.CustomEntity.Spawner.CustomEntitySpawnerIndex;
import com.tidesofwaronline.Exodus.Player.ExoPlayer;
import com.tidesofwaronline.Exodus.Player.ExperienceHandler;
import com.tidesofwaronline.Exodus.Player.ExoPlayer.ExoGameMode;

public class PlayerListener implements Listener {

	@SuppressWarnings("unused")
	private final Exodus plugin;

	public PlayerListener(final Exodus exodus) {
		this.plugin = exodus;
	}

	@EventHandler
	public void onSneakToggle(final PlayerToggleSneakEvent event) {
		ExoPlayer.getExodusPlayer(event.getPlayer()).combatSwitchCheck();
	}

	@EventHandler
	public void playerInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getType() == InventoryType.CRAFTING
				|| event.getInventory().getType() == InventoryType.CREATIVE) {
			ExoPlayer.getExodusPlayer((Player) event.getWhoClicked())
					.doWeaponSwap(event);
		}
	}

	@EventHandler
	public static void changeExp(final PlayerExpChangeEvent event) {
		ExperienceHandler.changeExp(event);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void hitMob(EntityDamageByEntityEvent event) {

		if (event.isCancelled()) {
			return;
		}

		if (!(event.getEntity() instanceof LivingEntity)) {
			return;
		}

		if (!(event.getDamager() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getDamager();
		ExoPlayer exop = ExoPlayer.getExodusPlayer(player);
		LivingEntity entity = (LivingEntity) event.getEntity();
		ItemStack item = player.getItemInHand();
		
		if (item.getType().getMaxDurability() > 0
				&& item.getType().getMaxDurability() - item.getDurability() <= 5) {
			item.setDurability((short) (item.getType().getMaxDurability() - 2));
			event.setCancelled(true);
			player.sendMessage("This item is broken and cannot be used!");
			player.updateInventory();
			return;
		}

		int damage = exop.getMeleeDamage();

		event.setDamage(0);
		
		if (entity.getHealth() - damage < 1) {
			entity.setHealth(0);
		} else {
			//entity.damage(damage);
			entity.setHealth(entity.getHealth() - damage);
		}

		exop.onHit(entity);

		if (CustomEntityHandler.getCustomEntity(entity) != null) {
			if (!event.getEntity().isDead()) {
				String msg = "Level: ";
				msg += String.valueOf(CustomEntityHandler.getCustomEntity(
						entity).getLevel());
				msg += " | ";
				if (entity.getHealth() > 0) {
					msg += (entity.getHealth()) + "/"
							+ entity.getMaxHealth() + " HP | ";
				} else {
					msg += "Dead! | ";
				}

				msg += "Damage done: " + damage;

				player.sendMessage(msg);
			} else {
				player.sendMessage("Level: " + "1" + " | " + entity.getHealth()
						+ "/" + entity.getMaxHealth() + " HP | Damage done: "
						+ damage);
			}
		}
	}

	@EventHandler
	public static void takeDamage(EntityDamageByEntityEvent event) {

		if (event.isCancelled()) {
			return;
		}

		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		ExoPlayer.getExodusPlayer((Player) event.getEntity()).onDamage(
				event.getDamager());
	}

	@EventHandler
	public static void onBlockDamage(BlockDamageEvent event) {
		if (ExoPlayer.getExodusPlayer(event.getPlayer()).getExoGameMode() == ExoGameMode.COMBAT) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public static void onBlockBreak(BlockBreakEvent event) {
		if (ExoPlayer.getExodusPlayer(event.getPlayer()).getExoGameMode() == ExoGameMode.COMBAT) {
			event.setCancelled(true);
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public static void toolDamage(BlockBreakEvent event) {
		Player player = event.getPlayer();
		ItemStack item = player.getItemInHand();
		if (item.getType().getMaxDurability() > 0
				&& item.getType().getMaxDurability() - item.getDurability() <= 5) {
			item.setDurability((short) (item.getType().getMaxDurability() - 2));
			event.setCancelled(true);
			player.sendMessage("This item is broken and cannot be used!");
			player.updateInventory();
		}
	}

	@EventHandler
	public static void onPortal(PlayerPortalEvent event) {
		event.setCancelled(true);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public static void shootBow(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			ExoPlayer exo = ExoPlayer.getExodusPlayer(player);
			if (exo.getExoGameMode() == ExoGameMode.COMBAT) {
				player.getInventory().setItem(2, exo.getEquippedarrow());
				player.getInventory().getItem(1).setDurability((short) 0);
				player.updateInventory();
			} else {
				event.setCancelled(true);
				player.sendMessage("You must be in Combat Mode to use bows!");
				player.updateInventory();
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onDropItem(PlayerDropItemEvent event) {
		if (ExoPlayer.getExodusPlayer(event.getPlayer()).getExoGameMode() == ExoGameMode.COMBAT) {
			event.setCancelled(true);
			event.getPlayer().updateInventory();
		}
	}

	@EventHandler
	public void onArrowHit(ProjectileHitEvent event) {
		//if (event.getEntity() instanceof Arrow) {
		//	Arrow arrow = (Arrow) event.getEntity();
		//	arrow.remove();
		//}
	}

	@EventHandler
	public void onPickup(PlayerPickupItemEvent event) {

		ExoPlayer exo = ExoPlayer.getExodusPlayer(event.getPlayer());

		if (exo.getExoGameMode() == ExoGameMode.COMBAT) {
			exo.addToInventory(event.getItem().getItemStack());
			event.getItem().remove();
			event.setCancelled(true);
		}

		if (event.getItem().getType() == EntityType.ARROW) { //Deny Pickup of Arrows
			event.setCancelled(true);
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		ExoPlayer exo = ExoPlayer.getExodusPlayer(event.getPlayer());
		if (event.getAction() == Action.LEFT_CLICK_AIR
				|| event.getAction() == Action.LEFT_CLICK_BLOCK
				|| event.getAction() == Action.RIGHT_CLICK_AIR
				|| event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (exo.getExoGameMode() == ExoGameMode.COMBAT) {
				ItemStack block = event.getPlayer().getItemInHand();
				if (block.getTypeId() == 0) {
					return;
				}
				if (event.getPlayer().getInventory().getHeldItemSlot() >= 3
						&& block.getTypeId() != 0) {
					exo.cast(block.getItemMeta().getDisplayName(),
							event.getAction());
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void clickSpawner(PlayerInteractEvent event) {
		if (event.getClickedBlock() != null
				&& event.getClickedBlock().getType() == Material.MOB_SPAWNER) {
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				CustomEntitySpawnerIndex.leftClick(event.getPlayer(),
						event.getClickedBlock());
			}
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				CustomEntitySpawnerIndex.rightClick(event.getPlayer(),
						event.getClickedBlock());
			}
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		event.getDrops().clear();
		ExoPlayer.getExodusPlayer(event.getEntity()).setExoGameMode(ExoGameMode.BUILD);
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
	}
}
