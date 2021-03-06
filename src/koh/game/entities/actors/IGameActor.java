package koh.game.entities.actors;

import koh.game.dao.DAO;
import koh.game.entities.environments.DofusCell;
import koh.game.entities.environments.DofusMap;
import koh.game.network.WorldClient;
import koh.protocol.client.Message;
import koh.protocol.types.game.context.EntityDispositionInformations;
import koh.protocol.types.game.context.GameContextActorInformations;
import koh.protocol.types.game.context.roleplay.GameRolePlayActorInformations;
import koh.protocol.types.game.look.EntityLook;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Neo-Craft
 * No refactor for the moment to many problems */
public abstract class IGameActor {

    @Getter
    @Setter
    protected int ID;
    @Getter
    protected volatile DofusCell cell;
    @Getter
    @Setter
    protected int mapid;

    @Setter
    protected EntityLook entityLook;

    protected abstract EntityLook getEntityLook();

    @Getter @Setter
    protected byte direction = 1;

    public void setActorCell(DofusCell cell){
        this.cell = cell;
    }

    public DofusMap getDofusMap() {
        return DAO.getMaps().findTemplate(this.mapid);
    }

    public GameContextActorInformations getGameContextActorInformations(Player character) {
        return new GameRolePlayActorInformations(this.ID, this.getEntityLook(), this.getEntityDispositionInformations(character));
    }

    public boolean canBeSeen(IGameActor Actor) {
        return true;
    }

    public void send(Message Packet) {
        if (this instanceof Player) {
            ((Player) this).send(Packet);
        }
    }


    public EntityDispositionInformations getEntityDispositionInformations(Player character) {
        return new EntityDispositionInformations(this.cell.getId(), direction);
    }

    //public DirectionsEnum direction;
}
