package com.tidesofwaronline.Exodus.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.kitteh.tag.TagAPI;

import com.tidesofwaronline.Exodus.Exodus;
import com.tidesofwaronline.Exodus.Buffs.Buff;
import com.tidesofwaronline.Exodus.Config.PlayerConfig;
import com.tidesofwaronline.Exodus.CustomItem.CustomItem;
import com.tidesofwaronline.Exodus.CustomItem.CustomItemHandler;
import com.tidesofwaronline.Exodus.Effects.PlayerLevelUpEffect;
import com.tidesofwaronline.Exodus.Quests.Quest;
import com.tidesofwaronline.Exodus.Races.Races.Race;
import com.tidesofwaronline.Exodus.Spells.Spellbook;
import com.tidesofwaronline.Exodus.Spells.Spells.Spell;
import com.tidesofwaronline.Exodus.Util.IconMenu;
import com.tidesofwaronline.Exodus.Util.ItemUtil;
import com.tidesofwaronline.Exodus.Util.MessageUtil;

public class ExoPlayer implements Runnable {

	static Plugin plugin;
	private final Player player;
	private final PlayerConfig config = new PlayerConfig(this);
	private IconMenu statsMenu;
	private Economy econ = Exodus.getEcon();

	//Double Click Stuff
	private long sneakLastPressTime;
	private long inventoryLastPressTime;
	private boolean crouched;
	private static final long DOUBLE_PRESS_INTERVAL = 250;
	Inventory dcInv;
	CustomItem pickeditem;
	int slot;

	//Inventories
	private Inventory buildInventory;
	public CustomItem equippedmelee = new CustomItem(0);
	public CustomItem equippedranged = new CustomItem(0);
	public CustomItem equippedarrow = new CustomItem(Material.ARROW, 64);
	
	//Gameplay
	boolean inCombat = false;
	boolean inDanger = false;
	public int level;
	public Party party = null;
	public Guild guild = null;
	Spellbook spellbook = new Spellbook(this);
	ArrayList<Buff> buffs = new ArrayList<Buff>();
	private ChatColor namecolor = ChatColor.WHITE;
	private ArrayList<Quest> quests = new ArrayList<Quest>();
	
	//Utility
	public boolean showSpawners = false;
	private boolean filter = false;

	public ExoPlayer(final Plugin plugin, final Player player) {

		ExoPlayer.plugin = plugin;
		this.player = player;

		registerPlayer(this);

		buildInventory = Bukkit.createInventory(player, InventoryType.PLAYER);

		config.initialize();

		this.level = config.getConfig().getInt("level", 1);
		this.equippedmelee = config.getEquippedMelee();
		this.equippedranged = config.getEquippedRanged();

		createStatsMenu();
		recalcXPBar();
	}

	public boolean addToInventory(ItemStack i) {
		HashMap<Integer, ItemStack> extra = buildInventory.addItem(i);
		if (extra.size() != 0) {
		}
		return true;
	}

	public void addXP(final int amount) {
		setAttribute("xp", (Integer) getAttribute("xp") + amount);
		gainXPEvent();
	}

	private int calculateHealth() {
		return (int) Math.round((this.level * this.level) * 1.25 + 19);
	}

	public void cast(String spell, Action action) {
		player.sendMessage("Casting");
	}

	private void click(IconMenu.OptionClickEvent event) {
		final String icon = event.getName();

		if (icon.equalsIgnoreCase("increase rogue")) {
			increaseStat("stats.rogue", 1, false);
		}
		if (icon.equalsIgnoreCase("increase ranger")) {
			increaseStat("stats.ranger", 1, false);
		}
		if (icon.equalsIgnoreCase("increase warrior")) {
			increaseStat("stats.warrior", 1, false);
		}
		if (icon.equalsIgnoreCase("increase cleric")) {
			increaseStat("stats.cleric", 1, false);
		}
		if (icon.equalsIgnoreCase("increase mage")) {
			increaseStat("stats.mage", 1, false);
		}
		if (icon.equalsIgnoreCase("increase warlock")) {
			increaseStat("stats.warlock", 1, false);
		}

		refreshOptions();
		statsMenu.rebuildMenu();
	}

