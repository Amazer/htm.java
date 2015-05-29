/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.numenta.nupic.Connections;

/**
 * Represents a proximal or distal dendritic segment.
 * Segments are owned by {@link Cell}s and in turn own {@link Synapse}s
 * which are obversely connected to by a "source cell", which is the {@link Cell}
 * which will activate a given {@link Synapse} owned by this {@code Segment}.
 * <br>远端树突(继承于segment)。呈现一个近端或者远端的树突segment。segment是被cells拥有的，因此，也拥有突触s
 * 突触显然是与一个source cell连接着，这个source cell会激活一个此segment给定的突触
 * 
 * </br>
 * @author Chetan Surpur
 * @author David Ray
 */
public class DistalDendrite extends Segment {
    private Cell cell;
    private int index;
    
    private static final Set<Synapse> EMPTY_SYNAPSE_SET = Collections.emptySet();
    
    /**
     * Constructs a new {@code Segment} object with the specified
     * owner {@link Cell} and the specified index.
     * 
     * @param cell      the owner
     * @param index     this {@code Segment}'s index.
     */
    public DistalDendrite(Cell cell, int index) {
        this.cell = cell;
        this.index = index;
    }
    
    /**
     * Returns the owner {@link Cell} 
     * @return
     */
    public Cell getParentCell() {
        return cell;
    }
    
    /**
     * Creates and returns a newly created {@link Synapse} with the specified
     * source cell, permanence, and index.
     * <br>新创建一个突触，with 给定的source cell，固定值，突触index
     * @param c             the connections state of the temporal memory
     * <br>时间记忆的连接状态
     * @param sourceCell    the source cell which will activate the new {@code Synapse}
     * @param permanence    the new {@link Synapse}'s initial permanence.
     * @param index         the new {@link Synapse}'s index.
     * 
     * 
     * @return
     */
    public Synapse createSynapse(Connections c, Cell sourceCell, double permanence, int index) {
    	Pool pool = new Pool(1);
    	Synapse s = super.createSynapse(c, c.getSynapses(this), sourceCell, pool, index, sourceCell.getIndex());
    	pool.setPermanence(c, s, permanence);
        return s;
    }
    
    /**
     * Returns all {@link Synapse}s
     * 
     * @param   c   the connections state of the temporal memory
     * @return
     */
    public List<Synapse> getAllSynapses(Connections c) {
        return c.getSynapses(this);
    }
    
    /**
     * Returns the synapses on a segment that are active due to lateral input
     * from active cells.
     * <br>由激活态的横向细胞而是激活态的一个segment上的突触s
     * @param activeSynapsesForSegment
     * <br>激活态突触
     * @param permanenceThreshold
     * <br>固定值阈值
     * @return
     */
    public Set<Synapse> getConnectedActiveSynapses(Map<DistalDendrite, Set<Synapse>> activeSynapsesForSegment, double permanenceThreshold) {
        Set<Synapse> connectedSynapses = null;
        
        if(!activeSynapsesForSegment.containsKey(this)) {
            return EMPTY_SYNAPSE_SET;
        }
        
        for(Synapse s : activeSynapsesForSegment.get(this)) {
            if(s.getPermanence() >= permanenceThreshold) {
            	if(connectedSynapses == null) {
            		connectedSynapses = new LinkedHashSet<Synapse>();
            	}
                connectedSynapses.add(s);
            }
        }
        return connectedSynapses == null ? EMPTY_SYNAPSE_SET : connectedSynapses;
    }
    
    /**
     * Called for learning {@code Segment}s so that they may
     * adjust the permanences of their synapses.
     * <br>调整segment.学习时调用。为突触调整永久值
     * @param c                     the connections state of the temporal memory
     * <br> 时间记忆的连接状态
     * @param activeSynapses        a set of active synapses owned by this {@code Segment} which
     *                              will have their permanences increased. All others will have their
     *                              permanences decreased.
     *                              <br>此segment激活态突触集，which的永久值将被increased。其他所有的突触的永久值将被decreased.
     * @param permanenceIncrement   the increment by which permanences are increased.
     * <br>永久值增量
     * @param permanenceDecrement   the increment by which permanences are decreased.
     * <br>永久值减量
     */
    public void adaptSegment(Connections c, Set<Synapse> activeSynapses, double permanenceIncrement, double permanenceDecrement) {
        for(Synapse synapse : c.getSynapses(this)) {
            double permanence = synapse.getPermanence();
            if(activeSynapses.contains(synapse)) {
                permanence += permanenceIncrement;
            }else{
                permanence -= permanenceDecrement;
            }
            
            permanence = Math.max(0, Math.min(1.0, permanence));
            
            synapse.setPermanence(c, permanence);
        }
    }
    
    /**
     * Returns a {@link Set} of previous winner {@link Cell}s which aren't already attached to any
     * {@link Synapse}s owned by this {@code Segment}
     * <br>返回上一个winner cell set,which 没有被attached to　任何一个此segment拥有的突触
     * @param   c               the connections state of the temporal memory
     * @param numPickCells      the number of possible cells this segment may designate
     * <br>可能被选出的cell数量
     * @param prevWinners       the set of previous winner cells
     * <br>前一个winner cells set
     * @param random            the random number generator
     * @return                  a {@link Set} of previous winner {@link Cell}s which aren't already attached to any
     *                          {@link Synapse}s owned by this {@code Segment}
     *                          <br>前一个winner cells set中没有被此segment中任何的突触 attached的cells set
     */
    public Set<Cell> pickCellsToLearnOn(Connections c, int numPickCells, Set<Cell> prevWinners, Random random) {
        //Create a list of cells that aren't already synapsed to this segment
        Set<Cell> candidates = new LinkedHashSet<Cell>(prevWinners);
        for(Synapse synapse : c.getSynapses(this)) {
            Cell sourceCell = synapse.getSourceCell();
            if(candidates.contains(sourceCell)) {
                candidates.remove(sourceCell);
            }
        }
        
        numPickCells = Math.min(numPickCells, candidates.size());
        List<Cell> cands = new ArrayList<Cell>(candidates);
        Collections.sort(cands);
        
        Set<Cell> cells = new LinkedHashSet<Cell>();
        for(int x = 0;x < numPickCells;x++) {
            int i = random.nextInt(cands.size());
            cells.add(cands.get(i));
            cands.remove(i);
        }
        
        return cells;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "" + index;
    }
}
