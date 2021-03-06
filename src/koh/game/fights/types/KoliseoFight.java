package koh.game.fights.types;

import koh.concurrency.CancellableScheduledRunnable;
import koh.game.actions.GameFight;
import koh.game.dao.DAO;
import koh.game.entities.actors.Player;
import koh.game.entities.actors.character.PlayerInst;
import koh.game.entities.environments.DofusMap;
import koh.game.entities.fight.Challenge;
import koh.game.entities.item.EffectHelper;
import koh.game.entities.item.InventoryItem;
import koh.game.entities.kolissium.ArenaBattle;
import koh.game.entities.kolissium.KolizeumExecutor;
import koh.game.fights.*;
import koh.game.fights.fighters.CharacterFighter;
import koh.game.fights.utils.AntiCheat;
import koh.game.network.WorldClient;
import koh.protocol.client.enums.EffectGenerationType;
import koh.protocol.client.enums.FighterRefusedReasonEnum;
import koh.protocol.client.enums.TextInformationTypeEnum;
import koh.protocol.messages.game.basic.TextInformationMessage;
import koh.protocol.messages.game.context.fight.FightOutcomeEnum;
import koh.protocol.messages.game.context.fight.GameFightEndMessage;
import koh.protocol.messages.game.context.fight.GameFightJoinMessage;
import koh.protocol.messages.game.context.fight.GameFightLeaveMessage;
import koh.protocol.types.game.context.fight.FightLoot;
import koh.protocol.types.game.context.fight.FightResultExperienceData;
import koh.protocol.types.game.context.fight.FightResultPlayerListEntry;
import koh.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Created by Melancholia on 6/10/16.
 */
public class KoliseoFight extends Fight {

    private SimpleLogger koliseoLog;
    static final FightController fightController = DAO.getMaps().findTemplate(DAO.getSettings().getIntElement("Register.StartMap")).getFightController();



