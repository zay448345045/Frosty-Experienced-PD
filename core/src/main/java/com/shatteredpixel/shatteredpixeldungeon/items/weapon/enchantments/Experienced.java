/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2023 Evan Debenham
 *
 * Experienced Pixel Dungeon
 * Copyright (C) 2019-2020 Trashbox Bobylev
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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.ExpGenerator;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.RandomItemTicket;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite.Glowing;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class Experienced extends Weapon.Enchantment {

	private static Glowing FX = new Glowing( 0x42a832 );
	
	@Override
	public long proc( Weapon weapon, Char attacker, Char defender, long damage ) {
		int level = (int) Math.max( 0, weapon.buffedLvl() );
		if (Random.Int(2) == 0) {
			Buff.affect(attacker, BlobImmunity.class, 10f);
		}
		if (Random.Int(7) == 0) {
			Dungeon.level.drop(new ScrollOfUpgrade().quantity(2L), attacker.pos).sprite.drop();
			GLog.p("Scroll of Upgrade dropped with a 14% chance!");
		}
		if (Random.Int(10) == 0) {
			Dungeon.level.drop(Generator.random(Generator.Category.TREASUREBAG), attacker.pos).sprite.drop();
			GLog.p("Some kind of treasure bag dropped with a 10% chance!");
		}
		if (Random.Int(50) == 0) {
			Dungeon.level.drop(new Ankh(), attacker.pos).sprite.drop();
			GLog.p("Ankh dropped with a 2% chance!");
		}
		if (Random.Int(20) == 0) {
			Dungeon.level.drop(new RandomItemTicket(), attacker.pos).sprite.drop();
			GLog.p("Random Item Ticket dropped with a 5% chance!");
		}
		damage += damage * 0.10f;
		return damage;
	}
	
	@Override
	public Glowing glowing() {
		return FX;
	}
}
