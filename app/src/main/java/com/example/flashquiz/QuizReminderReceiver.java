package com.example.flashquiz;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class QuizReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "quiz_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Intent pour ouvrir MainActivity au clic sur la notif
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Construire la notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_quiz)
                .setContentTitle("C'est l'heure du Quiz !")
                .setContentText("Revenez jouer à FlashQuiz et gagnez des points !")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // ouvre l'appli si on clique
                .setAutoCancel(true);            // ferme la notif quand on clique

        // Vérifie la permission
        if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                == PackageManager.PERMISSION_GRANTED) {
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
            } catch (SecurityException e) {
                Log.e("QuizReminderReceiver", "Erreur lors de l'envoi de la notification", e);
            }
        } else {
            Log.d("QuizReminderReceiver", "Permission POST_NOTIFICATIONS non accordée.");
        }
    }
}