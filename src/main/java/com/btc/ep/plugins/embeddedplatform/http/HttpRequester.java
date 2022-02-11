package com.btc.ep.plugins.embeddedplatform.http;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;

import com.google.gson.Gson;

public class HttpRequester {

	private static final int SUCCESS = 200;
    private static final int CREATED = 201;
    private static final int IN_PROGRESS = 202;
    public static String host = "http://localhost";
    public static int port = 29267;
    
    // Sometimes EP doesn't respond quickly (when it's very busy doing busy work...)
    public static int timeoutInSeconds = 10;
	private static CloseableHttpClient httpClient;
	public static PrintStream printStream;

    public static GenericResponse get(String route) {
    	if (httpClient == null) {
    		httpClient = createHttpClient();
    	}
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
        } catch (HttpHostConnectException endpointNotAvailable) {
        	// ignore, this is bound to happend when we check for EP availability
    	} catch (IOException e) {
        	e.printStackTrace();
        }
        return null;
    }

    private static CloseableHttpClient createHttpClient() {
    	RequestConfig requestConfig = RequestConfig.custom()
    			.setConnectTimeout(timeoutInSeconds * 1000)
    			.setConnectionRequestTimeout(timeoutInSeconds * 1000)
    			.setSocketTimeout(timeoutInSeconds * 1000).build();
        return HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
        
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
     * @throws ApiException 
     */
    @SuppressWarnings ("unchecked")
    public static Map<String, Object> getProgress(String jobId) throws ApiException {
        Map<String, Object> responseObject = new HashMap<>();
        GenericResponse r;
        r = HttpRequester.get("/ep/progress/" + jobId);
        if (r == null) {
        	return null;
        }
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
            	String msg = r.getStatus().getStatusCode() + ": " + r.getStatus().getReasonPhrase();
                System.err.println(msg);
                throw new ApiException("Request returned :" + msg);
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
     * 
     * <br><br><b>Best practice:</b> print what the long running task is supposed to do before starting it
     *
     * @param jobId the job to wait for
     * @return the created object (if available) or null
     */
    @SuppressWarnings("unchecked")
	public static <T> T waitForCompletion(String jobId) {
    	return (T) waitForCompletion(jobId, printStream, null, Object.class);
    }
    
    /**
     * Waits for the job to complete and returns the created object (if available) or null.
     * 
     * <br><br><b>Best practice:</b> print what the long running task is supposed to do before starting it
     *
     * @param jobId the job to wait for
     * @param out the print stream to use to report information
     * @param resultFieldName name of the topic being processed (may be null)
     * @return the created object (if available) or null
     */
    @SuppressWarnings("unchecked")
	public static <T> T waitForCompletion(String jobId, String resultFieldName) {
    	return (T) waitForCompletion(jobId, printStream, resultFieldName, Object.class);
    }
    
    
    /**
     * Waits for the job to complete and returns the created object (if available) or null.
     * 
     * <br><br><b>Best practice:</b> print what the long running task is supposed to do before starting it
     *
     * @param jobId the job to wait for
     * @param resultFieldName name of the topic being processed (may be null)
     * @param expectedResponseClass the class that you expect for the return type
     * @return the created object (if available) or null
     */
    public static <T> T waitForCompletion(String jobId, String resultFieldName, Class<T> expectedResponseClass) {
    	return (T) waitForCompletion(jobId, printStream, resultFieldName, expectedResponseClass);
    }
   
    /**
     * Waits for the job to complete and returns the created object (if available) or null.
     * 
     * <br><br><b>Best practice:</b> print what the long running task is supposed to do before starting it
     *
     * @param jobId the job to wait for
     * @param out the print stream to use to report information
     * @param resultFieldName name of the topic being processed (may be null)
     * @return the created object (if available) or null
     */
    @SuppressWarnings("unchecked")
	public static <T> T waitForCompletion(String jobId, PrintStream out, String resultFieldName) {
    	return (T) waitForCompletion(jobId, out, resultFieldName, Object.class);
    }
    
    /**
     * Waits for the job to complete and returns the created object (if available) or null.
     * 
     * <br><br><b>Best practice:</b> print what the long running task is supposed to do before starting it
     *
     * @param jobId the job to wait for
     * @param out the print stream to use to report information
     * @param resultFieldName name of the topic being processed (may be null)
     * @param expectedResponseClass the class that you expect for the return type
     * @return the created object (if available) or null
     */
	public static <T> T waitForCompletion(String jobId, PrintStream out, String resultFieldName, Class<T> expectedResponseClass) {
        Object createdObject = null;
        double oldProgressDone = 0d;
        while (true) {
            Map<String, Object> progress;
			try {
				progress = getProgress(jobId);
			} catch (ApiException e) {
				break;
			}
            if (progress != null) {
                int statusCode = (int)progress.get("statusCode");
                if (Arrays.asList(CREATED, SUCCESS).contains(statusCode)) {
                    createdObject = progress.get(resultFieldName);
                }
                if (statusCode != IN_PROGRESS) { // 202 -> in progress
                    break;
                }
                // job still in progress:
                if (progress.containsKey("message") && progress.containsKey("progress")) {
                    String message = (String)progress.get("message");
                    double progressDone = (double)progress.get("progress");
                    if (progressDone > oldProgressDone) {
                        System.out.println("[Debug output] " + message.replace("Task in progress: ", "") + " | " + progressDone + "%");
                        oldProgressDone = progressDone;
                    }
                }
            } else { // progress == null -> error
                System.out.println("[Debug output] No progress info available!");
                sleep(2000);
            }
        }
    	T resultingObject = magicJsonToJava(createdObject, expectedResponseClass);
    	return resultingObject;
    }
    
    /**
     * Converts the generic object into an object based on the targetClass.
     * 
     * @param <T> The target class type
     * @param obj the generic object (usually a List or a Map)
     * @param targetClass the target class that the object should be transformed into (must not be null)
     * @return the resulting object based on the target class
     */
	private static <T> T magicJsonToJava(Object obj, Class<T> targetClass) {
		// convert object to json
		String json = new Gson().toJson(obj);
		
		// convert json to target class
		T resultingObject = new Gson().fromJson(json, targetClass);
		return resultingObject;    		
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
                    .println("Connection attempt to " + getBasePath() + route + " timed out after " + timeoutInSeconds
                        + "s");
                return false;
            }
        }
    }

    public static boolean checkConnection(String route, int expectedStatusCode) {
        try {
            GenericResponse response = get(route);
            if (response.getStatus().getStatusCode() == expectedStatusCode) {
                //System.out.println("Successfully connected to " + getBasePath() + route);
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
    
    public static void closeHttpClient() {
    	try {
          httpClient.close();
      } catch (IOException e) {
      	e.printStackTrace();
      }
    }
}
