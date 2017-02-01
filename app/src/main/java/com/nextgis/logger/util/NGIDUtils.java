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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.nextgis.logger.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public final class NGIDUtils {
    private static final String OAUTH_NEW = "https://my.nextgis.com/oauth2/token/?grant_type=password&username=%s&password=%s&client_id=%s";
    private static final String OAUTH_REFRESH = "https://my.nextgis.com/oauth2/token/?grant_type=refresh_token&client_id=%s&refresh_token=%s";
    private static final String USER_INFO = "https://my.nextgis.com/api/v1/user_info/";
    private static final String GET = "GET";
    private static final String POST = "POST";

    public static final String PREF_ACCESS_TOKEN = "access_token";
    public static final String PREF_REFRESH_TOKEN = "refresh_token";
    public static final String PREF_USERNAME = "username";
    public static final String PREF_EMAIL = "email";
    public static final String PREF_FIRST_NAME = "first_name";
    public static final String PREF_LAST_NAME = "last_name";

    private static SharedPreferences mPreferences;

    public interface OnFinish {
        void onFinish(String data);
    }

    public static void get(Context context, String url, OnFinish callback) {
        if (mPreferences == null)
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String token = mPreferences.getString(PREF_ACCESS_TOKEN, "");
        new Load(callback).execute(url, "GET", token);
    }

    public static void getToken(Context context, String login, String password, OnFinish callback) {
        if (mPreferences == null)
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        new Load(callback).execute(null, null, null, login, password);
    }

    private static class Load extends AsyncTask<String, Void, String> {
        private OnFinish mCallback;

        Load(OnFinish callback) {
            mCallback = callback;
        }

        @Override
        protected String doInBackground(String... args) {
            String url = args[0];
            String method = args[1];
            String token = args[2];

            if (TextUtils.isEmpty(url) && TextUtils.isEmpty(token)) {
                String login = args.length > 3 ? args[3] : null;
                String password = args.length > 4 ? args[4] : null;
                try {
                    login = URLEncoder.encode(login, "UTF-8").replaceAll("\\+", "%20");
                    password = URLEncoder.encode(password, "UTF-8").replaceAll("\\+", "%20");
                } catch (UnsupportedEncodingException ignored) {}

                String accessNew = String.format(OAUTH_NEW, login, password, BuildConfig.CLIENT_ID);
                token = parseResponse(getData(accessNew, POST, null))[0];
                if (token == null)
                    return "0";
            }

            String userCheck = getData(USER_INFO, GET, token);
            if (userCheck == null) {
                String refreshToken = mPreferences.getString(PREF_REFRESH_TOKEN, "");
                String accessRefresh = String.format(OAUTH_REFRESH, BuildConfig.CLIENT_ID, refreshToken);
                token = parseResponse(getData(accessRefresh, POST, null))[0];
                if (token == null)
                    return "0";
                userCheck = getData(USER_INFO, GET, token);
            }

            saveUserInfo(userCheck);
            return getData(url, method, token);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (mCallback != null)
                mCallback.onFinish(result);
        }
    }

    private static void saveUserInfo(String userInfo) {
        try {
            JSONObject json = new JSONObject(userInfo);
            mPreferences.edit().putString(PREF_USERNAME, json.getString(PREF_USERNAME))
                        .putString(PREF_EMAIL, json.getString(PREF_EMAIL))
                        .putString(PREF_FIRST_NAME, json.getString(PREF_FIRST_NAME))
                        .putString(PREF_LAST_NAME, json.getString(PREF_LAST_NAME)).apply();
        } catch (JSONException | NullPointerException ignored) {}
    }

    private static String[] parseResponse(String response) {
        String token = null, refreshToken = null;
        try {
            JSONObject j = new JSONObject(response);
            token = j.getString("token_type") + " " + j.getString(PREF_ACCESS_TOKEN);
            refreshToken = j.getString(PREF_REFRESH_TOKEN);
            mPreferences.edit().putString(PREF_ACCESS_TOKEN, token).putString(PREF_REFRESH_TOKEN, refreshToken).apply();
        } catch (JSONException | NullPointerException ignored) {}

        return new String[]{token, refreshToken};
    }

    private static String getData(String target, String method, String token) {
        try {
            URL url = new URL(target);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            if (!TextUtils.isEmpty(token))
                conn.setRequestProperty("Authorization", token);

            conn.setRequestMethod(method);
            conn.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line, data = "";
            while ((line = in.readLine()) != null)
                data += line;
            in.close();

            return data;
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }

        return null;
    }

}
