package org.peno.b4.bikerisk;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Singleton that manages the connection with the server.
 * It stores the username and password and also provides methods to send request to the server.
 * When the server responds the Response listener gets called.
 */
public class ConnectionManager {
    private static final String TAG = "LoginManager";

    public interface ResponseListener {
        /**
         * called when server sends response -> update UI
         * @param req the original request
         * @param result if the result succeeded
         * @param response the complete response object
         */
        void onResponse(String req, Boolean result, JSONObject response);

        /**
         * called when connection is lost -> show banner
         * @param reason reason why the connection was dropped
         */
        void onConnectionLost(String reason);
    }

    private static ConnectionManager instance;

    private PrintWriter writer;
    private BufferedReader reader;
    private Socket socket;

    private Handler myHandler;
    private Context context;

    private ResponseListener responseListener;

    private String key;

    /**
     * Username of connected user.
     */
    public String user;

    private Thread commThread;

    /**
     * create a new LoginManager (should only be called if instance is null)
     * @param context the context from which the loginManager is created
     * @param listener the listener for responses
     */
    private ConnectionManager(Context context, ResponseListener listener) {
        myHandler = new Handler();
        this.context = context.getApplicationContext();
        this.responseListener = listener;
        instance = this;

        start();
    }

    /**
     * get the instance of the loginManager singleton.
     * @param context the context to create the connection manager in
     * @param listener the new listener for results
     * @return the static instance
     */
    public static ConnectionManager getInstance(Context context, ResponseListener listener) {
        if (instance == null) {
            instance = new ConnectionManager(context, listener);
        } else {
            instance.responseListener = listener;
        }
        return instance;
    }

    /**
     * get the instance of the loginManager singleton.
     * @return the static instance
     */
    public static ConnectionManager getInstance() {
        if (instance == null) {
            throw new NullPointerException("The connection manager was not initialized yet");
        }
        return instance;
    }

    //TODO: !!!!!!!!!!!rename to pause?!!!!!!!!!!!!!!
    /**
     * stops the communication thread and closes all sockets (use in onPause)
     */
    public void stop() {
        if (commThread != null)
            commThread.interrupt();
        responseListener = null;
        try {
            if (socket != null)
                socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        socket = null;
        writer = null;
        reader = null;
        Log.d(TAG, "paused, interrupted thread");
    }

    /**
     * start the communication thread.
     */
    public void start() {
        // don't start if we are already running...
        if (commThread == null) {
            commThread = new Thread(new CommunicationClass(Utils.HOST, Utils.PORT));
            commThread.start();
        }
    }

    /**
     * load the username and key from shared preferences
     * @return true if there was a previous user & key false otherwise
     */
    public boolean loadFromSharedPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        this.user = prefs.getString("user", "");
        this.key = prefs.getString("key", "");
        if (!this.user.equals("") && !this.key.equals(""))
            return true;
        return false;
    }

