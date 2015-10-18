package de.kaidev.githubupdatelibrary;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.Objects;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Kai on 02.10.2015.
 */
public class GithubUpdateFragment extends Fragment {
    public interface UpdateCallbacks {
        void checkPreExecute();
        void checkException();
        void checkPostExecute(boolean needUpdate, String remoteVersion, String name, String link);
        void checkClearUI();
        void downloadPreExecute();
        void downloadProgressUpdate(DownloadProgress progress);
        void downloadException();
        void downloadPostExecute(File result);
        void downloadClearUI();
    }

    String versionName;

    private UpdateCallbacks callbacks;

    private boolean runningCheck;
    private ASyncCheck taskCheck;

    private boolean runningDownload;
    private ASyncDownload taskDownload;

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        if (!(activity instanceof UpdateCallbacks))
            throw new IllegalStateException("Activity must implement UpdateCallbacks");
        callbacks = (UpdateCallbacks) activity;
        try {
            versionName = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            throw new Error("Cant find Package");
        }

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

    public void startCheck(String link){
        System.out.println("UpdateFragment.startCheck");
        if (!runningCheck){
            taskCheck = new ASyncCheck(link);
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

    class ASyncCheck extends AsyncTask<Void, Void, String> {

        private final String link;
        boolean gotException = false;

        public ASyncCheck(String link) {
            this.link = link;
        }

        @Override
        protected void onPreExecute() {
            System.out.println("ASyncCheck.onPreExecute");
            if (callbacks != null){
                callbacks.checkPreExecute();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            System.out.println("ASyncCheck.doInBackground");
            try {
                return IOUtils.toString(new URL(link));
            } catch (IOException e) {
                e.printStackTrace();
                cancel(true);
                gotException = true;
                return null;
            }
        }


        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject data = new JSONObject(result);
                String tag_name = data.getString("tag_name").substring(1);

                List<Integer> localVersionInts = Stream.of(Arrays.asList(versionName.split("\\.")))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());

                List<Integer> remoteVersionInts = Stream.of(tag_name.split("\\."))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());

                boolean needUpdate = remoteVersionInts.get(0) > localVersionInts.get(0) ||
                        Objects.equals(remoteVersionInts.get(0), localVersionInts.get(0)) && remoteVersionInts.get(1) > localVersionInts.get(1) ||
                        Objects.equals(remoteVersionInts.get(0), localVersionInts.get(0)) && Objects.equals(remoteVersionInts.get(1), localVersionInts.get(1)) && remoteVersionInts.get(2) > localVersionInts.get(2);

                JSONArray assets = data.getJSONArray("assets");
                JSONObject entry = assets.getJSONObject(0);
                String link = entry.getString("browser_download_url");

                String name = data.getString("name");
                callbacks.checkPostExecute(needUpdate, tag_name, name, link);
            } catch (JSONException e) {
                e.printStackTrace();
                callbacks.checkException();
            }
            runningCheck = false;
        }

        @Override
        protected void onCancelled() {
            if (gotException) {
                callbacks.checkException();
            } else {
                callbacks.checkClearUI();
            }
            runningCheck = false;
        }
    }


    public boolean isRunningCheck(){
        return runningCheck;
    }

    public void startDownload(String link, String version){
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

    class ASyncDownload extends AsyncTask<Void, DownloadProgress, File>{

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
            callbacks.downloadPreExecute();
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
                publishProgress(new DownloadProgress(DownloadStatus.DownloadStart, fileSize, 0));
                while (((count = input.read(data)) != -1) && !isCancelled()){
                    total += count;
                    output.write(data, 0, count);
                    publishProgress(new DownloadProgress(DownloadStatus.DownloadRunning, fileSize, total));
                }
                if (isCancelled()){
                    file.delete();
                } else {
                    publishProgress(new DownloadProgress(DownloadStatus.DownloadFinished, 0, 0));
                }
                return file;
            } catch (Exception e){
                e.printStackTrace();
                cancel(true);
                gotException = true;
                file.delete();
                return null;
            } finally {
                IOUtils.closeQuietly(output);
                IOUtils.closeQuietly(input);
            }
        }

        @Override
        protected final void onProgressUpdate(DownloadProgress... values) {
            callbacks.downloadProgressUpdate(values[0]);
        }

        @Override
        protected void onPostExecute(File result) {
            System.out.println("ASyncDownload.onPostExecute");
            callbacks.downloadPostExecute(result);
            runningDownload = false;
        }

        @Override
        protected void onCancelled() {
            System.out.println("ASyncDownload.onCancelled");
            if (gotException) {
                callbacks.downloadException();
            } else {
                callbacks.downloadClearUI();
            }
            runningDownload = false;
        }
    }

}
