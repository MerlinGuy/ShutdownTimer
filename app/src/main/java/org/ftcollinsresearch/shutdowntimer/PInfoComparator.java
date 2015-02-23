package org.ftcollinsresearch.shutdowntimer;

import java.util.Comparator;

/**
 * Created by jeff boehmer on 2/19/15.
 */
public class PInfoComparator implements Comparator<PInfo> {
    @Override
    public int compare(PInfo pi1, PInfo pi2) {
        return pi1.appname.compareToIgnoreCase(pi2.appname);
    }
}