/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * Experienced Pixel Dungeon
 * Copyright (C) 2019-2024 Trashbox Bobylev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

 package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

 import com.shatteredpixel.shatteredpixeldungeon.Assets;
 import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
 import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
 import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
 import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
 import com.shatteredpixel.shatteredpixeldungeon.Assets;
 import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
 import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
 import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
 import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
 import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
 import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
 import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
 import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
 import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
 import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
 import com.watabou.noosa.audio.Sample;
 import com.watabou.utils.BArray;
 import com.watabou.utils.PathFinder;
 import com.watabou.utils.Random;
 import com.watabou.utils.Callback;

 import java.util.ArrayList;
 
 public class ChaosShortSword extends MeleeWeapon {
 
     {
         image = ItemSpriteSheet.CHAOS_SHORTSWORD;
         hitSound = Assets.Sounds.HIT_SLASH;
         hitSoundPitch = 1.1f;
 
         internalTier = tier = 4;
     }
 
     @Override
     public long max(long lvl) {
         return  6*(tier+1) +    //30
                 lvl*(tier-1);   //+3+tier
     }
 
    @Override
    public String targetingPrompt() {
         return Messages.get(this, "prompt");
    }
 
    @Override
    protected int baseChargeUse(Hero hero, Char target){
        return 2;
    }

     @Override
     protected void duelistAbility(Hero hero, Integer target) {
         zap(this.damageRoll(hero) * 1.25f, target, hero, this);
     }

    public void zap(float damage, Integer attacker, Hero defender, MeleeWeapon weapon) {	
        if (attacker == null) {
			return;
		}

		Char enemy = Actor.findChar(attacker);
		if (enemy == null || enemy == defender || defender.isCharmedBy(enemy) || !Dungeon.level.heroFOV[attacker]) {
			GLog.w(Messages.get(weapon, "ability_no_target"));
			return;
		}

        defender.belongings.abilityWeapon = weapon;
		if (!defender.canAttack(enemy)){
			GLog.w(Messages.get(weapon, "ability_bad_position"));
			defender.belongings.abilityWeapon = null;
			return;
		}
		defender.belongings.abilityWeapon = null;

        defender.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				weapon.beforeAbilityUsed(defender, enemy);
				AttackIndicator.target(enemy);
				if (defender.attack(enemy, damage, 0, Char.INFINITE_ACCURACY)){
					Sample.INSTANCE.play( Assets.Sounds.LIGHTNING ); 
					affected.clear();
					arcs.clear();
					
					arc(enemy, defender, 2, affected, arcs);
					
					affected.remove(defender); //defender isn't hurt by lightning
					for (Char ch : affected) {
						if (ch.alignment != enemy.alignment) {
							ch.damage(Math.round(damage * 0.4f), this);
						}
					}

					enemy.sprite.parent.addToFront( new Lightning( arcs, null ) );
				}

				Invisibility.dispel();
				defender.spendAndNext(defender.attackDelay());
				
				if (!enemy.isAlive()){
					weapon.onAbilityKill(defender, enemy);
				}
				weapon.afterAbilityUsed(defender);
                
                
			}
        });

        
    }

    private ArrayList<Char> affected = new ArrayList<>();

	private ArrayList<Lightning.Arc> arcs = new ArrayList<>();
	
	public static void arc( Char attacker, Char defender, int dist, ArrayList<Char> affected, ArrayList<Lightning.Arc> arcs ) {
		
		affected.add(defender);
		
		defender.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
		defender.sprite.flash();
		
		PathFinder.buildDistanceMap( defender.pos, BArray.not( Dungeon.level.solid, null ), dist );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE) {
				Char n = Actor.findChar(i);
				if (n != null && n != attacker && !affected.contains(n)) {
					arcs.add(new Lightning.Arc(defender.sprite.center(), n.sprite.center()));
					arc(attacker, n, (Dungeon.level.water[n.pos] && !n.flying) ? 2 : 1, affected, arcs);
				}
			}
		}
	}
 
     @Override
     public ItemSprite.Glowing glowing() {
         return new ItemSprite.Glowing( 0x00FF00 );
     }
 }
 