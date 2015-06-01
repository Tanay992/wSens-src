package com.Mail;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.bluetoothsensor.R;

import java.util.Arrays;
import java.util.List;
import android.app.ProgressDialog;
import android.os.AsyncTask;

public class SendEmail extends Activity {

    public final String fromEmail = "wearsensdev@gmail.com";
    public final String fromPassword = "ucla2015";
    public final String emailSubject = "WearSens Log";
    public final String emailBody = "Text Log of Data (Attached)";

    public final Integer INVALID_ADDRESS = 101;

    protected TextView output_log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_email);
        final Button send = (Button) this.findViewById(R.id.send_email_button);
        output_log = (TextView) findViewById(R.id.output_log);

        send.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Log.i("SendMailActivity", "Send Button Clicked.");
                output_log.setText("");

                //Supports sending to multiple email addresses
                String toEmails = ((TextView) findViewById(R.id.email_addr))
                        .getText().toString();
                List toEmailList = Arrays.asList(toEmails
                        .split("\\s*,\\s*"));
                Log.i("SendMailActivity", "To List: " + toEmailList);

                new SendMailTask(SendEmail.this).execute(fromEmail,
                        fromPassword, toEmailList, emailSubject, emailBody);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_send_email, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setOutputLog (String text)
    {
        output_log.setText(text);
    }



    protected class SendMailTask extends AsyncTask {

        private ProgressDialog statusDialog;
        private SendEmail sendMailActivity;

        public SendMailTask(SendEmail activity) {
            sendMailActivity = activity;

        }

        protected void onPreExecute() {
            statusDialog = new ProgressDialog(sendMailActivity);
            statusDialog.setMessage("Getting ready...");
            statusDialog.setIndeterminate(false);
            statusDialog.setCancelable(false);
            statusDialog.show();
        }

        @Override
        protected Object doInBackground(Object... args) {
            try {
                Log.i("SendMailTask", "About to instantiate GMail...");
                HelperMail androidEmail = new HelperMail(args[0].toString(),
                        args[1].toString(), (List) args[2], args[3].toString(),
                        args[4].toString());
                androidEmail.createEmailMessage();
                publishProgress("Sending email....");

                androidEmail.sendEmail();
                publishProgress("Email Sent.");
                Log.i("SendMailTask", "Mail Sent.");
                return 0; //exited successfully

            } catch (Exception e) {
                String error_msg = e.getMessage();
                publishProgress(error_msg);
                Log.e("SendMailTask", error_msg + " message: ", e);

                if (error_msg.equals("Invalid Addresses"))
                    return INVALID_ADDRESS;

                return 1; //exited with error
            }

        }

        @Override
        public void onProgressUpdate(Object... values) {
            statusDialog.setMessage(values[0].toString());

        }

        @Override
        public void onPostExecute(Object result) {
            statusDialog.dismiss();
            if (result.equals(INVALID_ADDRESS))
                sendMailActivity.setOutputLog("Invalid Email Address");
            if (result.equals(0))
                sendMailActivity.finish();
        }

    }

}