	public boolean combatSwitchCheck() {
		// Get current time in nano seconds.
		final long pressTime = System.currentTimeMillis();
		crouched = !crouched;

		if (crouched == false) {//if standing
			// If double click...
			if (pressTime - sneakLastPressTime <= DOUBLE_PRESS_INTERVAL) {
				toggleCombat();
			} else {     // If not double click....
			}
			// record the last time the menu button was pressed.
			sneakLastPressTime = pressTime;
		}
		return true;
	}

	public YamlConfiguration config() {
		return this.config.getConfig();
	}

	private void createStatsMenu() {
		//if (this.statsMenu != null) {this.statsMenu.destroy();}
		//Set up stats menu
		statsMenu = new IconMenu(this.player.getDisplayName() + "'s Stats", 54,
				new IconMenu.OptionClickEventHandler() {
					@Override
					public void onOptionClick(
							final IconMenu.OptionClickEvent event) {
						click(event);
						event.setWillClose(false);
					}
				}, plugin);
		refreshOptions();
	}

	@SuppressWarnings("deprecation")
	public void doWeaponSwap(final InventoryClickEvent event) {
		// Get current time in nano seconds.
		final long pressTime = System.currentTimeMillis();

		if (pressTime - inventoryLastPressTime <= DOUBLE_PRESS_INTERVAL) {

			pickeditem = new CustomItem(event.getCursor());

			if (event.getSlot() == slot) {
				if (pickeditem.getTypeId() == 261) { //if it's a bow
					ItemStack old = equippedranged;
					setRanged(pickeditem);
					player.setItemOnCursor(new ItemStack(0));
					player.getInventory().setItem(slot, old);
					player.updateInventory();
				}

				boolean onlist = false;
				for (int i1 : ItemUtil.weapons) { //if it's a weapon
					if (pickeditem.getTypeId() == i1) {
						onlist = true;
					}
				}
				for (int i1 : ItemUtil.tools) { //if it's a tool
					if (pickeditem.getTypeId() == i1) {
						onlist = true;
					}
				}

				if (onlist == true) {
					ItemStack old = equippedmelee;
					setMelee(pickeditem);
					player.setItemOnCursor(new ItemStack(0));
					player.getInventory().setItem(slot, old);
					player.updateInventory();
				}
			}

		} else {
			slot = event.getSlot();
		}

		inventoryLastPressTime = pressTime;

	}

	private void enterCombat() {
		player.sendMessage("ENTERING COMBAT!");
		inCombat = true;
		inventorySave();
		player.getInventory().clear();

		player.getInventory().setItem(0, equippedmelee);
		player.getInventory().setItem(1, equippedranged);
		player.getInventory().setItem(2, equippedarrow);

		ArrayList<Spell> spells = spellbook.getSpells();

		for (int i = 0; i < 34 && i < spells.size(); i++) {
			player.getInventory().setItem(i + 3, spells.get(i).getItemStack());
		}

	}

	private void exitCombat() {
		player.sendMessage("EXITING COMBAT");
		inCombat = false;
		player.getInventory().setContents(buildInventory.getContents());
	}

	private void gainXPEvent() {
		final int xp = (Integer) getAttribute("xp");
		int level = config.getConfig().getInt("level");
		player.sendMessage("XP this level: "
				+ ExperienceHandler.XPThisLevel(xp, level) + "/"
				+ ExperienceHandler.totalXPThisLevel(level));
		while (ExperienceHandler.XPThisLevel(xp, level) >= ExperienceHandler
				.totalXPThisLevel(level)) {
			levelUp();
			level++;
		}
		recalcXPBar();
		config.set("xp", xp);
	}

