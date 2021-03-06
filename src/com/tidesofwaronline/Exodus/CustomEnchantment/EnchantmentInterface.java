package com.tidesofwaronline.Exodus.CustomEnchantment;

import org.bukkit.entity.LivingEntity;

import com.tidesofwaronline.Exodus.Player.ExoPlayer;

public interface EnchantmentInterface {
	public abstract void onDamage(ExoPlayer player, LivingEntity entity); //Called when the player is attacked by a LivingEntity
	public abstract void onHit(ExoPlayer player, LivingEntity entity);   //Called when the player attacks a LivingEntity
	public abstract void onTick(ExoPlayer player);     //Called every tick
	public String getName();
}