    /**
     * save the user and key to shared preferences
     */
    public void saveSharedPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user", user);
        editor.putString("key", key);
        editor.commit();
    }


    public void checkLogin() {
        JSONObject LogInObject = new JSONObject();
        try {
            LogInObject.put("req", "check-login");
            LogInObject.put("key", key);
            LogInObject.put("user", user);

            String message = LogInObject.toString();
            Log.d(TAG, "checking login...");
            new Thread(new WriteClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void login(String user, String pass) {
        JSONObject LogInObject = new JSONObject();
        try {
            LogInObject.put("req", "login");
            LogInObject.put("pass", pass);
            LogInObject.put("user", user);
            this.user = user;

            String message = LogInObject.toString();
            new Thread(new WriteClass(message)).start();
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void logout() {
        JSONObject LogOutObject = new JSONObject();
        try {
            LogOutObject.put("req", "logout");
            LogOutObject.put("key", key);
            LogOutObject.put("user", user);

            String message = LogOutObject.toString();
            new Thread(new WriteClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void getUserInfo(String name) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "user-info");
            JObject.put("key", key);
            JObject.put("user", user);
            JObject.put("info-user", name);

            String message = JObject.toString();
            new Thread(new WriteClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void createUser(String name, String pass, String email, int color) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "create-user");
            JObject.put("user", name);
            JObject.put("pass", pass);
            JObject.put("email", email);
            JObject.put("color", color);

            String message = JObject.toString();
            new Thread(new WriteClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void getAllPoints(String name) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "get-all-points");
            JObject.put("key", key);
            JObject.put("user", user);
            JObject.put("info-user", name);

            String message = JObject.toString();
            new Thread(new WriteClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void getStreetRank(String street) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "street-rank");
            JObject.put("key", key);
            JObject.put("user", user);
            JObject.put("street", street);

            String message = JObject.toString();
            new Thread(new WriteClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void addPoints(String street, int points) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "add-points");
            JObject.put("key", key);
            JObject.put("user", user);
            JObject.put("street", street);
            JObject.put("points", points);

            String message = JObject.toString();
            new Thread(new WriteClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void getStreet(String street) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "get-street");
            JObject.put("key", key);
            JObject.put("user", user);
            JObject.put("street", street);

            String message = JObject.toString();
            new Thread(new WriteClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void getAllStreets(LatLngBounds bounds) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "get-all-streets");
            JObject.put("key", key);
            JObject.put("user", user);
            JObject.put("neLat", bounds.northeast.latitude);
            JObject.put("neLong", bounds.northeast.longitude);
            JObject.put("swLat", bounds.southwest.latitude);
            JObject.put("swLong", bounds.southwest.longitude);

            String message = JObject.toString();
            new Thread(new WriteClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }


    /**
     * main communication class,
     *  the class starts a connection with the server and listens for responses
     */
    private class CommunicationClass implements Runnable {
        private String host;
        private int port;

        public CommunicationClass(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public void run() {
            try{
                // create socket & connect
                InetAddress address = InetAddress.getByName(host);
                socket = new Socket(address, port);
                writer = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.d(TAG, "started comm");

                while(!Thread.currentThread().isInterrupted()) {
                    if (reader == null)
                        continue;

                    String line = reader.readLine();
                    Log.d("RES", line);
                    if (line != null) {
                        try {
                            JSONObject resObj = new JSONObject(line);
                            String req = resObj.getString("req");

                            // save key and user if succeeded
                            if (req.equals("login")) {
                                if (resObj.getBoolean("res")) {
                                    ConnectionManager.this.key = resObj.getString("key");
                                    ConnectionManager.this.saveSharedPrefs();
                                }
                            }
                            if (responseListener != null)
                                myHandler.post(new LoginResultClass(req, resObj.getBoolean("res"), resObj));

                        } catch (JSONException err) {
                            err.printStackTrace();
                            // ignore wrong json..
                        }
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
                if (responseListener != null)
                    myHandler.post(new ConnectionLostClass(e.getMessage()));
            } finally {
                // clear commthread so we can restart
                commThread = null;
                Log.d(TAG, "closing sockets");
                if (socket != null && socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "closed sockets");
            }
        }
    }

    private class LoginResultClass implements Runnable {
        private Boolean result;
        private String req;
        private JSONObject response;

        public LoginResultClass(String req, Boolean result, JSONObject response) {
            this.result = result;
            this.req = req;
            this.response = response;
        }
        @Override
        public void run(){
            responseListener.onResponse(req, result, response);
        }
    }

    private class WriteClass implements Runnable {
        private String message;

        public WriteClass(String message) {
            this.message = message;
        }

        @Override
        public void run(){
            if (socket == null || !socket.isConnected() || writer == null) {
                // restart connection
                start();
            }
            // wait while connecting:
            //TODO: i!!!!!!!!!!!!!ncrease sleep time each iteration, timeout after 5  seconds -> Error activity!!!!!!!!!!!!!!!!!

            while (writer == null || socket == null || !socket.isConnected()) {
                try {
                    Thread.sleep(100);
                    start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            writer.println(message);
            writer.flush();
        }
    }

    private class ConnectionLostClass implements Runnable {

        private String error;

        public ConnectionLostClass(String error) {
            this.error = error;
        }

        @Override
        public void run() {
            responseListener.onConnectionLost(error);
        }
    }
}
