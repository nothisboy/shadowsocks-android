package com.github.shadowsocks;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by hackeris on 15/7/28.
 */
public class HttpManager {
    protected void log(String msg) {
        Log.d("Shadowsocks.HttpManager", msg);
    }

    protected static CookieManager cookieManager = new CookieManager();
    protected Map<String, List<String>> headerFields;
    protected List<String> cookiesHeader;

    /**
     * Executes a GET request to given URL with given parameters.
     *
     * @param urlWithAppendedbundle
     * @return
     * @throws IOException
     */
    public String get(String urlWithAppendedbundle) throws IOException {
        URL url = new URL(urlWithAppendedbundle);
        StringBuilder resultBuilder = new StringBuilder();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setUseCaches(false);
        connection.setRequestMethod("GET");

        // TODO: 15-10-5 add Cookie
        addCookie(connection);


        InputStream is = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(isr);
        String inputLine;
        while ((inputLine = bufferedReader.readLine()) != null) {
            resultBuilder.append(inputLine);
        }
        connection.disconnect();
        return resultBuilder.toString();
    }

    public String post(String urlString, Bundle bundle) throws IOException {
        StringBuilder postBody = new StringBuilder();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                log(key + ": " + bundle.getString(key));
                postBody.append(key);
                postBody.append("=");
                postBody.append(bundle.getString(key));
                postBody.append("&");
            }
// remove the last &
            postBody.delete(postBody.length() - 1, postBody.length());
        }
        return post(urlString, postBody.toString());
    }

    /**
     * Performs a HTTP POST or PATCH request with given postBody and headers.
     *
     * @param urlString
     * @param postBody
     * @param isPatchRequest - If true, then performs PATCH request, POST otherwise.
     * @return
     * @throws IOException
     */
    public String post(String urlString, String postBody, boolean isPatchRequest) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        if (isPatchRequest) {
            connection.setRequestMethod("PATCH");
        } else {
            connection.setRequestMethod("POST");
        }
        byte[] bytes = postBody.getBytes();
        connection.getOutputStream().write(bytes);
        InputStream is = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(isr);
        String inputLine;
        StringBuilder resultBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null) {
            resultBuilder.append(inputLine);
        }

        // // TODO: 15-10-5 store Cookie
        storeCookie(connection);

        connection.disconnect();
        return resultBuilder.toString();
    }

    /**
     * Performs a HTTP POST with given postBody and headers.
     *
     * @param url
     * @param postBody
     * @return
     * @throws IOException
     */
    public String post(String url, String postBody)
            throws IOException {
// this is POST, so isPathRequest=false
        return post(url, postBody, false);
    }

    /**
     * Returns GET url with appended parameters.
     *
     * @param url
     * @param bundle
     * @return
     */
    public static String toGetUrl(String url, Bundle bundle) {
        if (bundle != null) {
            if (!url.endsWith("?")) {
                url = url + "?";
            }
            for (String key : bundle.keySet()) {
                url = url + key + "=" + bundle.getString(key) + "&";
            }
        }
        return url;
    }

    protected void addCookie(HttpURLConnection connection) {
        if (cookieManager.getCookieStore().getCookies().size() > 0) {
            connection.setRequestProperty("Cookie",
                    TextUtils.join(";", cookieManager.getCookieStore().getCookies()));
        }
    }

    protected void storeCookie(HttpURLConnection connection) {
        headerFields = connection.getHeaderFields();
        cookiesHeader = headerFields.get("Set-Cookie");
        if (cookiesHeader != null) {
            for (String cookie : cookiesHeader) {
                cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
            }
        }
    }
}