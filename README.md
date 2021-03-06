# FindCodeInSMS

This is not a lib, just a example of simple code to get patterns from incoming SMS.

While app is running, send a message that contains the text 'code [foo]', where [foo] is a number with 6 digits.

For example, if the the sms with the text "Bacon ipsum dolor amet turducken brisket burgdoggen salami short loin leberkas flank landjaeger pork loin boudin andouille code: 123456 tongue venison. Porchetta kielbasa boudin biltong, meatloaf pancetta rump swine." is received from device, the code "123456" appears to you.

The [foo] and sender number appears in the screen. If other message comes, the text is appended to the other.

![alt tag](https://github.com/MadeInLabs/FindCodeInSMS/blob/master/findCodeInSMS.gif)

# What was done

## Request permission

First of all, we have to ask for the user the permissions to read SMS incomings.

Add the user permission in [Android Manifest](app/src/main/AndroidManifest.xml):
```java
<uses-permission android:name="android.permission.RECEIVE_SMS" />
```

And request permission at run time (for more details access the [Android Developer Guide](https://developer.android.com/training/permissions/requesting.html?hl=pt-br)) like done in the onCreate() method of [MainActivity](app/src/main/java/br/com/luisfelipeas5/findcodeinsms/MainActivity.java). In this part of the code, we first check if the user already gave the permission, if didn't, we request, otherwise the other steps are executed:
```java
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
```

After the answer of the user to the requesting permission, onRequestPermissionsResult() method is called and there we verify if the permission was granted, if did, we are ready to listen the SMS and look for code/token.

```java
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
```

## Create and register the Receiver

To listen SMS received by the device, we have to create a BroadcastReceiver (what is this? Take a look to this [guide of Android Developers](https://developer.android.com/guide/components/broadcasts.html)). We did this in the [MainActivity](app/src/main/java/br/com/luisfelipeas5/findcodeinsms/MainActivity.java) on the method startBroadcastReceiver().

When onReceive() method is called, we read the sender number and message of the SMS that was inside the intent parameter. After that, the next steps will detect some code inside the message.

```java
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
```

It was created as an anonymous subclass of BroadcastReceiver abstract class because it is a easy way to modify the UI of this activity.

To this BroadcastReceiver works, we have to register it as a receiver of SMS received, using android.provider.Telephony.SMS_RECEIVED as a intent filter(watch out with the captilized 'T'):
```java
IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
registerReceiver(smsReceiver, filter);
```

## Find the pattern inside the SMS

With a regex and the class [Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) of java, we looking for the pattern 'code: [foo]' in the text, where [foo] is a decimal with six digits.

```java
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
        }
}
```
