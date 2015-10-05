package de.kaidev.githubupdatelibrary;

import org.apache.commons.io.IOUtils;

/**
 * Created by Kai on 05.10.2015.
 */
public class DownloadProgress {

    public DownloadStatus status;
    public int max;
    public int progress;

    public DownloadProgress(DownloadStatus status, int max, int progress) {
        this.status = status;
        this.max = max;
        this.progress = progress;
    }
}
