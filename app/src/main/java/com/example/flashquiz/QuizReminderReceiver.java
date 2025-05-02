package com.example.flashquiz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class QuizReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "quiz_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                == PackageManager.PERMISSION_GRANTED) {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("FlashQuiz 🔔")
                    .setContentText("C’est l’heure de ton quiz quotidien ! Viens tester tes connaissances.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManagerCompat.from(context).notify(1001, builder.build());
        }
    }
}