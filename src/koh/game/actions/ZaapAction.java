package koh.game.actions;

import koh.game.dao.DAO;
import koh.game.entities.actors.IGameActor;
import koh.game.entities.actors.Player;
import koh.game.entities.environments.DofusMap;
import koh.game.entities.environments.DofusZaap;
import koh.game.entities.environments.MapPosition;
import koh.protocol.client.enums.DialogTypeEnum;
import koh.protocol.client.enums.TeleporterTypeEnum;
import koh.protocol.client.enums.TextInformationTypeEnum;
import koh.protocol.messages.game.basic.TextInformationMessage;
import koh.protocol.messages.game.dialog.LeaveDialogMessage;
import koh.utils.Enumerable;
import koh.protocol.messages.game.interactive.zaap.ZaapListMessage;

/**
 *
 * @author Neo-Craft
 */
public class ZaapAction extends GameAction {

    public ZaapAction(IGameActor Actor) {
        super(GameActionTypeEnum.ZAAP, Actor);
    }

    @Override
    public void execute() {
        //Stream<Entry<Integer, DofusZaap>> zaaps = MapDAOImpl.zaaps.entrySet().stream().filter(x -> x.getValue().mapid != ((player) actor).currentMap.id);
        this.actor.send(new ZaapListMessage(TeleporterTypeEnum.TELEPORTER_ZAAP, mapIds(), subAreaIds(), costs(), Enumerable.DuplicatedKey(DAO.getMaps().getZaapsLength() - 1, TeleporterTypeEnum.TELEPORTER_ZAAP), ((Player) actor).currentMap.id));
    }

    @Override
    public void abort(Object[] Args) {
        try {
            int map = (int) Args[0];
            DofusZaap zaap = DAO.getMaps().getZaap(map);
            if (zaap == null) {
                return;
            }
            if (((Player) actor).kamas < getCostTo(zaap.getMap())) {
                actor.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 6, new String[0]));
                return;
            }
            ((Player) actor).inventoryCache.substractKamas(getCostTo(zaap.getMap()));
            ((Player) actor).teleport(map, zaap.Cell);

            this.endExecute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void endExecute() throws Exception {
        actor.send(new LeaveDialogMessage(DialogTypeEnum.DIALOG_TELEPORTER));
        super.endExecute();
    }

    public int[] subAreaIds() {
        return DAO.getMaps().getZaapsNot(((Player) actor).currentMap.id).mapToInt(x -> x.getValue().getMap().subAreaId).toArray();
    }

    public int[] mapIds() {
        return DAO.getMaps().getZaapsNot(((Player) actor).currentMap.id).mapToInt(x -> x.getKey()).toArray();
    }

    public int[] costs() {
        int[] Cost = new int[DAO.getMaps().getZaapsLength() - 1];
        int i = 0;
        for (DofusMap zaap : DAO.getMaps().getZaapsNot(((Player) actor).currentMap.id).map(x -> x.getValue().getMap()).toArray(DofusMap[]::new)) {
            Cost[i] = getCostTo(zaap);
            i++;
        }
        return Cost;
    }

    public short getCostTo(DofusMap map) {
        MapPosition position1 = map.position;
        MapPosition position2 = ((Player) actor).currentMap.position;
        return (short) Math.floor(Math.sqrt((double) ((position2.posX - position1.posX) * (position2.posX - position1.posX) + (position2.posY - position1.posY) * (position2.posY - position1.posY))) * 10.0);
    }

    @Override
    public boolean canSubAction(GameActionTypeEnum ActionType) {
        return false;
    }

}
