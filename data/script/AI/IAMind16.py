#
#		IA Mind of "Taxcollector"
#		@author: alleos13
#		@date: 08/06/2013
from koh.game.entities.mob import IAMind
from koh.game.fights.AI import AIProcessor

class IAMind16(IAMind):

    def play(self,IA):
        if IA.getUsedNeurons() == 1:
            IA.initCells()
            IA.selfAction()
        elif IA.getUsedNeurons() == 2:
            IA.initCells()
            IA.selfAction()
        elif IA.getUsedNeurons() == 3:
            IA.initCells()
            IA.selfAction()
        elif IA.getUsedNeurons() == 4:
            IA.initCells()
            IA.selfAction()
        else:
            IA.stop();