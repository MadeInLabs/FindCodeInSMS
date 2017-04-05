package br.com.luisfelipeas5.findcodeinsms;

import android.Manifest;
import android.animation.Animator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.telephony.SmsMessage;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_SMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int receiveSMSPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        int readSMSPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
        if (receiveSMSPermissionCheck != PackageManager.PERMISSION_GRANTED
                || readSMSPermissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(false);
                builder.setMessage(R.string.request_receive_sms_permission_explanation);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS},
                        MY_PERMISSIONS_REQUEST_SMS);
            }
        } else {
            startBroadcastReceiver();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_SMS) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBroadcastReceiver();
            } else {
                Snackbar snackbar = Snackbar.make(findViewById(R.id.layout_root),
                        R.string.request_receive_sms_permission_explanation, Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        }
    }

    private void startBroadcastReceiver() {
        BroadcastReceiver smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    SmsMessage[] messagesFromIntent = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    for (SmsMessage smsMessage : messagesFromIntent) {
                        showCode(smsMessage);
                    }
                }else {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        if (pdus != null) {
                            for (Object pdu : pdus) {
                                SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdu);
                                showCode(currentMessage);
                            }
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter);
    }

    private void showCode(SmsMessage smsMessage) {
        showCode(smsMessage.getOriginatingAddress(), smsMessage.getMessageBody());
    }

    private void showCode(String phoneNumber, String message) {
        if (message == null) {
            return;
        }
        Pattern pattern = Pattern.compile("code: (\\d{6})");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String code = matcher.group(1);
            TextView txtCode = (TextView) findViewById(R.id.txt_code);
            txtCode.append(getString(R.string.phone_number_code, phoneNumber, code));
            changeBackground();
        }
    }

    private void changeBackground() {
        View nestedScrollView = findViewById(R.id.nested_scroll_view);
        View backgroundView = findViewById(R.id.img_background);

        if (backgroundView.getVisibility() != View.VISIBLE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int centerX = (int) (nestedScrollView.getX() + nestedScrollView.getWidth()/ 2);
                int centerY = (int) (nestedScrollView.getY() + nestedScrollView.getHeight()/ 2);

                float endRadius = 100;
                if (backgroundView.getHeight() > backgroundView.getWidth()) {
                    endRadius += backgroundView.getHeight();
                } else {
                    endRadius = backgroundView.getWidth();
                }

                Animator circularReveal = ViewAnimationUtils.createCircularReveal(backgroundView, centerX, centerY, 0, endRadius);
                circularReveal.setDuration(300);
                backgroundView.setVisibility(View.VISIBLE);
                circularReveal.start();
            } else {
                AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
                alphaAnimation.setDuration(300);
                backgroundView.setVisibility(View.VISIBLE);
                backgroundView.startAnimation(alphaAnimation);
            }
        }
    }
}
