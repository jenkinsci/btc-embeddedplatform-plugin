package com.btc.ep.plugins.embeddedplatform.http;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openapitools.client.Configuration;

import com.google.gson.Gson;

public class HttpRequester {

    private static final int CREATED = 201;
    private static final int IN_PROGRESS = 202;
    public static String host = "http://localhost";
    public static int port = 29267;

    public static GenericResponse get(String route) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet get = new HttpGet(getBasePath() + route);
        get.setHeader("Accept", "application/json");
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                GenericResponse r = new GenericResponse();
                r.setContent(responseString);
                r.setStatus(response.getStatusLine());
                return r;
            }
        } catch (IOException e) {
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
            }
        }
        return null;
    }

    public static GenericResponse post(String route, Object payload) throws IOException {
        String json = new Gson().toJson(payload);
        return post(route, json);
    }

    public static GenericResponse post(String route, String json) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(getBasePath() + route);
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept", "application/json");
        post.setEntity(new StringEntity(json));
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                GenericResponse r = new GenericResponse();
                r.setContent(responseString);
                r.setStatus(response.getStatusLine());
                return r;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpClient.close();
        }
        return null;
    }

    /**
     * Returns the response object (if available) or at least a map containing the key 'statusCode'.<br><br>
     *
     * 201 -> 'uid' -> string (operation complete + object id)<br>
     * 202 -> 'message' -> string, 'progressDone' -> double
     *
     * @param jobId the jobId to retrieve the progress for
     * @return
     */
    @SuppressWarnings ("unchecked")
    public static Map<String, Object> getProgress(String jobId) {
        Map<String, Object> responseObject = new HashMap<>();
        GenericResponse r;
        r = HttpRequester.get("/ep/progress/" + jobId);
        String responseString = r.getContent();
        int statusCode = r.getStatus().getStatusCode();
        responseObject.put("statusCode", statusCode);
        switch (statusCode) {
            case 200:
                break;
            case 201: // 201 -> 'uid' -> string (operation complete + object id)
            case 202: // 202 -> 'message' -> string, 'progressDone' -> int 
                if (responseString.startsWith("[")) {
                    List<Object> list = new Gson().fromJson(responseString, List.class);
                    responseObject.put("list", list);
                    return responseObject;
                } else {
                    Map<String, Object> map = new Gson().fromJson(responseString, Map.class);
                    responseObject.putAll(map);
                    return responseObject;
                }
            default:
                System.err.println(r.getStatus().getStatusCode() + ": " + r.getStatus().getReasonPhrase());
                break;
        }
        return null;
    }

    /**
     *
     *
     * @return
     */
    private static String getBasePath() {
        String basePath = Configuration.getDefaultApiClient().getBasePath();
        if (basePath == null) {
            basePath = host + ":" + port;
        }
        return basePath;

    }

    /**
     * Waits for the job to complete and returns the created object (if available) or null.
     * Outputs are printed to System.out.
     *
     * @param jobId the job to wait for
     * @return the created object (if available) or null
     */
    public static Object waitForCompletion(String jobId, String expectedObjectName) {
        return waitForCompletion(jobId, expectedObjectName, System.out);
    }

    /**
     * Waits for the job to complete and returns the created object (if available) or null.
     * Outputs are printed to System.out.
     *
     * @param jobId the job to wait for
     * @return the created object (if available) or null
     */
    public static Object waitForCompletion(String jobId) {
        return waitForCompletion(jobId, "uid", System.out);
    }

    /**
     * Waits for the job to complete and returns the created object (if available) or null.
     *
     * @param jobId the job to wait for
     * @param topic name of the topic being processed (may be null)
     * @param out the print stream to use to report information
     * @return the created object (if available) or null
     */
    public static Object waitForCompletion(String jobId, String expectedObjectName, PrintStream out) {
        Object createdObject = null;
        double oldProgressDone = 0d;
        while (true) {
            Map<String, Object> progress = getProgress(jobId);
            if (progress != null) {
                int statusCode = (int)progress.get("statusCode");
                if (statusCode == CREATED) {
                    createdObject = progress.get(expectedObjectName);
                }
                if (statusCode != IN_PROGRESS) { // 202 -> in progress
                    break;
                }
                // job still in progress:
                if (progress.containsKey("message") && progress.containsKey("progressDone")) {
                    String message = (String)progress.get("message");
                    double progressDone = (double)progress.get("progressDone");
                    if (progressDone > oldProgressDone) {
                        System.out.println(message + " | " + progressDone + "%");
                        oldProgressDone = progressDone;
                    }
                }
            } else { // progress == null -> error
                out.println("No progress info available!");
                break;
            }
            sleep(2000);
        }
        return createdObject;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignored
        }
    }

    /**
     * Attempts GET requests to the specified route. Returns true if successful (expected status) or false on timeout.
     *
     * @param route route to connect to
     * @param expectedStatusCode the status code to expect
     * @param timeoutInSeconds the timeout after which to return false
     * @param delayInSeconds the delay between the connection attempts
     * @throws Exception
     */
    public static boolean checkConnection(String route, int expectedStatusCode, int timeoutInSeconds,
        int delayInSeconds)
        throws Exception {
        long startingTime = System.currentTimeMillis();
        while (true) {
            if (checkConnection(route, expectedStatusCode)) {
                return true;
            }
            // try again after 2 seconds
            Thread.sleep(delayInSeconds);
            if ((System.currentTimeMillis() - startingTime) > (timeoutInSeconds * 1000)) {
                System.out
                    .println("Connection to " + getBasePath() + route + " timed out after " + timeoutInSeconds + "s");
                return false;
            }
        }
    }

    public static boolean checkConnection(String route, int expectedStatusCode) {
        try {
            GenericResponse response = get(route);
            if (response.getStatus().getStatusCode() == expectedStatusCode) {
                System.out.println("Successfully connected to " + getBasePath() + route);
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
}
