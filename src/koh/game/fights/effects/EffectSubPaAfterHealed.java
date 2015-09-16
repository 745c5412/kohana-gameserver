package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffSubPaAfterHealed;

/**
 *
 * @author Neo-Craft
 */
public class EffectSubPaAfterHealed extends EffectBase {

    @Override
    public int ApplyEffect(EffectCast CastInfos) {
        for (Fighter Target : CastInfos.Targets) {
            Target.Buffs.AddBuff(new BuffSubPaAfterHealed(CastInfos, Target));
        }
        return -1;
    }

}