package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffDamagePerPM;

/**
 *
 * @author Neo-Craft
 */
public class EffectDamagePerPM extends EffectBase {

    @Override
    public int applyEffect(EffectCast castInfos) {
        for (Fighter Target : castInfos.targets) {
            Target.getBuff().addBuff(new BuffDamagePerPM(castInfos, Target));
        }

        return -1;
    }

}