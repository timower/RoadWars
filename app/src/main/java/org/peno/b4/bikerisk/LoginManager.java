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
import android.widget.Toast;


/**
 * Created by timo on 10/12/15.
 * lol, test
 */
public class LoginManager {
    public interface LoginResultListener {
        void onLoginResult(Boolean result);
        void onLoginError(String error);
    }

    private static LoginManager instance;

    private PrintWriter writer;
    private Handler myHandler;
    private Context context;
    private LoginResultListener loginListener;

    private String key;
    private String user;

    public LoginManager(Context context){
        myHandler = new Handler();
        this.context = context;
        new Thread(new CommunicationClass()).start();
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

                while(!Thread.currentThread().isInterrupted()) {
                    if (reader != null) {
                        String line = reader.readLine();
                        if (line != null) {
                            try {
                                JSONObject resObj = new JSONObject(line);
                                String req = resObj.getString("req");
                                if (req.equals("check-login")) {
                                    if (loginListener != null)
                                        myHandler.post(new LoginResultClass(resObj.getBoolean("res")));
                                } else if (req.equals("login")) {
                                    if (resObj.getBoolean("res")) {
                                        LoginManager.this.key = resObj.getString("key");
                                        LoginManager.this.saveSharedPrefs();
                                    }
                                    if (loginListener != null)
                                        myHandler.post(new LoginResultClass(resObj.getBoolean("res")));
                                }
                            } catch (JSONException err) {
                                err.printStackTrace();
                            }
                            myHandler.post(new toastClass(line));
                        }
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
                if (loginListener != null)
                    myHandler.post(new ErrorRedirectClass(e.getMessage()));
                //TODO: redirect to error page
            }
        }
    }

    private class toastClass implements Runnable {
        private String message;
        public toastClass(String message) {
            this.message = message;
        }
        @Override
        public void run(){
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    private class LoginResultClass implements Runnable {
        private Boolean result;
        public LoginResultClass(Boolean result) {
            this.result = result;
        }
        @Override
        public void run(){
            loginListener.onLoginResult(result);
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
