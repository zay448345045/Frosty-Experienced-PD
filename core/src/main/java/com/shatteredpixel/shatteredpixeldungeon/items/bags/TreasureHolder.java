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

package com.shatteredpixel.shatteredpixeldungeon.items.bags;

import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.AdditionItem;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.BinaryItem;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.DivisibilityItem;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.DivisionItem;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.ExponentialItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.MixedOperationItem;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.MultiplicationItem;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.OddEvenItem;
import com.shatteredpixel.shatteredpixeldungeon.items.QuestionaireItem;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.Questionnaire;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.RectangularItem;
import com.shatteredpixel.shatteredpixeldungeon.items.questionnaires.SubtractionItem;
import com.shatteredpixel.shatteredpixeldungeon.items.treasurebags.TreasureBag;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class TreasureHolder extends Bag {

	{
		image = ItemSpriteSheet.BACKPACK;
	}

	/**
	 * @param item
	 * This item holds something, including questionnaires.
	 */

	@Override
	public boolean canHold( Item item ) {
		if (item instanceof TreasureBag || item instanceof QuestionaireItem
				/*
				|| item instanceof AdditionItem || item instanceof DivisionItem
				|| item instanceof MultiplicationItem || item instanceof SubtractionItem
				|| item instanceof ExponentialItem || item instanceof MixedOperationItem
				|| item instanceof OddEvenItem || item instanceof DivisibilityItem
				|| item instanceof BinaryItem || item instanceof RectangularItem
				*/ || item instanceof Questionnaire){

			return super.canHold(item);
		} else {
			return false;
		}
	}

	public int capacity(){
		return 63;
	}
	
	@Override
	public boolean collect( Bag container ) {
		if (super.collect( container )) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public long value() {
		return 60;
	}

}
