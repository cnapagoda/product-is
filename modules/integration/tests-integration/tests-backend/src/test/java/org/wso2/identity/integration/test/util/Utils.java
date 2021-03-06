/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.identity.integration.test.util;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.user.mgt.stub.types.carbon.FlaggedName;
import org.apache.http.client.HttpClient;
import org.wso2.identity.integration.test.utils.CommonConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Utils {

    private static String RESIDENT_CARBON_HOME;

    public static  boolean nameExists(FlaggedName[] allNames, String inputName) {
        boolean exists = false;

        for (FlaggedName flaggedName : allNames) {
            String name = flaggedName.getItemName();

            if (name.equals(inputName)) {
                exists = true;
                break;
            } else {
                exists = false;
            }
        }

        return exists;
    }

    public static String getResidentCarbonHome() {
        if(StringUtils.isEmpty(RESIDENT_CARBON_HOME)){
            RESIDENT_CARBON_HOME = System.getProperty("carbon.home");;
        }
        return RESIDENT_CARBON_HOME;
    }

    public static Tomcat getTomcat(Class testClass) {
        Tomcat tomcat = new Tomcat();
        tomcat.getService().setContainer(tomcat.getEngine());
        tomcat.setPort(CommonConstants.DEFAULT_TOMCAT_PORT);
        tomcat.setBaseDir("");

        StandardHost stdHost = (StandardHost) tomcat.getHost();

        stdHost.setAppBase("");
        stdHost.setAutoDeploy(true);
        stdHost.setDeployOnStartup(true);
        stdHost.setUnpackWARs(true);
        tomcat.setHost(stdHost);

        setSystemProperties(testClass);
        return tomcat;
    }

    public static void setSystemProperties(Class classIn) {
        URL resourceUrl = classIn.getResource(File.separator + "keystores" + File.separator + "products" + File
                .separator + "wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStore", resourceUrl.getPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }

    public static void startTomcat(Tomcat tomcat, String webAppUrl, String webAppPath)
            throws LifecycleException {
        tomcat.addWebapp(tomcat.getHost(), webAppUrl, webAppPath);
        tomcat.start();
    }

    public static HttpResponse sendPOSTMessage(String sessionKey, String commonAuthUrl, String userAgent, String
            acsUrl, String artifact, String userName, String password, HttpClient httpClient) throws Exception {
        HttpPost post = new HttpPost(commonAuthUrl);
        post.setHeader("User-Agent", userAgent);
        post.addHeader("Referer", String.format(acsUrl, artifact));
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("username", userName));
        urlParameters.add(new BasicNameValuePair("password", password));
        urlParameters.add(new BasicNameValuePair("sessionDataKey", sessionKey));
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        return httpClient.execute(post);
    }

    public static HttpResponse sendRedirectRequest(HttpResponse response, String userAgent, String acsUrl, String
            artifact, HttpClient httpClient) throws IOException {
        Header[] headers = response.getAllHeaders();
        String url = "";
        for (Header header : headers) {
            if ("Location".equals(header.getName())) {
                url = header.getValue();
            }
        }

        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", userAgent);
        request.addHeader("Referer", String.format(acsUrl, artifact));
        return httpClient.execute(request);
    }

    public static String getRedirectUrl(HttpResponse response) {
        Header[] headers = response.getAllHeaders();
        String url = "";
        for (Header header : headers) {
            if ("Location".equals(header.getName())) {
                url = header.getValue();
            }
        }
        return url;
    }

    public static HttpResponse sendGetRequest(String url, String userAgent, HttpClient httpClient) throws Exception {
        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", userAgent);
        return httpClient.execute(request);
    }

    public static HttpResponse sendSAMLMessage(String url, Map<String, String> parameters, String userAgent, TestUserMode userMode, String tenantDomainParam, String tenantDomain, HttpClient httpClient) throws IOException {
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        HttpPost post = new HttpPost(url);
        post.setHeader("User-Agent", userAgent);
        for (Map.Entry<String,String> entry : parameters.entrySet()) {
            urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        if (userMode == TestUserMode.TENANT_ADMIN || userMode == TestUserMode.TENANT_USER){
            urlParameters.add(new BasicNameValuePair(tenantDomainParam, tenantDomain));
        }
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        return httpClient.execute(post);
    }

    public static String extractDataFromResponse(HttpResponse response, String key, int token)
            throws IOException {
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));
        String line;
        String value = "";

        while ((line = rd.readLine()) != null) {
            if (line.contains(key)) {
                String[] tokens = line.split("'");
                value = tokens[token];
            }
        }
        rd.close();
        return value;
    }
}
