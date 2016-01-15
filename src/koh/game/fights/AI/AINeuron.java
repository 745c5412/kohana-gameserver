package koh.game.fights.AI;

import koh.game.entities.spells.SpellLevel;
import koh.game.fights.Fighter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Melancholia on 1/11/16.
 */
public class AINeuron {

    public List<Fighter> myEnnemies = new ArrayList<Fighter>();
    public boolean myAttacked = false;
    public List<Short> myReachableCells = new ArrayList<Short>();
    public SpellLevel myBestSpell;
    public int myBestScore = 0;
    public short myBestMoveCell, myBestCastCell;
    public boolean myFirstTargetIsMe;
    public Map<Integer,Integer> myScoreInvocations = new HashMap<Integer,Integer>();

    @Override
    public void finalize(){
        try {
            if (myEnnemies != null)
            {
                myEnnemies.clear();
                myEnnemies = null;
            }
            myAttacked = false;
            if (myReachableCells != null)
            {
                myReachableCells.clear();
                myReachableCells = null;
            }
            if (myScoreInvocations != null)
            {
                myScoreInvocations.clear();
                myScoreInvocations = null;
            }
            myBestSpell = null;
            myFirstTargetIsMe = false;
            myBestScore =  0;
            myBestMoveCell = myBestCastCell = 0;
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
