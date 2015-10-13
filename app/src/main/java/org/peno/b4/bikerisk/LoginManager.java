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
import java.net.UnknownHostException;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;


/**
 * Created by timo on 10/12/15.
 * lol, test
 */
public class LoginManager {

    public interface LoginResultListener {
        void loginResult(Boolean result);
    }

    private PrintWriter writer;
    private Handler myhandler;
    private Context context;
    private LoginResultListener loginListener;

    public LoginManager(Context context){
        myhandler = new Handler();
        this.context = context;
        new Thread(new CommunicationClass()).start();
    }

    void checkLogin(LoginResultListener listener, String User, String Key) {
        JSONObject LogInObject = new JSONObject();
        try {
            LogInObject.put("req", "check-login");
            LogInObject.put("key", Key);
            LogInObject.put("user", User);

            String message = LogInObject.toString();
            loginListener = listener;
            new Thread(new writeClass(message)).start();

        } catch (JSONException e) {
            e.printStackTrace();
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
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

                while(!Thread.currentThread().isInterrupted()) {
                    if (reader != null) {
                        String line = reader.readLine();
                        try {
                            JSONObject resObj = new JSONObject(line);

                            if (resObj.getString("req").equals("check-login")) {
                                if (loginListener != null)
                                    myhandler.post(new LoginResultClass(resObj.getBoolean("res")));
                            }
                        } catch (JSONException err) {
                            err.printStackTrace();
                        }
                        myhandler.post(new toastClass(line));
                    }
                }
            } catch (UnknownHostException err) {
                err.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
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
            loginListener.loginResult(result);
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
            }
            writer.println(message);
            writer.flush();
        }
    }
}
