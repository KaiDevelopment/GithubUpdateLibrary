package de.kaidev.githubupdatelibrary;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Pair;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Kai on 02.10.2015.
 */
public class GithubUpdateFragment extends Fragment {
    interface UpdateCallbacks {
        void checkPreExecute();
        void checkException();
        void checkPostExecute(String[] data, boolean needUpdate);
        void checkClearUI();
        void downloadPreExecute();
        void downloadProgressUpdate(Pair<Integer, Integer> progress);
        void downloadException();
        void downloadPostExecute(File result);
        void downloadClearUI();
    }

    private UpdateCallbacks callbacks;

    private boolean runningCheck;
    private ASyncCheck taskCheck;

    private boolean runningDownload;
    private ASyncDownload taskDownload;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof UpdateCallbacks))
            throw new IllegalStateException("Activity must implement UpdateCallbacks");
        callbacks = (UpdateCallbacks) activity;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onPause() {
        System.out.println("UpdateFragment.onPause");
        super.onPause();
        if (!getActivity().isChangingConfigurations()) {
            cancelCheck();
            cancelDownload();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelCheck();
        cancelDownload();
    }

    public void startCheck(){
        System.out.println("UpdateFragment.startCheck");
        if (!runningCheck){
            taskCheck = new ASyncCheck();
            taskCheck.execute();
            runningCheck = true;
        }
    }

    public void cancelCheck() {
        System.out.println("UpdateFragment.cancelCheck");
        if (runningCheck){
            taskCheck.cancel(true);
            taskCheck = null;
            runningCheck = false;
        }
    }
    public boolean isRunningDownload(){
        return runningDownload;
    }

    class ASyncCheck extends AsyncTask<Void, Void, String[]> {

        boolean gotException = false;

        @Override
        protected void onPreExecute() {
            System.out.println("ASyncCheck.onPreExecute");
            if (callbacks != null){
                callbacks.checkPreExecute();
            }
        }

        @Override
        protected String[] doInBackground(Void... params) {
            System.out.println("ASyncCheck.doInBackground");
            String newestVersion = downloadHTTP(
                    "https://dl.dropboxusercontent.com/s/vmbhygvvfz941a6/newestversion.txt?dl=0");
            if (isCancelled()) return null;

            String patchNotes = downloadHTTP(
                    "https://dl.dropboxusercontent.com/s/fmniehotyypckws/patchnotes.txt?dl=0");
            if (isCancelled()) return null;

            String link = downloadHTTP(
                    "https://dl.dropboxusercontent.com/s/yn0qxyjsvkeamwp/link.txt?dl=0");
            if (isCancelled()) return null;
            return new String[]{newestVersion, patchNotes, link};
        }

        private String downloadHTTP(String url){
            try {
                HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();
                InputStream inputStream = httpURLConnection.getInputStream();
                String response = IOUtils.toString(inputStream, Charsets.UTF_8);
                inputStream.close();
                return response;
            } catch (Exception e) {
                e.printStackTrace();
                gotException = true;
                this.cancel(true);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (callbacks != null){
                boolean needUpdate = false;
                try {
                    String versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
                    String[] localVersion = versionName.split("\\.");
                    int[] localVersionInts = new int[localVersion.length];
                    for (int i = 0; i < localVersion.length; i++){
                        localVersionInts[i] = Integer.parseInt(localVersion[i]);
                    }

                    String[] remoteVersion = result[0].split("\\.");
                    int[] remoteVersionInts = new int[remoteVersion.length];
                    for (int i = 0; i < remoteVersion.length; i++){
                        remoteVersionInts[i] = Integer.parseInt(remoteVersion[i]);
                    }

                    needUpdate = remoteVersionInts[0] > localVersionInts[0] ||
                            remoteVersionInts[0] == localVersionInts[0] && remoteVersionInts[1] > localVersionInts[1] ||
                            remoteVersionInts[0] == localVersionInts[0] && remoteVersionInts[1] == localVersionInts[1] && remoteVersionInts[2] > localVersionInts[2];
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                callbacks.checkPostExecute(result, needUpdate);
            }
            runningCheck = false;
        }

        @Override
        protected void onCancelled() {
            if (callbacks != null){
                if (gotException) {
                    callbacks.checkException();
                } else {
                    callbacks.checkClearUI();
                }
            }
            runningCheck = false;
        }
    }


    public boolean isRunningCheck(){
        return runningCheck;
    }

    public void startDownload(String link, String version){
        System.out.println("link = [" + link + "], version = [" + version + "]");
        if (!runningDownload){
            taskDownload = new ASyncDownload(link, version);
            taskDownload.execute();
            runningDownload = true;
        }
    }

    public void cancelDownload(){
        if (runningDownload){
            taskDownload.cancel(true);
            taskDownload = null;
            runningDownload = false;
        }
    }

    class ASyncDownload extends AsyncTask<Void, Pair<Integer, Integer>, File>{

        private final String link;
        private final String version;

        private File file;

        boolean gotException = false;

        public ASyncDownload(String link, String version) {
            this.link = link;
            this.version = version;
        }

        @Override
        protected void onPreExecute() {
            if (callbacks != null){
                callbacks.downloadPreExecute();
            }
        }

        @Override
        protected File doInBackground(Void... params) {
            OutputStream output = null;
            InputStream input = null;

            try {
                URLConnection connection = new URL(link).openConnection();
                connection.connect();
                int fileSize = connection.getContentLength();
                input = new BufferedInputStream(connection.getInputStream());

                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"vertretungsplanapp-v"+version+".apk");
                output = new BufferedOutputStream(new FileOutputStream(file));

                byte[] data = new byte[1024];

                int total = 0;
                int count;
                publishProgress(new Pair<>(-2, fileSize));
                while (((count = input.read(data)) != -1) && !isCancelled()){
                    total += count;
                    output.write(data, 0, count);
                    publishProgress(new Pair<>(total, fileSize));
                }
                if (isCancelled()){
                    file.delete();
                } else {
                    publishProgress(new Pair<>(-1, -1));
                }
                return file;
            } catch (Exception e){
                e.printStackTrace();
                cancel(true);
                gotException = true;
                file.delete();
                return null;
            } finally {
                if (output!=null){
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (input != null){
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @SafeVarargs
        @Override
        protected final void onProgressUpdate(Pair<Integer, Integer>... values) {
            if (callbacks != null){
                callbacks.downloadProgressUpdate(values[0]);
            }
        }

        @Override
        protected void onPostExecute(File result) {
            System.out.println("ASyncDownload.onPostExecute");
            if (callbacks != null){
                callbacks.downloadPostExecute(result);
            }
            runningDownload = false;
        }

        @Override
        protected void onCancelled() {
            System.out.println("ASyncDownload.onCancelled");
            if (callbacks != null){
                if (gotException) {
                    callbacks.downloadException();
                } else {
                    callbacks.downloadClearUI();
                }
            }
            runningDownload = false;
        }
    }
}
