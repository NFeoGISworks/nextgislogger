/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2017 NextGIS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * *****************************************************************************
 */

package com.nextgis.logger.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.widget.Toast;

import com.nextgis.logger.BuildConfig;
import com.nextgis.logger.R;
import com.nextgis.logger.ui.activity.PreferencesActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class ApkDownloader extends AsyncTask<String, Integer, String> {
    private Activity mActivity;
    private ProgressDialog mProgress;
    private String mApkPath;
    private boolean mAutoClose;

    private ApkDownloader(Activity activity, boolean showToast) {
        mActivity = activity;
        mAutoClose = showToast;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mProgress = new ProgressDialog(mActivity);
        mProgress.setMessage(mActivity.getString(R.string.message_loading));
        mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgress.setIndeterminate(true);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancel(true);
                Toast.makeText(mActivity, mActivity.getString(R.string.cancel_download), Toast.LENGTH_SHORT).show();
            }
        });
        mProgress.show();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            URL url = new URL(params[0]);
            mApkPath = Environment.getExternalStorageDirectory() + "/download/" + Uri.parse(params[0]).getLastPathSegment();
            URLConnection connection = url.openConnection();
            if (url.getProtocol().equalsIgnoreCase("https"))
                connection.setRequestProperty("Authorization", params[1]);

            connection.connect();
            int fileLength = connection.getContentLength() / 1024;
            InputStream input = new BufferedInputStream(connection.getInputStream());
            OutputStream output = new FileOutputStream(mApkPath);

            byte data[] = new byte[1024];
            long total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                total += count;
                publishProgress((int) total / 1024, fileLength);
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            return null;
        } catch (MalformedURLException e) {
            return mActivity.getString(R.string.error_invalid_url);
        } catch (IOException e) {
            return mActivity.getString(R.string.error_network_unavailable);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        mProgress.dismiss();

        if (result == null) {
            Intent install = new Intent();
            install.setAction(Intent.ACTION_VIEW);
            File apk = new File(mApkPath);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(mActivity, "com.nextgis.logger.file_provider", apk);
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else
                uri = Uri.fromFile(apk);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            mActivity.startActivity(install);
        } else
            Toast.makeText(mActivity, result, Toast.LENGTH_LONG).show();

        if (mAutoClose)
            mActivity.finish();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        if (mProgress.isIndeterminate()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                mProgress.setProgressNumberFormat(String.format(Locale.getDefault(), "%.2f Mb", values[1] / 1024f));

            mProgress.setIndeterminate(false);
            mProgress.setMax(values[1]);
        }

        mProgress.setProgress(values[0]);
    }

    public static void check(final Activity activity, final boolean showToast) {
        final String token = PreferenceManager.getDefaultSharedPreferences(activity).getString(NGIDUtils.PREF_ACCESS_TOKEN, "");
        final boolean isMultiPane = activity instanceof PreferencesActivity && ((PreferencesActivity) activity).onIsMultiPane();
        if (showToast && TextUtils.isEmpty(token)) {
            Toast.makeText(activity, R.string.error_401, Toast.LENGTH_SHORT).show();
            if (!isMultiPane)
                activity.finish();
            return;
        }

        NGIDUtils.get(activity, LoggerConstants.APK_VERSION_UPDATE, new NGIDUtils.OnFinish() {
            @Override
            public void onFinish(String data) {
                if (data != null) {
                    try {
                        JSONObject json = new JSONObject(data);
                        if (json.getInt("versionCode") <= BuildConfig.VERSION_CODE) {
                            if (showToast) {
                                Toast.makeText(activity, R.string.update_no, Toast.LENGTH_SHORT).show();
                                if (!isMultiPane)
                                    activity.finish();
                            }
                            return;
                        }

                        // there is new version, create download dialog
                        final String url = json.getString("path");
                        DialogInterface.OnClickListener dcl = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int button) {
                                if (button == DialogInterface.BUTTON_POSITIVE)
                                    new ApkDownloader(activity, showToast).execute(url, token);
                                else if (showToast && !isMultiPane)
                                    activity.finish();
                            }
                        };

                        AlertDialog.Builder adb = new AlertDialog.Builder(activity);
                        String update = String.format(activity.getString(R.string.update_new), json.getString("versionName"));
                        adb.setMessage(update).setTitle(R.string.update_title)
                           .setPositiveButton(android.R.string.yes, dcl).setNegativeButton(android.R.string.no, dcl).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
