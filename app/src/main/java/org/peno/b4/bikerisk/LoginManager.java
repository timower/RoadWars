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


/**
 * Created by timo on 10/12/15.
 * lol, test
 */
public class LoginManager {
    private static final String TAG = "LoginManager";

    public interface LoginResultListener {
        void onLoginResult(String req, Boolean result, JSONObject response);
        void onLoginError(String error);
    }

    private static LoginManager instance;

    private PrintWriter writer;
    private Handler myHandler;
    private Context context;
    private LoginResultListener loginListener;

    private String key;
    public String user;

    private Thread commThread;

    public LoginManager(Context context){
        myHandler = new Handler();
        this.context = context;
        start();
        instance = this;
    }

    public static LoginManager getInstance() {
        return instance;
    }

    public boolean loadFromSharedPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        this.user = prefs.getString("user", "");
        this.key = prefs.getString("key", "");
        if (!this.user.equals("") && !this.key.equals(""))
            return true;
        return false;
    }

    public void saveSharedPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user", user);
        editor.putString("key", key);
        editor.commit();
    }

    public void checkLogin(LoginResultListener listener) {
        JSONObject LogInObject = new JSONObject();
        try {
            LogInObject.put("req", "check-login");
            LogInObject.put("key", key);
            LogInObject.put("user", user);

            String message = LogInObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: retry?
        }
    }

    public void login(LoginResultListener listener, String user, String pass) {
        JSONObject LogInObject = new JSONObject();
        try {
            LogInObject.put("req", "login");
            LogInObject.put("pass", pass);
            LogInObject.put("user", user);
            this.user = user;

            String message = LogInObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();
        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: retry?
        }
    }

    public void logout(LoginResultListener listener) {
        JSONObject LogOutObject = new JSONObject();
        try {
            LogOutObject.put("req", "logout");
            LogOutObject.put("key", key);
            LogOutObject.put("user", user);

            String message = LogOutObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: retry?
        }
    }

    public void getUserInfo(LoginResultListener listener) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "user-info");
            JObject.put("key", key);
            JObject.put("user", user);

            String message = JObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: retry?
        }
    }

    public void createUser(LoginResultListener listener, String name, String pass, String email, int color) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "create-user");
            JObject.put("user", name);
            JObject.put("pass", pass);
            JObject.put("email", email);
            JObject.put("color", color);

            String message = JObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: retry?
        }
    }

    public void getAllPoints(LoginResultListener listener) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "get-all-points");
            JObject.put("key", key);
            JObject.put("user", user);

            String message = JObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: retry?
        }
    }

    public void getStreetRank(LoginResultListener listener, String street) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "street-rank");
            JObject.put("key", key);
            JObject.put("user", user);
            JObject.put("street", street);

            String message = JObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: retry?
        }
    }

    public void addPoints(LoginResultListener listener, String street, int points) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "add-points");
            JObject.put("key", key);
            JObject.put("user", user);
            JObject.put("street", street);
            JObject.put("points", points);

            String message = JObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: retry?
        }
    }

    public void getPoly(LoginResultListener listener, String street) {
        JSONObject JObject = new JSONObject();
        try {
            JObject.put("req", "get-poly");
            JObject.put("key", key);
            JObject.put("user", user);
            JObject.put("street", street);

            String message = JObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: retry?
        }
    }

    public void pause() {
        commThread.interrupt();
        Log.d(TAG, "paused, interrupted thread");
    }

    public void start() {
        commThread = new Thread(new CommunicationClass());
        commThread.start();
    }

    private class CommunicationClass implements Runnable{
        private Socket socket;
        private BufferedReader reader;

        public CommunicationClass(){

        }
        @Override
        public void run(){
            try{
                InetAddress address = InetAddress.getByName("128.199.52.178");
                this.socket = new Socket(address, 4444);
                writer = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())));
                reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                Log.d(TAG, "started comm");
                while(!Thread.currentThread().isInterrupted()) {
                    if (reader != null) {
                        String line = reader.readLine();
                        if (line != null) {
                            try {
                                JSONObject resObj = new JSONObject(line);
                                String req = resObj.getString("req");
                                if (req.equals("login")) {
                                    if (resObj.getBoolean("res")) {
                                        LoginManager.this.key = resObj.getString("key");
                                        LoginManager.this.saveSharedPrefs();
                                    }
                                    if (loginListener != null)
                                        myHandler.post(new LoginResultClass(req, resObj.getBoolean("res"), resObj));
                                } else {
                                    if (loginListener != null)
                                        myHandler.post(new LoginResultClass(req, resObj.getBoolean("res"), resObj));
                                }
                            } catch (JSONException err) {
                                err.printStackTrace();
                            }
                            myHandler.post(new logClass(line));
                        }
                    }
                }
                writer.close();
                reader.close();
                socket.close();
                Log.d(TAG, "closed sockets");
            } catch (IOException e){
                e.printStackTrace();
                if (loginListener != null)
                    myHandler.post(new ErrorRedirectClass(e.getMessage()));
            }
        }
    }

    private class logClass implements Runnable {
        private String message;
        public logClass(String message) {
            this.message = message;
        }
        @Override
        public void run(){
            Log.d(TAG, message);
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
            loginListener.onLoginResult(req, result, response);
        }
    }

    private class writeClass implements Runnable {
        private String message;
        public writeClass(String message) {
            this.message = message;
        }
        @Override
        public void run(){
            while ((!Thread.currentThread().isInterrupted()) && writer == null) {
                //TODO: fix or timeout
            }
            writer.println(message);
            writer.flush();
        }
    }

    private class ErrorRedirectClass implements Runnable {
        private String error;

        public ErrorRedirectClass(String error) {
            this.error = error;
        }

        @Override
        public void run() {
            loginListener.onLoginError(error);
        }
    }
}
