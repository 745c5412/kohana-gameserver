package koh.game.fights.effects.buff;

import koh.game.dao.DAO;
import koh.game.entities.spells.SpellLevel;
import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;
import koh.game.fights.fighters.SummonedFighter;
import koh.protocol.client.enums.FightDispellableEnum;
import koh.protocol.client.enums.FightStateEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.actions.fight.AbstractFightDispellableEffect;
import koh.protocol.types.game.actions.fight.FightTemporaryBoostStateEffect;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Neo-Craft
 */
public class BuffState extends BuffEffect {

    public BuffState(EffectCast CastInfos, Fighter target) {
        super(CastInfos, target, BuffActiveType.ACTIVE_STATS, BuffDecrementType.TYPE_BEGINTURN);

        if(this.duration != -1 && castInfos.effectType != StatsEnum.INVISIBILITY
                && !(castInfos.effect != null && castInfos.caster == target && getState() == FightStateEnum.PESANTEUR)) {
            this.duration++;
        }

    }

    private FightStateEnum getState(){
        return FightStateEnum.valueOf(castInfos.effect.value);
    }

    @Override
    public int applyEffect(MutableInt DamageValue, EffectCast DamageInfos) {
        this.target.getStates().addState(this);
        //Vise le telegraph
        if(castInfos.effect != null && getState() == FightStateEnum.Téléfrag && target.getCellId() == castInfos.cellId && this.target instanceof SummonedFighter && target.asSummon().getGrade().getMonsterId() == 3958) { // Synchro
            final SpellLevel spell = DAO.getSpells().findSpell(5435).getLevelOrNear(target.asSummon().getGrade().getLevel());
            castInfos.getFight().launchSpell(target, spell, target.getCellId(), true, true, true, castInfos.spellId);
           // target.tryDie(castInfos.caster.getID(), true);
        }
        return super.applyEffect(DamageValue, DamageInfos);
    }

    @Override
    public int removeEffect() {
        this.target.getStates().delState(this);

        return super.removeEffect();
    }

    @Override
    public AbstractFightDispellableEffect getAbstractFightDispellableEffect() {
        return new FightTemporaryBoostStateEffect(this.getId(), this.target.getID(), (short) this.duration, FightDispellableEnum.DISPELLABLE_BY_STRONG_DISPEL, (short) this.castInfos.spellId, this.castInfos.getEffectUID(), this.castInfos.parentUID, (short) (castInfos.effect == null ? FightStateEnum.INVISIBLE.value : Math.abs(this.castInfos.randomJet(target))), (short) (castInfos.effect == null ? FightStateEnum.INVISIBLE.value : castInfos.effect.value));
    }

}
