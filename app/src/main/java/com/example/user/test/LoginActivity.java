package com.example.user.test;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity{

    String password, username, credentialsFromServerString;
    Boolean rememberMeStatment;
    int idUserFromServer;
    EditText usernameEditText, passwordEditText;
    Button loginButton, registerButton;
    CheckBox rememberMe;
    String[] parameters = new String[2];

    OutputStream outputStream               = null;
    InputStream inputStream                 = null;
    HttpURLConnection httpURLConnection     = null;


    JSONObject reader                       = new JSONObject();
    JSONObject responseFromserver           = new JSONObject();

    OverallMethods overallMethods           = new OverallMethods();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        //overallMethods.setImmersiveMode(LoginActivity.this);

        final View decorView = getWindow().getDecorView();
        overallMethods.UiChangeListener(decorView);

        //Hide the status bar
        getSupportActionBar().hide();

        setContentView(R.layout.activity_login);

        usernameEditText        = (EditText)findViewById(R.id.username);
        passwordEditText        = (EditText)findViewById(R.id.password);
        loginButton             = (Button)findViewById(R.id.loginButton);
        registerButton          = (Button)findViewById(R.id.registerButton);
        rememberMe              = (CheckBox)findViewById(R.id.remember_me);

        SharedPreferences sharedPref    = getSharedPreferences("Login", Activity.MODE_PRIVATE);
        String rememberMeSharedPref     = sharedPref.getString("rememberMe", null);
        String usernameSharedPref       = sharedPref.getString("username", null);
        String passwordSharedPref       = sharedPref.getString("password", null);

        if(rememberMeSharedPref != null && rememberMeSharedPref.equals("true")) {

            usernameEditText.setText(usernameSharedPref);
            passwordEditText.setText(passwordSharedPref);
            rememberMe.setChecked(true);
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                rememberMeStatment      = (rememberMe.isChecked()) ? true : false;

                try {

                    username   = usernameEditText.getText().toString();
                    password   = passwordEditText.getText().toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                parameters[0]   = username;
                parameters[1]   = password;

                if(overallMethods.checkCredetntials(parameters))    {

                    AsyncTask loginAsyncTask = new AsyntTaskLogin();
                    loginAsyncTask.execute(new String[]{username,password, String.valueOf(rememberMeStatment)});

                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Please complete all fields", Snackbar.LENGTH_LONG)
                            .setActionTextColor(Color.RED)
                            .show();
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent goToRegisterActivity = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(goToRegisterActivity);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        overallMethods.setImmersiveMode(LoginActivity.this);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = getCurrentFocus();

        if (v != null &&
                (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) &&
                v instanceof EditText &&
                !v.getClass().getName().startsWith("android.webkit.")) {
            int scrcoords[] = new int[2];
            v.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + v.getLeft() - scrcoords[0];
            float y = ev.getRawY() + v.getTop() - scrcoords[1];

            if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom())
                overallMethods.hideKeyboard(this);
        }
        return super.dispatchTouchEvent(ev);
    }

    private class AsyntTaskLogin extends AsyncTask<String, Integer, String>  {

        ProgressDialog loginProcessDialog;
        StringBuilder response  = new StringBuilder();
        String success, userStatus;

        @Override
        protected String doInBackground(String... params) {

            /*
            *   Posting the credentials to PHP SERVER
            * */
            try {
                URL url = new URL("http://10.0.2.2/login_project/index.php");
                JSONObject loginCredentials = new JSONObject();
                loginCredentials.put("type", "login");
                loginCredentials.put("username", params[0]);
                loginCredentials.put("password", params[1]);
                String loginString = loginCredentials.toString();

                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(15000);
                httpURLConnection.setConnectTimeout(15000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                OutputStream os = httpURLConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(loginString);

                writer.flush();
                writer.close();
                os.close();

                /*
                *   Getting the response from SERVER
                * */
                int responseFromServer = httpURLConnection.getResponseCode();

                if (responseFromServer == HttpURLConnection.HTTP_OK)    {

                    /*
                    *   Reading the output/response from SERVER
                    * */
                    BufferedReader input = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String strLine = null;
                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                }

                /*
                *   Creating the JSON Object and Parse it
                * */
                reader              = new JSONObject(response.toString());
                success             = reader.getString("success");
                userStatus          = reader.getString("user_status");
                idUserFromServer    = reader.getInt("id_user");

                JSONObject credentialsFromServer = new JSONObject();
                credentialsFromServer.put("success", success);
                credentialsFromServer.put("username", username);
                credentialsFromServer.put("password", password);
                credentialsFromServer.put("id_user", idUserFromServer);
                credentialsFromServerString = credentialsFromServer.toString();

            }
            catch (IOException e) {
                e.printStackTrace();
            }catch (JSONException e) {
                e.printStackTrace();
            } finally {
                httpURLConnection.disconnect();
            }

            return credentialsFromServerString;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loginProcessDialog=new ProgressDialog(LoginActivity.this);
            loginProcessDialog.setMessage("Checking credentials");
            loginProcessDialog.show();
            loginProcessDialog.setCancelable(false);
            loginProcessDialog.setCanceledOnTouchOutside(false);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            loginProcessDialog.dismiss();


            SharedPreferences sharedPref    = getSharedPreferences("Login", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            try {
                responseFromserver      = new JSONObject(result);
                success                 = responseFromserver.getString("success");
                idUserFromServer        = responseFromserver.getInt("id_user");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(success.equals("1")) {
                if(rememberMeStatment)  {
                    editor.putString("username", username);
                    editor.putString("password", password);
                }

                editor.putString("rememberMe", rememberMeStatment.toString());
                editor.commit();

                Intent goToMainAtivity = new Intent(LoginActivity.this, MainActivity.class);
                goToMainAtivity.putExtra("userID", String.valueOf(idUserFromServer));
                startActivity(goToMainAtivity);
            } else if(success.equals("0")) {
                Snackbar.make(findViewById(android.R.id.content), "Username or password is incorect. Try again!", Snackbar.LENGTH_LONG)
                        .setActionTextColor(Color.RED)
                        .show();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            loginProcessDialog.setProgress(values[0]);
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

    }
}
