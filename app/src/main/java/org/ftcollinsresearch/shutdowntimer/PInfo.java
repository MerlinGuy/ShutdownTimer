package org.ftcollinsresearch.shutdowntimer;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.Comparator;

/**
 * Created by jeff boehmer on 2/19/15.
 *
 * Simple class to hold the information of install Applications
 */
public class PInfo {

    public String appname = "";
    public String pname = "";
    public String versionName = "";
    public int versionCode = 0;

    public PInfo (String appname, String packagename) {
        this.appname = appname;
        this.pname =packagename;
    }

    /**
     * This constructor builds a PInfo object using a base PackageInfo object.
     *
     * @param pinfo holds the Package Information
     * @param pm is the PackagaManger which is used to extract the Applicaiton Name from the
     *           PackageInfo object.
     */
    public PInfo (PackageInfo pinfo, PackageManager pm) {
        this.appname = pinfo.applicationInfo.loadLabel(pm).toString();
        this.pname = pinfo.packageName;
        this.versionName = pinfo.versionName;
        this.versionCode = pinfo.versionCode;
    }

    /**
     * This method is needed so that the ArrayAdapter using PInfos can correctly
     * show the package's application name
     *
     * @return the package application name
     */
    public String toString() {
        return appname;
    }

}

