/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2020 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.items.questionnaires;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class BinaryItem extends Questionnaire {
    {
        image = ItemSpriteSheet.EXOTIC_SCROLL_PLUS;
        unique = true;
        identify();
    }

    private static final String AC_ANSWER = "ANSWER";
    private static final String AC_REFRESH = "REFRESH";

    private int CODE = Random.Int(1000) + 1;
    private String ANSWER = Integer.toBinaryString(CODE);
    public static int totalAnswers_i = 0;
    public static int streak_i = 0;
    public int t = totalAnswers_i;
    public static int s = streak_i;



    @Override
    public ArrayList<String> actions(Hero hero ) {
        ArrayList<String> actions = super.actions( hero );
        actions.add( AC_ANSWER );
        actions.add( AC_REFRESH );
        actions.remove( AC_THROW );
        actions.remove( AC_DROP );
        return actions;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public void execute( Hero hero, String action ) {
        super.execute( hero, action );
        if (action.equals( AC_ANSWER ) && hero.buff(CodeCooldown10.class) == null ) {
            askCode();
        } else if (action.equals( AC_ANSWER ) && hero.buff(CodeCooldown10.class) != null) {
            GLog.w(Messages.get(this, "cooldown"));
            GameScene.flash(0xFFFF0000);
        }
        if (action.equals( AC_REFRESH )) {
            CODE = Random.Int(1000) + 1;
            ANSWER = Integer.toBinaryString(CODE);
        }
    }

    private void askCode() {
        GameScene.show(new WndTextInput( "Input Answer","Convert to Binary: " + CODE, "", 40, false, "Done", "Cancel" ) {
            @Override
            public void onSelect( boolean positive, String text ) {
                if (text.equals(ANSWER)) {
                    Buff.affect(hero, CodeCooldown10.class).set(3);
                    GLog.h("You answered the question correctly.");
                    CODE = Random.Int(1000) + 1;
                    ANSWER = Integer.toBinaryString(CODE);
                    totalAnswers_i += 1;
                    streak_i += 1;
                    GameScene.flash(0xFF008000);
                    switch (Random.Int(9)) {
                        case 0:
                            updateQuickslot();
                            Buff.affect(hero, PotionOfCleansing.Cleanse.class, 30f * streak_i);
                            break;
                        case 1:
                            updateQuickslot();
                            Buff.affect(hero, ArcaneArmor.class).set(6, 50 * streak_i);
                            break;
                        case 2:
                            updateQuickslot();
                            Buff.affect(hero, Barkskin.class).set(6, 50 * streak_i);
                            break;
                        case 3:
                            updateQuickslot();
                            Buff.affect(hero, Adrenaline.class, 30 * streak_i);
                            break;
                        case 4:
                            updateQuickslot();
                            Buff.affect(hero, BlobImmunity.class, 60 * streak_i);
                            break;
                        case 5:
                            updateQuickslot();
                            Dungeon.level.drop(Generator.randomArmor(), curUser.pos).sprite.drop();
                            Dungeon.level.drop(Generator.randomArmor(), curUser.pos).sprite.drop();
                            break;
                        case 6:
                            updateQuickslot();
                            Dungeon.level.drop(Generator.randomArtifact(), curUser.pos).sprite.drop();
                            break;
                        case 7:
                            updateQuickslot();
                            Dungeon.level.drop(Generator.randomMissile(), curUser.pos).sprite.drop();
                            Dungeon.level.drop(Generator.randomMissile(), curUser.pos).sprite.drop();
                            break;
                        case 8:
                            updateQuickslot();
                            Dungeon.level.drop(Generator.randomWeapon(), curUser.pos).sprite.drop();
                            Dungeon.level.drop(Generator.randomWeapon(), curUser.pos).sprite.drop();
                            break;
                    }
                } else if (text.equals("")) {
                    GLog.w("You didn't answer the question.");
                } else {
                    GameScene.flash(0xFFFF0000);
                    GLog.w("That answer is not equals as the given, try again.");
                    streak_i = 0;
                }
            }

            @Override
            public void onBackPressed() {
                GLog.w("You didn't answer the question.");
                this.hide();
            }
        } );
    }


    @Override
    public ItemSprite.Glowing glowing() {
        return new ItemSprite.Glowing( 0xA9F5A9, 0.3f );
    }

    @Override
    public long level() {
        return streak_i;
    }

    @Override
    public String name() {
        return "Binary Questionnaire";
    }

    @Override
    public String desc() {
        return "This item represents your intelligence, it may also give you some rewards.\n\nConvert to Binary: " + CODE + "\nAnswered Correctly: " + totalAnswers_i + "\nStreak: " + streak_i
                + "\n\n_Streaks resets at zero when wrong answer is entered._"
                + "\n\n_Streak Pass List:_"
                + "\n- (0) None.";
    }
    private String STREAKS = "STREAKS";
    private String TOTAL_ANSWERS = "TOTAL_ANSWERS";
    @Override
    public void storeInBundle( Bundle bundle ) {
        super.storeInBundle( bundle );
        bundle.put( STREAKS, streak_i );
        bundle.put( TOTAL_ANSWERS, totalAnswers_i );
    }

    @Override
    public void restoreFromBundle( Bundle bundle ) {
        super.restoreFromBundle( bundle );
        streak_i = bundle.getInt(STREAKS);
        totalAnswers_i = bundle.getInt(TOTAL_ANSWERS);
    }

    public static class CodeCooldown10 extends Buff {

        int duration = 0;
        int maxDuration = 0;

        {
            type = buffType.NEUTRAL;
            announced = false;
        }

        public void set(int time) {
            maxDuration = time;
            duration = maxDuration;
        }

        public void hit(int time) {
            duration -= time;
            if (duration <= 0) detach();
        }

        @Override
        public boolean act() {
            duration--;
            if (duration <= 0) {
                detach();
            }
            spend(TICK);
            return true;
        }

        @Override
        public int icon() {
            return BuffIndicator.TIME;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0xA9F5A9);
        }

        @Override
        public float iconFadePercent() {
            return Math.max(0, (maxDuration - duration) / maxDuration);
        }

        @Override
        public String iconTextDisplay() {
            return Integer.toString(duration);
        }

        @Override
        public String toString() {
            return Messages.get(this, "name");
        }

        @Override
        public String desc() {
            return Messages.get(this, "desc", duration);
        }

        private static final String MAX_DURATION = "maxDuration";
        private static final String DURATION = "duration";
        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put( MAX_DURATION, maxDuration );
            bundle.put( DURATION, duration );
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            maxDuration = bundle.getInt( MAX_DURATION );
            duration = bundle.getInt( DURATION );
        }
    }
}
