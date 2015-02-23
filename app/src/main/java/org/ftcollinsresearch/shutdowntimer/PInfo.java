package org.ftcollinsresearch.shutdowntimer;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.Comparator;

/**
 * Created by jeff boehmer on 2/19/15.
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

    public PInfo (PackageInfo pinfo, PackageManager pm) {
        this.appname = pinfo.applicationInfo.loadLabel(pm).toString();
        this.pname = pinfo.packageName;
        this.versionName = pinfo.versionName;
        this.versionCode = pinfo.versionCode;
    }

    public String toString() {
        return appname;
    }

}

