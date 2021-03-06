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

import java.util.List;

import org.numenta.nupic.Connections;

/**
 * Base class which handles the creation of {@link Synapses} on behalf of
 * inheriting class types.
 * <br>处理创建突触的父类</br>
 * @author David Ray
 * @see DistalDendrite
 * @see ProximalDendrite
 */
public abstract class Segment {
	/**
     * Creates and returns a newly created {@link Synapse} with the specified
     * source cell, permanence, and index.
     * <br>新创建一个突触。给定source cell，固定值，和索引</br>
     * IMPORTANT: 	For DistalDendrites, there is only one synapse per pool, so the
     * 				synapse's index doesn't really matter (in terms of tracking its
     * 				order within the pool. In that case, the index is a global counter
     * 				of all distal dendrite synapses.
     * 
     * 				For ProximalDendrites, there are many synapses within a pool, and in
     * 				that case, the index specifies the synapse's sequence order within
     * 				the pool object, and may be referenced by that index.
     * <br>重点：
     * 对于远端树突，一个pool只有一个突触，所以突触的index并不是很重要（就跟踪他在pool里面的顺序而言）。
     * 在那种情况下，index是一个所有远端树突突触的全局的counter
     * <br>
     * 对于近端树突来说，在一个pool中有许多突触。在这种情况下，index表示在pool对象中突触的顺序。
     * </br>
     * </br>
     * @param c             the connections state of the temporal memory
     * <br>时间记忆的连接状态</br>
     * @param sourceCell    the source cell which will activate the new {@code Synapse}
     * <br>会激活新突触的source cell</br>
     * @param pool		    the new {@link Synapse}'s pool for bound variables.
     * <br>新突触pool for 给定的variables
     * @param index         the new {@link Synapse}'s index.
     * <br>新突触的index
     * @param inputIndex	the index of this {@link Synapse}'s input (source object); be it a Cell or InputVector bit.
     * <br>新突触的输入对象的index
     * 
     * @return Synapse 一个新的突触
     */
    public Synapse createSynapse(Connections c, List<Synapse> syns, Cell sourceCell, Pool pool, int index, int inputIndex) {
        Synapse s = new Synapse(c, sourceCell, this, pool, index, inputIndex);
        syns.add(s);
        return s;
    }
}
