package com.example.bob.download;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by 1510044 on 2016/8/3.
 */
public class DownloadManager {

    private static final String TAG="DownloadManager";
    private ProgressDialog mProgressDialog;
    private DownloadTask downloadTask;
    private DownloadTask02 downloadTask02;
    private TaskDelegate mTaskDelegate;

    public DownloadManager(Activity activity, TaskDelegate listener) {

        mTaskDelegate= listener;

        mProgressDialog = new ProgressDialog(activity);
        mProgressDialog.setMessage("Downloading...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
            }
        });
    }

    public void downloadFromFTP(String url){
        downloadTask= new DownloadTask();
        downloadTask.execute(url);
    }

    public void downloadFromDropbox(DropboxAPI<AndroidAuthSession> mDBApi){
        downloadTask02= new DownloadTask02();
        downloadTask02.execute(mDBApi);
    }

    private long getFileSize(FTPClient ftp, String filePath) throws Exception {
        long fileSize = 0;
        FTPFile[] files = ftp.listFiles(filePath);
        if (files.length == 1 && files[0].isFile()) {
            fileSize = files[0].getSize();
        }
        Log.e(TAG, "File size = " + fileSize);
        return fileSize;
    }

    private class DownloadTask extends AsyncTask<String, Integer, Boolean> {

        File targetFile = new File(Environment.getExternalStorageDirectory()
                + File.separator + "osmdroid" + File.separator , MapConstant.TW);
        FTPClient mFTPClient = new FTPClient();
        OutputStream outputStream = null;
        boolean success = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {

            try {
                mFTPClient.connect(params[0], MapConstant.FTP_PORT);

                if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {

                    //FTP login
                    try {
                        mFTPClient.login("anonymous", "");
                        mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
                        mFTPClient.enterLocalPassiveMode();
                    }catch (Exception e){
                        Log.e(TAG, e.toString());
                    }

                    //FTP download
                    try {
                        FileOutputStream local = new FileOutputStream(targetFile);
                        final String filePath= MapConstant.REMOTE_DIR + MapConstant.TW;
                        final long size= getFileSize(mFTPClient, filePath);

                        outputStream= new CountingOutputStream(local) {
                            @Override
                            public void beforeWrite(int count) {
                                super.beforeWrite(count);
                                int progress = Math.round((getByteCount() * 100) / size);
                                publishProgress(progress);
                            }
                        };

                        success = mFTPClient.retrieveFile(filePath, outputStream);
                    }
                    finally {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }
                    return success;
                }
            } catch(Exception e) {
                Log.e(TAG, e.toString());
            }finally {
                if (mFTPClient != null) {
                    try {
                        mFTPClient.logout();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        mFTPClient.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        protected void onProgressUpdate(Integer... progress) {
            Log.e("In progress", progress[0].toString());
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        public void onPostExecute(Boolean res) {
            if(res){
                Log.e(TAG,"download success");
                mProgressDialog.dismiss();
                mTaskDelegate.taskCompletionResult(res);
            }else{
                Log.e(TAG,"download failure");
            }
        }
    }

    private class DownloadTask02 extends AsyncTask<DropboxAPI, Integer, Boolean>{

        FileOutputStream outputStream= null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(DropboxAPI... params) {

            try {
                File targetFile = new File(Environment.getExternalStorageDirectory()
                        + File.separator + "osmdroid" + File.separator , MapConstant.TW);
                outputStream = new FileOutputStream(targetFile);
                ProgressListener mProgressLisenter = new ProgressListener() {

                    @Override
                    public void onProgress(long arg0, long arg1) {
                        // TODO Auto-generated method stub
                        publishProgress((int) (arg0 * 100 / arg1));
                    }

                    @Override
                    public long progressInterval() {
                        return 100;
                    }
                };
                DropboxAPI.DropboxFileInfo info = params[0].getFile(MapConstant.DB_TW, null, outputStream, mProgressLisenter);

            } catch (DropboxException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        protected void onProgressUpdate(Integer... progress) {
            Log.e("In progress", progress[0].toString());
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        public void onPostExecute(Boolean res) {
            if(res){
                Log.e(TAG,"download success");
                mProgressDialog.dismiss();
                mTaskDelegate.taskCompletionResult(res);
            }else{
                Log.e(TAG,"download failure");
            }
        }
    }
}
