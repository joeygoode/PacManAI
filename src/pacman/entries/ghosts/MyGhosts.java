package pacman.entries.ghosts;

import java.util.EnumMap;
import java.util.Random;
import java.util.ArrayList;
import java.awt.Color;

import pacman.controllers.Controller;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.*;
import pacman.game.GameView;
import pacman.game.internal.Node;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getActions() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.ghosts.mypackage).
 */

public class MyGhosts extends Controller<EnumMap<GHOST,MOVE>>
{

    private Random rnd=new Random();
    private Game m_CurrentGame;
    private EnumMap<GHOST,Integer> m_targets=new EnumMap<GHOST,Integer>(GHOST.class);
    private EnumMap<GHOST,MOVE> m_moves=new EnumMap<GHOST,MOVE>(GHOST.class);
    private EnumMap<GHOST,Color> m_DebugColors= new EnumMap<GHOST,Color>(GHOST.class);
    private STATE m_state;
    private int m_ScatterTime1, m_ChaseTime1, m_ScatterTime2, m_ChaseTime2, m_ScatterTime3,
                m_ChaseTime3, m_ScatterTime4;
    public MyGhosts()
    {
        m_DebugColors.put(GHOST.BLINKY,Color.RED);
        m_DebugColors.put(GHOST.PINKY,Color.PINK);
        m_DebugColors.put(GHOST.INKY,Color.CYAN);
        m_DebugColors.put(GHOST.SUE,Color.ORANGE);
    }
    public EnumMap<GHOST,MOVE> getMove(Game game,long timeDue)
    {
        m_CurrentGame = game;
        UpdateInternals();
        for(GHOST ghost : GHOST.values())	//for each ghost
        {
            if (!m_CurrentGame.isGhostEdible(ghost))
            GameView.addPoints(m_CurrentGame,m_DebugColors.get(ghost),m_targets.get(ghost));
            if(m_CurrentGame.doesGhostRequireAction(ghost))		//if ghost requires an action
            {
                int current_node = m_CurrentGame.getGhostCurrentNodeIndex(ghost);
                int[] neighbors = m_CurrentGame.getNeighbouringNodes(current_node);
                if(m_CurrentGame.isGhostEdible(ghost))
                {
                   ArrayList<MOVE> PossibleMoves = new ArrayList<MOVE>();
                   for (int node : neighbors)
                   {
                       PossibleMoves.add(m_CurrentGame.getMoveToMakeToReachDirectNeighbour(current_node,node));
                   }
                   double random = rnd.nextDouble() * PossibleMoves.size();
                   m_moves.put(ghost, PossibleMoves.get((int) Math.floor(random)));
                }

                double distance = Double.MAX_VALUE;
                int next_node = -1;
                for (int node : neighbors)
                {
                    MOVE move = m_CurrentGame.getMoveToMakeToReachDirectNeighbour(current_node,node);
                    if(m_moves.get(ghost).equals(getOppositeDirection(move)))
                    {
                        continue;
                    }
                    double temp_distance = m_CurrentGame.getDistance(node,m_targets.get(ghost),DM.EUCLID);
                    if (temp_distance < distance)
                    {
                        distance = temp_distance;
                        next_node = node;
                    }
                }
                m_moves.put(ghost,m_CurrentGame.getMoveToMakeToReachDirectNeighbour(current_node,next_node));
            }
        }

        return m_moves;
    }
    private MOVE getOppositeDirection(MOVE move)
    {
        if (move.equals(MOVE.UP)) return MOVE.DOWN;
        else if (move.equals(MOVE.LEFT)) return MOVE.RIGHT;
        else if (move.equals(MOVE.DOWN)) return MOVE.UP;
        else if (move.equals(MOVE.RIGHT)) return MOVE.LEFT;
        else return MOVE.NEUTRAL;
    }

