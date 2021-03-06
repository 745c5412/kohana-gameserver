package koh.game.entities.fight;

import koh.game.entities.environments.MovementPath;
import koh.game.entities.item.Weapon;
import koh.game.entities.spells.SpellLevel;
import koh.game.fights.Fight;
import koh.game.fights.FightTeam;
import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;
import koh.protocol.client.enums.StatsEnum;

/**
 * Created by Melancholia on 8/29/16.
 * 15,Invoquer une Arakne à chaque fois que le sort est disponible, pendant toute la durée du combat.]
 */
public class Araknophile extends Challenge {

    public static final int SPELL = 370;
    public Araknophile(Fight fight, FightTeam team) {
        super(fight, team);
    }

    @Override
    public void onFightStart() {

    }

    @Override
    public void onTurnStart(Fighter fighter) {

    }

    @Override
    public void onTurnEnd(Fighter fighter) {
        if(fighter.getSpells() == null || fighter.getTeam() != team){
            return;
        }
        if(fighter.getSpells().stream().anyMatch(s -> s.getSpellId() ==SPELL)
                && fighter.getStats().getTotal(StatsEnum.ADD_SUMMON_LIMIT) > 0
                && fighter.getSpellsController().canLaunchSpellId(SPELL)){
            this.failChallenge();
        }
    }

    @Override
    public void onFighterKilled(Fighter target, Fighter killer) {

    }

    @Override
    public void onFighterMove(Fighter fighter, MovementPath path) {

    }

    @Override
    public void onFighterSetCell(Fighter fighter, short startCell, short endCell) {

    }

    @Override
    public void onFighterCastSpell(Fighter fighter, SpellLevel spell) {

    }

    @Override
    public void onFighterCastWeapon(Fighter fighter, Weapon weapon) {

    }

    @Override
    public void onFighterTackled(Fighter fighter) {

    }

    @Override
    public void onFighterLooseLife(Fighter fighter, EffectCast cast, int damage) {

    }

    @Override
    public void onFighterHealed(Fighter fighter, EffectCast cast, int heal) {

    }

}