	public Object getAttribute(final String string) {
		final Object toreturn = config.getConfig().get(string);
		if (toreturn != null) {
			return toreturn;
		}
		return 0;
	}

	public Inventory getBuildInventory() {
		return buildInventory;
	}
	
	public Economy econ() {
		return econ;
	}

	public Guild getGuild() {
		return this.guild;
	}

	public int getMeleeDamage() {
		if (this.isInCombat()) {
			ItemStack is = player.getInventory().getItemInHand();
			if (CustomItemHandler.isCustomItem(is)) {
				CustomItem i = new CustomItem(is);
				return i.getDamage();
			}
		}
		return 1;
	}

	public org.bukkit.ChatColor getNameColor() {
		return this.namecolor;
	}

	public Party getParty() {
		return this.party;
	}

	public Player getPlayer() {
		return player;
	}

	public PlayerConfig getPlayerConfig() {
		return this.config;
	}

	public int[] getReputation() {
		int[] a = new int[8];
		a[0] = Integer.valueOf(getAttribute("repVenturi").toString());
		a[1] = Integer.valueOf(getAttribute("repDia'ab").toString());
		a[2] = Integer.valueOf(getAttribute("repNordic").toString());
		a[3] = Integer.valueOf(getAttribute("repElven").toString());
		a[4] = Integer.valueOf(getAttribute("repAbraxian").toString());
		a[5] = Integer.valueOf(getAttribute("repNagrath").toString());
		a[6] = Integer.valueOf(getAttribute("repScience").toString());
		a[7] = Integer.valueOf(getAttribute("repDwarves").toString());

		Arrays.sort(a);

		return a;
	}

	public Integer getReputation(Race race) {
		switch (race) {
		case VENTURI:
			return Integer.valueOf(getAttribute("repVenturi").toString());
		case DIAAB:
			return Integer.valueOf(getAttribute("repDia'ab").toString());
		case NORDIC:
			return Integer.valueOf(getAttribute("repNordic").toString());
		case ELVEN:
			return Integer.valueOf(getAttribute("repElven").toString());
		case ABRAXIAN:
			return Integer.valueOf(getAttribute("repAbraxian").toString());
		case NAGRATH:
			return Integer.valueOf(getAttribute("repNagrath").toString());
		case SCIENCE:
			return Integer.valueOf(getAttribute("repScience").toString());
		case DWARVES:
			return Integer.valueOf(getAttribute("repDwarves").toString());
		default:
			return null;
		}
	}

	public Spell[] getSpells() {
		Spell[] spells = { Spell.TEST, Spell.HEAL };
		return spells;
	}

	public int getStaminaMax() {
		return (config().getInt("stats.warrior") * 5)
				+ (config().getInt("stats.cleric") * 4)
				+ (config().getInt("stats.warlock") * 3);
	}

	public int getStaminaRegen() {
		return (config().getInt("stats.rogue") * 1)
				+ (config().getInt("stats.ranger") * 1)
				+ (config().getInt("stats.mage") * 1);

	}

	private void increaseStat(final String atr, final int amount,
			final boolean override) {
		if ((Integer) getAttribute(atr) < plugin.getConfig().getInt("statmax")) {
			if (override) {

				modifyAttribute(atr, amount);
			} else if ((Integer) getAttribute("skillpoints") > 0) {
				modifyAttribute(atr, amount);
				modifyAttribute("skillpoints", -1);
			}
		}
		config.save();
	}

	//@SuppressWarnings("deprecation")
	public void inventoryLoad() {
		buildInventory.setContents(config.loadInventory(player).getContents());
		if (!inCombat) {
			player.getInventory().setContents(buildInventory.getContents());
		}
		//player.updateInventory();
	}

