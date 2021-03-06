package com.tidesofwaronline.Exodus.Abilities;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import com.tidesofwaronline.Exodus.Abilities.Spell;

public abstract class SpellEvent extends Event {

    private Spell spell;
    private Player caster;
   
    public SpellEvent(Spell spell, Player caster) {
            this.spell = spell;
            this.caster = caster;
    }
   
    /**
     * Gets the spell involved in the event.
     * @return the spell
     */
    public Spell getSpell() {
            return spell;
    }
   
    /**
     * Gets the player casting the spell.
     * @return the casting player
     */
    public Player getCaster() {
            return caster;
    }
   
}
