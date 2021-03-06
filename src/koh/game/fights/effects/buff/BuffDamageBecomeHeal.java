package koh.game.fights.effects.buff;

import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;
import koh.game.fights.effects.EffectHeal;
import koh.protocol.client.enums.FightDispellableEnum;
import koh.protocol.types.game.actions.fight.AbstractFightDispellableEffect;
import koh.protocol.types.game.actions.fight.FightTriggeredEffect;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Melancholia
 */
public class BuffDamageBecomeHeal extends BuffEffect {

    public BuffDamageBecomeHeal(EffectCast CastInfos, Fighter Target) {
        super(CastInfos, Target, BuffActiveType.ACTIVE_ATTACKED_AFTER_JET, BuffDecrementType.TYPE_BEGINTURN);
    }

    @Override
    public int applyEffect(MutableInt damageValue, EffectCast DamageInfos) {
        if (DamageInfos.isReflect || DamageInfos.isReturnedDamages || DamageInfos.isPoison || damageValue.getValue() < -1) {
            return -1;
        }
        // mort
        if (caster.isDead()) {
            //target.buff.RemoveBuff(this);
            return -1;
        }
        
        damageValue.setValue((damageValue.getValue() * this.castInfos.effect.value) /100);

        if (EffectHeal.applyHeal(castInfos, target, damageValue, false) == -3) {
            return -3;
        }
        damageValue.setValue(0);
        return super.applyEffect(damageValue, DamageInfos);
    }

    @Override
    public AbstractFightDispellableEffect getAbstractFightDispellableEffect() {
        return new FightTriggeredEffect(this.getId(), this.target.getID(), (short) this.duration, FightDispellableEnum.REALLY_NOT_DISPELLABLE, this.castInfos.spellId, this.castInfos.effect.effectUid, 0, (short) this.castInfos.effect.diceNum, (short) this.castInfos.effect.diceSide, (short) this.castInfos.effect.value, (short) 0/*(this.castInfos.effect.delay)*/);
    }

}