    private void UpdateInternals()
    {
        if (m_CurrentGame.getCurrentLevel() == 0)
        {
            m_ScatterTime1 = 7;
            m_ScatterTime2 = 7;
            m_ScatterTime3 = 5;
            m_ScatterTime4 = 5;
            m_ChaseTime1 = 20;
            m_ChaseTime2 = 20;
            m_ChaseTime3 = 20;
        }
        else if (m_CurrentGame.getCurrentLevel() == 1)
        {
            m_ChaseTime3 = 1033;
            m_ScatterTime4 = 1;
        }
        else if (m_CurrentGame.getCurrentLevel() == 4)
        {
            m_ScatterTime1 = 5;
            m_ScatterTime2 = 5;
        }

        if (m_CurrentGame.getCurrentLevelTime() < 60 * m_ScatterTime1) m_state = STATE.SCATTER;
        else if (m_CurrentGame.getCurrentLevelTime() < 60 * (m_ChaseTime1 + m_ScatterTime1)) m_state = STATE.CHASE;
        else if (m_CurrentGame.getCurrentLevelTime() < 60 * (m_ScatterTime2 + m_ChaseTime1 + m_ScatterTime1)) m_state = STATE.SCATTER;
        else if (m_CurrentGame.getCurrentLevelTime() < 60 * (m_ChaseTime2 + m_ScatterTime2 + m_ChaseTime1 + m_ScatterTime1)) m_state = STATE.CHASE;
        else if (m_CurrentGame.getCurrentLevelTime() < 60 * (m_ScatterTime3 + m_ChaseTime2 + m_ScatterTime2 + m_ChaseTime1 + m_ScatterTime1)) m_state = STATE.SCATTER;
        else if (m_CurrentGame.getCurrentLevelTime() < 60 * (m_ChaseTime3 + m_ScatterTime3 + m_ChaseTime2 + m_ScatterTime2 + m_ChaseTime1 + m_ScatterTime1)) m_state = STATE.CHASE;
        else if (m_CurrentGame.getCurrentLevelTime() < 60 * (m_ScatterTime4 + m_ChaseTime3 + m_ScatterTime3 + m_ChaseTime2 + m_ScatterTime2 + m_ChaseTime1 + m_ScatterTime1)) m_state = STATE.SCATTER;
        else m_state = STATE.CHASE;

        if (m_state.equals(STATE.SCATTER))
        {
            int[] powerpills = m_CurrentGame.getPowerPillIndices();
            m_targets.put(GHOST.PINKY,powerpills[0]);
            m_targets.put(GHOST.BLINKY,powerpills[1]);
            m_targets.put(GHOST.INKY,powerpills[2]);
            m_targets.put(GHOST.SUE,powerpills[3]);
        }  
        else if (m_state.equals(STATE.CHASE))
        {
            int PacManNode = m_CurrentGame.getPacmanCurrentNodeIndex();
            m_targets.put(GHOST.BLINKY,PacManNode);
            MOVE PacManDirection = m_CurrentGame.getPacmanLastMoveMade();
            int pinky_target = GetNodeInDirection(PacManNode,PacManDirection,8);
            m_targets.put(GHOST.PINKY,pinky_target);
            int[] powerpills = m_CurrentGame.getPowerPillIndices();
            if (m_CurrentGame.doesGhostRequireAction(GHOST.INKY))
            {
                int PacManX = m_CurrentGame.getNodeXCood(PacManNode);
                int PacManY = m_CurrentGame.getNodeYCood(PacManNode);
                int BlinkyX = m_CurrentGame.getNodeXCood(m_CurrentGame.getGhostCurrentNodeIndex(GHOST.BLINKY));
                int BlinkyY = m_CurrentGame.getNodeXCood(m_CurrentGame.getGhostCurrentNodeIndex(GHOST.BLINKY));
                int InkyTargetX = (PacManX - BlinkyX) + PacManX;
                int InkyTargetY = (PacManY - BlinkyY) + PacManY;
                m_targets.put(GHOST.INKY,GetNodeClosestToPos(InkyTargetX,InkyTargetY));
            }

            if (m_CurrentGame.getDistance(m_CurrentGame.getGhostCurrentNodeIndex(GHOST.SUE),PacManNode,DM.MANHATTAN) < 16)
                m_targets.put(GHOST.SUE,powerpills[3]);
            else
                m_targets.put(GHOST.SUE,PacManNode);
        }
    }

    int GetNodeInDirection(int CurrentNode, MOVE direction, int iterations)
    {
        int IntermediateNode = CurrentNode;
        for (int i = 0; i < iterations; i++)
        {
            int tempNode = m_CurrentGame.getNeighbour(IntermediateNode,direction);
            if(tempNode == -1)
            {
                int[] neighbors = m_CurrentGame.getNeighbouringNodes(IntermediateNode);
                for (int node : neighbors)
                {
                    MOVE move = m_CurrentGame.getMoveToMakeToReachDirectNeighbour(IntermediateNode,node);
                    if(direction.equals(getOppositeDirection(move))) continue;
                    IntermediateNode = node;
                    direction = move;
                    break;
                }
            }
            else IntermediateNode = tempNode;
        }
        return IntermediateNode;
    }

    int GetNodeClosestToPos(int x, int y)
    {
        Node[] nodes = m_CurrentGame.getCurrentMaze().graph;
        int distance = Integer.MAX_VALUE;
        int node = -1;
        for(Node n : nodes)
        {
            int tempDistance = (n.x - x) * (n.x - x) + (n.y - y) * (n.y - y);
            if (distance > tempDistance)
            {
                distance = tempDistance;
                node = n.nodeIndex;
            }
        }
        return node;
    }

    private enum STATE
    {
        CHASE,
        SCATTER,
        FRIGHTENED
    }
}