    public KoliseoFight(DofusMap map, WorldClient attacker, WorldClient defender) {
        super(FightTypeEnum.FIGHT_TYPE_PVP_ARENA, map);
        final Fighter attFighter = new CharacterFighter(this, attacker);
        final Fighter defFighter = new CharacterFighter(this, defender);

        attacker.addGameAction(new GameFight(attFighter, this));
        defender.addGameAction(new GameFight(defFighter, this));

        // Les leaders d'equipes
        this.myTeam1.setLeader(attFighter);
        this.myTeam2.setLeader(defFighter);

        // On despawn avant la vue du flag de combat
        attFighter.joinFight();
        defFighter.joinFight();
        try{
            while (this.challenges.size() < 2){
                final int key = DAO.getChallenges().pop();
                if(!Challenge.canBeUsed(this, myTeam1, key)){
                    continue;
                }
                try {
                    final Challenge chall = DAO.getChallenges().find(key).getDeclaredConstructor(CHALLENGE_CONSTRUCTOR).newInstance(this, myTeam1);
                    this.challenges.put(key, myTeam1,chall);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e ) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void initFight(){
        this.joinFightTeam(myTeam1.leader, this.myTeam1, true, (short) -1, true);
        this.joinFightTeam(myTeam2.leader, this.myTeam2, true, (short) -1, true);


        // Si un timer pour le lancement du combat
        if (this.getStartTimer() != -1) {
            this.startTimer(new CancellableScheduledRunnable(BACK_GROUND_WORKER, (getStartTimer() * 1000)) {
                @Override
                public void run() {
                    try {
                        Thread.sleep(getStartTimer() * 1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    startFight();

                }
            }, "startTimer");
        }
        fightController.addFight(this);
    }



    @Override
    public void leaveFight(Fighter fighter) {
        switch (this.fightState) {
            case STATE_PLACE:
                /*if (fighter == fighter.getTeam().leader) {
                    break;
                } else {
                    this.map.sendToField(new GameFightUpdateTeamMessage(this.fightId, fighter.getTeam().getFightTeamInformations()));

                    this.sendToField(new GameFightRemoveTeamMemberMessage(this.fightId, fighter.getTeam().id, fighter.getID()));

                    fighter.leaveFight();
                }*/
                break;
            case STATE_ACTIVE:
                if (fighter.tryDie(fighter.getID(), true) != -3) {
                    fighter.send(leftEndMessage((CharacterFighter) fighter));
                    this.sendToField(new GameFightLeaveMessage(fighter.getID()));
                    fighter.leaveFight();
                }
                fighter.getPlayer().send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 343, String.valueOf(ArenaBattle.BAN_TIME_MINUTES)));
                PlayerInst.getPlayerInst(fighter.getID()).setBannedTime(System.currentTimeMillis() + ArenaBattle.BAN_TIME_MINUTES * 60000);

                break;
            default:
                logger.error("Incredible left from fighter {} ", fighter.getID());
        }
    }


    @Override
    public FighterRefusedReasonEnum canJoin(FightTeam team, Player character) {
        return FighterRefusedReasonEnum.NOT_ENOUGH_ROOM;
    }

    @Override
    public void endFight(FightTeam winners, FightTeam loosers) {
        this.myResult = new GameFightEndMessage(System.currentTimeMillis() - this.fightTime, this.ageBonus, this.lootShareLimitMalus);

        final StringBuilder log = new StringBuilder(SimpleLogger.getCurrentTimeStamp());
        log.append("\n");
        fightController.removeFight(this);


        final double butin = Math.abs(this.challenges.cellSet().stream()
                .filter(c -> c.getColumnKey() == winners && !c.getValue().isFailed())
                .mapToInt(c -> Challenge.getXPBonus(c.getRowKey()))
                .sum() * 0.01) + 1;
        for (Fighter fighter : (Iterable<Fighter>) winners.getFighters()::iterator) {
            super.addNamedParty((CharacterFighter) fighter, FightOutcomeEnum.RESULT_VICTORY);
            log.append("Winner : ").append(fighter.getPlayer().getNickName()).append("Cote ").append(fighter.getPlayer().getKolizeumRate().getRatingd()).append(" ");

            try {
                log.append(fighter.getPlayer().getAccount().lastIP).append(" ");
            } catch (Exception e) {
            }


            if (fighter.isLeft())
                continue;
            final int diviser = AntiCheat.deviserBy(getLoosers().getFighters().filter(fr -> fr instanceof CharacterFighter), fighter, true, FightTypeEnum.FIGHT_TYPE_PVP_ARENA);
            int cote = FightFormulas.cotePoint(fighter.asPlayer(), winners.getFighters(), loosers.getFighters(), false) / diviser;
            final short honorWon = (short) ((FightFormulas.koliseoPoint(fighter, winners.getFighters(), loosers.getFighters(), false, true) / diviser) * butin);
            fighter.getPlayer().setKoliseoPoints(fighter.getPlayer().getKoliseoPoints() + honorWon);
            long count;
            try {
                count = getLoosers().getFighters().filter(fr -> fr.isPlayer() && fr.getPlayer() != null && fr.getPlayer().getAccount() != null && fr.getPlayer().getAccount().lastIP.equalsIgnoreCase(fighter.getPlayer().getAccount().lastIP)).count();
            } catch (Exception e) {
                count = 0;
            }
            int kamas = RANDOM.nextInt(150) + 40;
            if (count == KolizeumExecutor.getTEAM_SIZE()) {
                cote = 0;
                kamas = 10;
            } else if (count != 0 && (getLoosers().getFighters().count() - count) >= 1) {
                cote /= (count * 4);
                kamas /= (count * 4);
            }
            fighter.getPlayer().addKamas(kamas);
            //final boolean canCraft = fighter.getPlayer().getHonor() > 400;
            final int tokenQua = (int) Math.max(Math.abs(honorWon * 0.15f), 1);


            final InventoryItem item = InventoryItem.getInstance(DAO.getItems().nextItemId(), ArenaBattle.KOLIZETON.getId(), 63, fighter.getPlayer().getID(), tokenQua, EffectHelper.generateIntegerEffect(ArenaBattle.KOLIZETON.getPossibleEffects(), EffectGenerationType.NORMAL, false));
            if (fighter.getPlayer().getInventoryCache().add(item, true)) {
                item.setNeedInsert(true);
            }

            log.append("cote += ")
                    .append(cote)
                    .append(" Zetons=").append(tokenQua)
                    .append(" Divizer =").append(diviser)
                    .append(" OpponentSameIpCount=").append(count)
                    .append("\n");
            this.myResult.results.add(
                    new FightResultPlayerListEntry(FightOutcomeEnum.RESULT_VICTORY,
                            fighter.getWave(),
                            new FightLoot(tokenQua > 0 ? (new int[]{ArenaBattle.KOLIZETON.getId(), tokenQua}) : new int[0], kamas),
                            fighter.getID(),
                            fighter.isAlive(),
                            (byte) fighter.getLevel(),
                            new FightResultExperienceData[]{
                                    new FightResultExperienceData(fighter.getPlayer().getKolizeumRate().getScreenRating(), true, 0, true, 2300, true, cote, true, 0, false, 0, false, false, (byte) 0)}));

        }

        for (Fighter fighter : (Iterable<Fighter>) loosers.getFighters()::iterator) {
            super.addNamedParty(fighter.asPlayer(), FightOutcomeEnum.RESULT_LOST);
            log.append("Losser : ").append(fighter.getPlayer().getNickName()).append("Cote ").append(fighter.getPlayer().getKolizeumRate().getRatingd()).append(" ");

            try {
                log.append(fighter.getPlayer().getAccount().lastIP).append(" ");
            } catch (Exception e) {
            }
            if (fighter.isLeft())
                continue;
            final int divizer = AntiCheat.deviserBy(getWinners().getFighters().filter(fr -> fr instanceof CharacterFighter), fighter, false, FightTypeEnum.FIGHT_TYPE_PVP_ARENA);
            final int cote = (FightFormulas.cotePoint(fighter.asPlayer(), winners.getFighters(), loosers.getFighters(), true) / divizer);

            final short loosedHonor = (short) (FightFormulas.honorPoint(fighter, winners.getFighters(), loosers.getFighters(), true) / divizer);
            fighter.getPlayer().setKoliseoPoints(Math.max(0, fighter.getPlayer().getKoliseoPoints() - loosedHonor));

            log.append("cote -= ").append(cote).append("\n");

            this.myResult.results.add(
                    new FightResultPlayerListEntry(FightOutcomeEnum.RESULT_LOST,
                            fighter.getWave(),
                            new FightLoot(new int[0], 0),
                            fighter.getID(),
                            fighter.isAlive(),
                            (byte) fighter.getLevel(),
                            new FightResultExperienceData[]{
                                    new FightResultExperienceData(fighter.getPlayer().getKolizeumRate().getScreenRating(), true, 0, true, 4000, true, cote, true, 0, false, 0, false, false, (byte) 0)}));
        }


        log.append("=========================================/n");
        try {
            koliseoLog = new SimpleLogger("logs/koli/" + SimpleLogger.getCurrentDayStamp() + ".txt", 0);
            koliseoLog.write(log.toString());
            koliseoLog.newLine();
            koliseoLog.close();
            /*final Map<String, Long> ips = winners.getFighters()
                    .filter(f -> f.isPlayer() && f.getPlayer().getAccount() != null && f.getPlayer().getAccount().currentIP != null)
                    .map(f -> f.getPlayer().getAccount().currentIP)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            ips.entrySet().stream()
                    .filter(cel -> cel.getValue() > 1L)
                    .forEach(cell -> {
                        loosers.getFighters().filter(f -> f.isPlayer() && f.getPlayer().getAccount() != null && f.getPlayer().getAccount().currentIP != null)
                                .forEach(losser -> {
                                    final int P_PROPERTIES.get(losser.getID()).victimByIPS.get(FightTypeEnum.FIGHT_TYPE_PVP_ARENA).get(cell.getKey());
                                });
                    });*/

        } catch (Exception e) {
            e.printStackTrace();
        }

        super.endFight();
    }


    @Override
    public int getStartTimer() {
        return 30;
    }

    @Override
    public int getTurnTime() {
        return 45000;
    }

    @Override
    protected void sendGameFightJoinMessage(Fighter fighter) {
        //boolean canBeCancelled, boolean canSayReady, boolean isFightStarted, short timeMaxBeforeFightStart, byte fightType
        fighter.send(new GameFightJoinMessage(false, !this.isStarted(), this.isStarted(), (short) this.getPlacementTimeLeft(), this.fightType.value));
    }

    @Override
    public GameFightEndMessage leftEndMessage(CharacterFighter fighter) {
        return null;
    }
}
