package org.peno.b4.roadwars;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
        boolean onResponse(String req, Boolean result, JSONObject response);

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

    private Lock writeLock;

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
        writeLock = new ReentrantLock();

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
        return !this.user.equals("") && !this.key.equals("");
    }

    /**
     * save the user and key to shared preferences
     */
    public void saveSharedPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user", user);
        editor.putString("key", key);
        editor.apply();
    }

    private void sendRequest(Object... kvs) {
        if (kvs.length % 2 != 0)
            throw new RuntimeException("key value pairs must be even");
        JSONObject reqObject = new JSONObject();
        try {
            for (int i = 0; i < kvs.length; i += 2) {
                reqObject.put((String)kvs[i], kvs[i+1]);
            }
            new Thread(new WriteClass(reqObject.toString())).start();
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void checkLogin() {
        sendRequest("req", "check-login", "key", this.key, "user", this.user);
    }

    public void getFriends() {
        sendRequest("req", "get-friends", "user", user, "key", key);
    }

    public void getFriendRequests() {
        sendRequest("req", "get-friend-reqs", "user", user, "key", key);
    }

    public void login(String user, String pass) {
        this.user = user;
        sendRequest("req", "login", "pass", pass, "user", user);
    }

    public void logout() {
        sendRequest("req", "logout", "key", key, "user", user);
    }

    public void getUserInfo(String name) {
        sendRequest("req", "user-info", "key", key, "user", user, "info-user", name);
    }

    public void createUser(String name, String pass, String email, int color) {
        sendRequest("req", "create-user", "user", name, "pass", pass, "email", email, "color", color);
    }

    public void getAllPoints(String name) {
        sendRequest("req", "get-all-points", "key", key, "user", user, "info-user", name);
    }

    public void getStreetRank(String street) {
        sendRequest("req", "street-rank", "key", key, "user", user, "street", street);
    }

    public void addPoints(String street, int points) {
        sendRequest("req", "add-points", "key", key, "user", user, "street", street, "points", points);
    }

    public void getStreet(String street) {
        sendRequest("req", "get-street", "key", key, "user", user, "street", street);
    }

    public void getAllStreets(LatLngBounds bounds) {
        sendRequest("req", "get-all-streets", "key", key, "user", user, "neLat", bounds.northeast.latitude,
                "neLong", bounds.northeast.longitude, "swLat", bounds.southwest.latitude,
                        "swLong", bounds.southwest.longitude);
    }

    public void addFriend(String name) {
        sendRequest("req", "add-friend", "key", key, "user", user, "name", name);
    }

    public void removeFriend(String name) {
        sendRequest("req", "remove-friend", "key", key, "user", user, "name", name);
    }

    public void acceptFriend(String name) {
        sendRequest("req", "accept-friend", "key", key, "user", user, "name", name);
    }

    public void declineFriend(String name) {
        sendRequest("req", "remove-friend-req", "key", key, "user", user, "name", name);
    }

    public void nfcFriend(String name) {
        sendRequest("req", "nfc-friend", "key", key, "user", user, "name", name);
    }

    public void getAllUsers() {
        sendRequest("req", "get-all-users", "key", key, "user", user);
    }

    public void getUnknownUsers() {
        sendRequest("req", "get-unknown-users", "key", key, "user", user);
    }

    public void startMinigame(String name, String street) {
        sendRequest("req", "start-minigame", "key", key, "user", user, "name", name, "street", street);
    }

    public void finishMinigame(String name, String street) {
        sendRequest("req", "finish-minigame", "key", key, "user", user, "name", name, "street", street);
    }

    public void stopMinigame(String name, String street) {
        sendRequest("req", "stop-minigame", "key", key, "user", user, "name", name, "street", street);
    }

    public void getOnlineUsers() {
        sendRequest("req", "get-online-users", "key", key, "user", user);
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
                    if (line != null) {
                        Log.d("RES", line);
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
            if (responseListener != null) {
                boolean responseFinished = responseListener.onResponse(req, result, response);
                if (!responseFinished) {
                    Utils.onResponse(req, result, response);
                }
            }
        }
    }

    private class WriteClass implements Runnable {
        private String message;

        public WriteClass(String message) {

            this.message = message;
        }

        @Override
        public void run(){
            writeLock.lock();
            try {

                int time = 100;
                int tot_time = 0;
                while (writer == null || socket == null || !socket.isConnected()) {
                    try {
                        start();
                        Thread.sleep(time);
                        tot_time += time;
                        time *= 2;
                        Log.d(TAG, "reconnect attempt");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (tot_time > 2000) {
                        Log.d(TAG, "connection failed");
                        return;
                    }
                }

                writer.println(message);
                writer.flush();
            } finally {
                writeLock.unlock();
            }
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