	public boolean inventorySave() {
		buildInventory.setContents(player.getInventory().getContents());
		if (config.saveInventory(buildInventory)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isInCombat() {
		return inCombat;
	}

	public void levelUp() {

		this.level++;

		player.sendMessage("Congratulations! You've reached level " + level
				+ "!");

		new PlayerLevelUpEffect(this.player);

		setMaxHP(calculateHealth());

		setAttribute("level", this.level);
		modifyAttribute("skillpoints", 4);
		config.save();
	}

	public void modifyAttribute(final String path, final int value) {
		final int oldvalue = config.getConfig().getInt(path);
		final int newvalue = oldvalue + value;
		config.getConfig().set(path, newvalue);
	}

	public void onDamage(Entity damager) {
		CustomItem i = new CustomItem(player.getInventory().getItemInHand());
		if (CustomItemHandler.isCustomItem(i)) {
			i.onDamage(this.player, damager);
		}
	}

	/* Disabled due to client not sending InventoryOpenEvent
	public boolean inventorySwitchCheck() {
		// Get current time in nano seconds.
		final long pressTime = System.currentTimeMillis();

		//if standing
		// If double click...
		if (pressTime - inventoryLastPressTime <= DOUBLE_PRESS_INTERVAL) {
			openStatsMenu();
		}
		// record the last time the menu button was pressed.
		inventoryLastPressTime = pressTime;

		return true;
	}*/

	public void onHit(LivingEntity entity) {
		CustomItem i = new CustomItem(player.getInventory().getItemInHand());
		if (CustomItemHandler.isCustomItem(i)) {
			i.onHit(this.player, entity);
		}
	}

	public void openStatsMenu() {
		refreshOptions();
		statsMenu.open(this.player);
	}

	public void recalcXPBar() {
		final int level = (Integer) getAttribute("level");
		if (player.getLevel() != level) {
			player.setLevel(level);
		}
		player.setExp(ExperienceHandler.getXPBarPercentage(this));
	}

	public void refreshOptions() {
		statsMenu

				//Misc. Stats
				.setOption(
						//Stats Head
						11,
						new ItemStack(Material.SKULL_ITEM, 1, (short) 3),
						"Level " + getAttribute("level"),
						"Race: " + getAttribute("race"),
						"HP: " + player.getHealth() + "/"
								+ player.getMaxHealth(),
						"XP: "
								+ ExperienceHandler.XPThisLevel(
										(Integer) getAttribute("xp"),
										(Integer) getAttribute("level"))
								+ "/"
								+ ExperienceHandler
										.totalXPThisLevel((Integer) getAttribute("level")))
				.setOption(
						//Skillpoints
						16,
						new ItemStack(Material.BOOK,
								(Integer) getAttribute("skillpoints")),
						"Skill Points - " + getAttribute("skillpoints"),
						"Gain these as you level up,", "Spend them on stats!")

				//Skill Stats
				.setOption(
						//Warrior
						4,
						new ItemStack(Material.IRON_CHESTPLATE, (Integer) this
								.getAttribute("stats.warrior")), "Warrior",
						"Attune to the Warrior to", "hit harder with melee,",
						"equip better armor,", "and tank enemies!")
				.setOption(
						//Rogue
						13,
						new ItemStack(Material.IRON_SWORD, (Integer) this
								.getAttribute("stats.rogue")), "Rogue",
						"Run and fight longer!")
				.setOption(
						//Ranger
						22,
						new ItemStack(Material.BOW, (Integer) this
								.getAttribute("stats.ranger")), "Ranger",
						"Keeps you alive!")

				.setOption(
						//Cleric
						31,
						new ItemStack(Material.NETHER_STAR, (Integer) this
								.getAttribute("stats.cleric")), "Cleric",
						"Modifies your Rogue regeneration rate!")
				.setOption(
						//Mage
						40,
						new ItemStack(Material.BLAZE_POWDER, (Integer) this
								.getAttribute("stats.mage"), (short) 3),
						"Mage", "More mana, more spellpower!")
				.setOption(
						//Warlock
						49,
						new ItemStack(Material.EYE_OF_ENDER, (Integer) this
								.getAttribute("stats.warlock")), "Warlock",
						"Affects mana regen!")

				//Allegiance
				.setOption(10, new ItemStack(Material.GOLD_BLOCK, 1),
						"Venturi", "Reputation: " + getAttribute("repVenturi"))
				.setOption(19, new ItemStack(Material.WOOL, 1, (short) 14),
						"Dia'ab", "Reputation: " + getAttribute("repDia'ab"))
				.setOption(28, new ItemStack(Material.SNOW_BLOCK, 1), "Nordic",
						"Reputation: " + getAttribute("repNordic"))
				.setOption(37, new ItemStack(Material.GLOWSTONE, 1), "Elven",
						"Reputation: " + getAttribute("repElven"));

		//Increase Stat Icon
		if ((Integer) getAttribute("skillpoints") > 0) {
			if ((Integer) getAttribute("stats.warrior") < plugin.getConfig()
					.getInt("statmax", 64)) {
				statsMenu
						.setOption(
								5,
								new ItemStack(Material.INK_SACK, 1, (short) 10),
								"Increase Warrior",
								"Click to increase your",
								"Warrior to "
										+ ((Integer) this
												.getAttribute("stats.warrior") + 1));
			} else {
				statsMenu.setOption(5, new ItemStack(Material.AIR));
			}

			if ((Integer) getAttribute("stats.rogue") < plugin.getConfig()
					.getInt("statmax", 64)) {

				statsMenu
						.setOption(
								14,
								new ItemStack(Material.INK_SACK, 1, (short) 10),
								"Increase Rogue",
								"Click to increase your",
								"Rogue to "
										+ ((Integer) this
												.getAttribute("stats.rogue") + 1));
			} else {
				statsMenu.setOption(14, new ItemStack(Material.AIR));
			}

			if ((Integer) getAttribute("stats.ranger") < plugin.getConfig()
					.getInt("statmax", 64)) {
				statsMenu
						.setOption(
								23,
								new ItemStack(Material.INK_SACK, 1, (short) 10),
								"Increase Ranger",
								"Click to increase your",
								"Ranger to "
										+ ((Integer) this
												.getAttribute("stats.ranger") + 1));
			} else {
				statsMenu.setOption(23, new ItemStack(Material.AIR));
			}

			if ((Integer) getAttribute("stats.cleric") < plugin.getConfig()
					.getInt("statmax", 64)) {
				statsMenu
						.setOption(
								32,
								new ItemStack(Material.INK_SACK, 1, (short) 10),
								"Increase Cleric",
								"Click to increase your",
								"Cleric to "
										+ ((Integer) this
												.getAttribute("stats.cleric") + 1));
			} else {
				statsMenu.setOption(32, new ItemStack(Material.AIR));
			}

			if ((Integer) getAttribute("stats.mage") < plugin.getConfig()
					.getInt("statmax", 64)) {
				statsMenu
						.setOption(
								41,
								new ItemStack(Material.INK_SACK, 1, (short) 10),
								"Increase Mage",
								"Click to increase your",
								"Mage to "
										+ ((Integer) this
												.getAttribute("stats.mage") + 1));
			} else {
				statsMenu.setOption(41, new ItemStack(Material.AIR));
			}

			if ((Integer) getAttribute("stats.warlock") < plugin.getConfig()
					.getInt("statmax", 64)) {
				statsMenu
						.setOption(
								50,
								new ItemStack(Material.INK_SACK, 1, (short) 10),
								"Increase Warlock",
								"Click to increase your",
								"Warlock to "
										+ ((Integer) this
												.getAttribute("stats.warlock") + 1));
			} else {
				statsMenu.setOption(50, new ItemStack(Material.AIR));
			}
		} else { //If Skill Points 0, set all to air
			statsMenu.setOption(5, new ItemStack(Material.AIR))
					.setOption(14, new ItemStack(Material.AIR))
					.setOption(23, new ItemStack(Material.AIR))
					.setOption(32, new ItemStack(Material.AIR))
					.setOption(41, new ItemStack(Material.AIR))
					.setOption(50, new ItemStack(Material.AIR));
		}

		//Melee weapon
		if (equippedmelee != null && equippedmelee.getTypeId() != 0) {
			statsMenu.setOption(20, equippedmelee);
		} else { //No melee equipped
			statsMenu.setOption(20, new ItemStack(Material.IRON_SWORD),
					"No Melee Weapon Equipped!");
		}

		//Ranged weapon
		if (equippedranged != null && equippedranged.getTypeId() != 0) {
			statsMenu.setOption(29, equippedranged);
		} else {
			statsMenu.setOption(29, new ItemStack(Material.BOW),
					"No Ranged Weapon Equipped!");
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	public boolean savePlayer() {
		if (inCombat) {
			setCombat(false);
		}

		if (inventorySave()) { //Saving inventory also saves config
			MessageUtil.log("Player " + this.player.getName() + " saved");
			return true;
		} else {
			return false;
		}
	}

	public void setAttribute(final String path, final Object value) {
		config.getConfig().set(path, value);
	}

	public boolean setCombat(final boolean combat) {
		inCombat = combat;
		if (inCombat) {
			enterCombat();
		} else {
			exitCombat();
		}
		return inCombat;

	}

	public void setInDanger(final boolean danger) {
		inDanger = danger;
	}

	public void setMaxHP(int value) {
		player.setMaxHealth(value);
		player.setHealth(value);
		config().set("maxhp", value);
		config().set("hp", value);
		config.save();
	}

	public void setMelee(CustomItem item) {
		this.equippedmelee = item;
		setAttribute("equippedmelee", item);
		player.sendMessage("Melee Weapon Equipped!");
		config.save();
	}

	public void setRace(String string) {
		if (plugin.getConfig().contains("races." + string)) {
			setAttribute("race", string);
			this.namecolor = ChatColor.getByChar(plugin.getConfig().getString(
					"races." + string + ".color", "f"));
			TagAPI.refreshPlayer(player);
			config.save();
			player.sendMessage("Your race is now the " + string);
		} else {
			player.sendMessage("Race " + string + " does not exist!");
		}

	}

	public void setRanged(final CustomItem item) {
		this.equippedranged = new CustomItem(item);
		setAttribute("equippedranged", item);
		player.sendMessage("Ranged Weapon Equipped!");
	}

	public boolean toggleCombat() {
		if (inDanger == false) {
			if (!inCombat) {
				enterCombat();
			} else {
				exitCombat();
			}
		}
		return inCombat;
	}

	public boolean isFilter() {
		return filter;
	}

	public void setFilter(boolean filter) {
		this.filter = filter;
	}
	
	private static Map<String, ExoPlayer> playerIndex = new HashMap<String, ExoPlayer>();

	public static void registerPlayer(ExoPlayer exodusplayer) {
		playerIndex.put(exodusplayer.getPlayer().getName(), exodusplayer);
	}

	public static ExoPlayer getExodusPlayer(Player player) {
		return playerIndex.get(player.getName());
	}
	
	public static ExoPlayer getExodusPlayer(String player) {
		return playerIndex.get(player);
	}

	public static ExoPlayer getExodusPlayer(HumanEntity player) {
		return playerIndex.get(player.getName());
	}
	
	public static void removePlayer(String player) {
		playerIndex.remove(player);
	}
	
	public static void clear() {
		playerIndex.clear();
	}
	
	public Quest addQuest(Quest quest) {
		quests.add(quest);
		quest.alert(this.getPlayer());
		return quest;
	}

	public ArrayList<Quest> getQuests() {
		return quests;
	}
}
