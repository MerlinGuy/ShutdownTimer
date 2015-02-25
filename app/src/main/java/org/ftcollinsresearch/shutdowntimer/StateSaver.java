/**
 * Created by jeff boehmer on 2/25/15.
 */
package org.ftcollinsresearch.shutdowntimer;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class StateSaver {

    public StateSaver(Activity activity) {
    }

    public static void saveState(Activity activity, String state_name, JSONObject jsonState) {
        try {
            byte[] encBytes = Base64.encode(jsonState.toString().getBytes(), Base64.DEFAULT);
            String filename = activity.getApplicationContext().getPackageName() + "." + state_name;
            FileOutputStream fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(encBytes);
            fos.close();

            Log.d("FCR", "saveState:  " + jsonState.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONObject restoreState(Activity activity, String state_name) {
        InputStream in = null;

        try {
            String filename = activity.getApplicationContext().getPackageName() + "." + state_name;
            File file = activity.getBaseContext().getFileStreamPath(filename);
            JSONObject json;
            if (file.exists()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                in = new FileInputStream(file);

                while ( (bytesRead = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                byte[] bytes = Base64.decode(baos.toByteArray(), Base64.DEFAULT);
                json = new JSONObject(new String(bytes));
            } else {
                json = new JSONObject();
            }

            Log.d("FCR", "restoreState:  " + json.toString());
            return json;

        } catch (Exception e) {
            Log.e("restoreState", e.getMessage());
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch(Exception e){
                    Log.e("FCR","exception", e);
                }
            }
        }
    }


}
