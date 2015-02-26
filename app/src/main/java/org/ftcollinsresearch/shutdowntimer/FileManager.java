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
import java.util.Arrays;

public class FileManager {

    public static void saveState(Activity activity, String state_name, JSONObject jsonState) {
        try {
            byte[] encBytes = Base64.encode(jsonState.toString().getBytes(), Base64.DEFAULT);
            String filename = activity.getApplicationContext().getPackageName() + "." + state_name;
            FileOutputStream fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(encBytes);
            fos.close();

            Log.d("FCR", "saveState:  " + state_name);
            Log.d("FCR", "      JSON  " + jsonState.toString());
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

            Log.d("FCR", "restoreState:  " + state_name);
            Log.d("FCR", "    JSON  " + json.toString());
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

    public static void deleteBadFiles(Activity activity) {
        File dir = activity.getFilesDir();
        String[] files = activity.getApplication().fileList();
        String prefix = activity.getApplicationContext().getPackageName();
        int start = prefix.length();
        String file;
        for (int i=files.length-1; i>-1; i--) {
            file = files[i];
            if (file.length() == start) {
                (new File(dir, file)).delete();
            }
        }
    }

    public static void deleteAllFiles(Activity activity) {
        File dir = activity.getFilesDir();
        String[] files = activity.getApplication().fileList();
        for (int i=files.length-1; i>-1; i--) {
            (new File(dir, files[i])).delete();
        }
    }

    public static String[] getAllFiles(Activity activity) {
        String[] files = activity.getApplication().fileList();
        String prefix = activity.getApplicationContext().getPackageName() + ".";
        int start = prefix.length();
        int length = files.length;
        String file;
        for (int i=length-1;i>-1;i--) {
            file = files[i];
            files[i] = file.substring(start);
        }

        Arrays.sort(files);
        return files;
    }

    public static void deleteFile(Activity activity, String fileName) {
        File dir = activity.getFilesDir();
        (new File(dir, fileName)).delete();
    }

}
