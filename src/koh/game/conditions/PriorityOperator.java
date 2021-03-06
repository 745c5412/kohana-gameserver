package koh.game.conditions;

import koh.game.entities.actors.Player;

/**
 *
 * @author Neo-Craft
 */
public class PriorityOperator extends ConditionExpression {

    public ConditionExpression Expression;

    @Override
    public boolean eval(Player character) {
        return this.Expression.eval(character);
    }

    @Override
    public String toString() {
        return String.format("({0})", this.Expression);
    }

}
