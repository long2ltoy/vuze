/**
 * Created on May 21, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.speedmanager.impl;

public class SaturatedMode implements Comparable {
    public static final SaturatedMode AT_LIMIT = new SaturatedMode("AT_LIMIT",0.97f);
    public static final SaturatedMode HIGH = new SaturatedMode("HIGH",0.80f);
    public static final SaturatedMode MED = new SaturatedMode("MED",0.30f);
    public static final SaturatedMode LOW = new SaturatedMode("LOW",0.03f);
    public static final SaturatedMode NONE = new SaturatedMode("NONE",0.0f);

    private final String name;
    private final float percentCapacity;

    private SaturatedMode(String _name, float _percent) {
        name = _name;
        percentCapacity = _percent;
    }

    private float getThreshold(){
        return percentCapacity;
    }

    /**
     * From the currentRate and limit determine the mode.
     * @param currentRate -
     * @param limit -
     * @return - SaturatedMode
     */
    public static SaturatedMode getSaturatedMode(int currentRate,int limit){
        float percent = (float)currentRate/(float)limit;

        if( percent > AT_LIMIT.getThreshold() ){
            return AT_LIMIT;
        }
        else if( percent > HIGH.getThreshold() ){
            return HIGH;
        }
        else if( percent > MED.getThreshold() ){
            return MED;
        }
        else if( percent > LOW.getThreshold() ){
            return LOW;
        }

        return NONE;
    }

    public String toString() {
        return name;
    }

    /**
     * @param satMode the SaturatedMode to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    public int compareTo(SaturatedMode satMode) {

        if( percentCapacity < satMode.getThreshold() ){
            return -1;
        }
        else if( percentCapacity > satMode.getThreshold() ){
            return +1;
        }

        return 0;
    }

    /**

     * @param obj the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this Object.
     */
    public int compareTo(Object obj) {

        if( !(obj instanceof SaturatedMode) ){
            throw new ClassCastException("Only comparable to SaturatedMode class.");
        }
        SaturatedMode casted = (SaturatedMode) obj;
        return compareTo(casted);
    }
}
