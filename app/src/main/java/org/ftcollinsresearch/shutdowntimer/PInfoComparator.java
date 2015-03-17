/**
 * Created by jeff boehmer on 2/19/15.
 */

package org.ftcollinsresearch.shutdowntimer;

import java.util.Comparator;

/**
 * Simple comparator for sorting the PInfo objects by appname
 */
public class PInfoComparator implements Comparator<PInfo> {
    @Override
    public int compare(PInfo pi1, PInfo pi2) {
        return pi1.appname.compareToIgnoreCase(pi2.appname);
    }
}