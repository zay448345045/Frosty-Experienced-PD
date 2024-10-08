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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

public class Bestiary {
	
	public static ArrayList<Class<? extends Mob>> getMobRotation( int depth ){
		ArrayList<Class<? extends Mob>> mobs = standardMobRotation( depth );
		addRareMobs(depth, mobs);
		swapMobAlts(mobs);
		Random.shuffle(mobs);
		return mobs;
	}
	
	//returns a rotation of standard mobs, unshuffled.
	private static ArrayList<Class<? extends Mob>> standardMobRotation( int depth ){
		if (depth < 21 && depth > 0) {
			return new ArrayList<>(Arrays.asList(
					Rat.class,
					Snake.class,
					Gnoll.class,
					Swarm.class,
					Crab.class,
					Slime.class
			));
		} else if (depth < 41 && depth > 20) {
			return new ArrayList<>(Arrays.asList(
					Skeleton.class,
					Thief.class,
					Swarm.class,
					DM100.class,
					Guard.class,
					Necromancer.class
			));
		} else if (depth < 61 && depth > 40) {
			return new ArrayList<>(Arrays.asList(
					Bat.class,
					Brute.class,
					Shaman.random(),
					Spinner.class,
					DM200.class
			));
		} else if (depth < 81 && depth > 60) {
			return new ArrayList<>(Arrays.asList(
					Ghoul.class,
					Elemental.random(),
					Warlock.class,
					Monk.class,
					Golem.class
			));
		} else if (depth < 101 && depth > 80) {
			return new ArrayList<>(Arrays.asList(
					Succubus.class,
					Eye.class,
					Scorpio.class,
					RipperDemon.class
			));
		} else if (depth > 101) {
			return new ArrayList<>(Arrays.asList(
					Rat.class,
					Snake.class,
					Gnoll.class,
					Swarm.class,
					Crab.class,
					Slime.class,
					Skeleton.class,
					Thief.class,
					Swarm.class,
					DM100.class,
					Guard.class,
					Necromancer.class,
					Bat.class,
					Brute.class,
					Shaman.random(),
					Spinner.class,
					DM200.class,
					Ghoul.class,
					Elemental.random(),
					Warlock.class,
					Monk.class,
					Golem.class,
					Succubus.class,
					Eye.class,
					Scorpio.class,
					RipperDemon.class,
					OOFThief.class
			));
		} else {
			return null;
		}
	}
	
	//has a chance to add a rarely spawned mobs to the rotation
	public static void addRareMobs( int depth, ArrayList<Class<?extends Mob>> rotation ){

		switch (depth){

			// Sewers
			case 19: default:
				if (Random.Float() < 0.10f) rotation.add(Thief.class);
				if (Random.Float() < 0.25f) rotation.add(Thief.class);
				return;

			// Prison
			case 39:
				if (Random.Float() < 0.10f) rotation.add(Bat.class);
				if (Random.Float() < 0.25f) rotation.add(Bat.class);
				return;

			// Caves
			case 59:
				if (Random.Float() < 0.10f) rotation.add(Ghoul.class);
				if (Random.Float() < 0.25f) rotation.add(Ghoul.class);
				return;

			// City
			case 79:
				if (Random.Float() < 0.10f) rotation.add(Succubus.class);
				if (Random.Float() < 0.25f) rotation.add(Succubus.class);
				return;
		}
	}
	
	//switches out regular mobs for their alt versions when appropriate
	private static void swapMobAlts(ArrayList<Class<?extends Mob>> rotation){
		for (int i = 0; i < rotation.size(); i++){
			if (Random.Int( 10 ) == 0) {
				Class<? extends Mob> cl = rotation.get(i);
				if (cl == Rat.class) {
					cl = Albino.class;
				} else if (cl == Slime.class) {
					cl = CausticSlime.class;
				} else if (cl == Thief.class) {
					cl = Bandit.class;
				} else if (cl == Necromancer.class){
					cl = SpectralNecromancer.class;
				} else if (cl == Brute.class) {
					cl = ArmoredBrute.class;
				} else if (cl == DM200.class) {
					cl = DM201.class;
				} else if (cl == Monk.class) {
					cl = Senior.class;
				} else if (cl == Scorpio.class) {
					cl = Acidic.class;
				}
				rotation.set(i, cl);
			}
		}
	}
}
