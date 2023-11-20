package com.example.myapplication;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.eclipse.paho.client.mqttv3.*;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private EditText AdresseBrokerEditText,PortEditText, TopicEditText , SeuilEditText;
    private TextView gasValueTextView;
    private ListView listView;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> gasValuesList = new ArrayList<>();
    private SharedPreferences prefs;
    private MqttClient mqttClient;
    private double thresholdValue = 0.0;
    private Vibrator vibrator ;
    private LineChart lineChart;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AdresseBrokerEditText = findViewById(R.id.AdresseBrokerEditText);
        PortEditText = findViewById(R.id.PortEditText);
        TopicEditText = findViewById(R.id.TopicEditText);
        SeuilEditText = findViewById(R.id.SeuilEditText);
        gasValueTextView = findViewById(R.id.gasValueTextView);
        listView = findViewById(R.id.list_id);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, gasValuesList);
        listView.setAdapter(arrayAdapter);



        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set the threshold value
                String thresholdStr = SeuilEditText.getText().toString();
                if (!thresholdStr.isEmpty()) {
                    thresholdValue = Double.parseDouble(thresholdStr);
                }
                // Connect to MQTT broker
                startButtonClick(view);
            }
        });
    }
    private void connectToMQTTBroker() {
        String brokerAddress = AdresseBrokerEditText.getText().toString();
        String brokerPort = PortEditText.getText().toString();
        final String broker = "tcp://" + brokerAddress + ":" + brokerPort;
        final String clientId = "AndroidCl" + System.currentTimeMillis();

        try {
            mqttClient = new MqttClient(broker, clientId, null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.connect(options);
            // Ajout du message de connexion réussie
            if (mqttClient.isConnected()) {
                showToast("Connecté au HiveMQ Broker");
            }
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // Handle connection lost
                    showToast("Probléme de connexion");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Handle incoming messages
                    if (topic.equals(TopicEditText.getText().toString())) {
                        String gasValueStr = new String(message.getPayload());
                        displayGasValue(gasValueStr);

                        // Check if the gas value exceeds the threshold
                        double gasValue = Double.parseDouble(gasValueStr);
                        if (gasValue > thresholdValue) {
                            handleGasExceedingThreshold(gasValue);
                        }
                        gasValuesList.add(gasValueStr);
                        updateGasValuesList();
                    }
                }
                private void updateGasValuesList() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Notify the ArrayAdapter that the underlying data has changed
                            arrayAdapter.notifyDataSetChanged();
                            // Scroll the ListView to the last position
                            listView.setSelection(arrayAdapter.getCount() - 1);
                        }
                    });
                }


                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used in this example
                }
            });

            mqttClient.subscribe(TopicEditText.getText().toString(), 0);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    // Ajouter cette méthode pour afficher le Toast
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void displayGasValue(final String value) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Modifier la ligne suivante pour afficher "valeur de gaz : valeur"
                gasValueTextView.setText("valeur de gaz : " + value);

                // Afficher le Toast avec le nouveau texte
                Toast.makeText(getApplicationContext(), "valeur de gaz : " + value, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void handleGasExceedingThreshold(final double gasValue) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (gasValue > thresholdValue) {
                    // Vibration
                    vibrateDevice();
                    // Add the gas value to the list with its position
                    String gasEntry = "Valeur du gaz: " + gasValue;
                    gasValuesList.add(gasEntry);
                    afficherNotificationDepassementSeuil(gasValue);
                    Intent intent = new Intent(MainActivity.this, ChartActivity.class);
                    intent.putStringArrayListExtra("Valeurs du gaz", gasValuesList);
                    startActivity(intent);

                }
            }
        });
    }

    private void vibrateDevice() {
        // Service de vibration
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // Vibration pour  (3 seconds)
            vibrator.vibrate(3000);
        }
    }


    private void afficherNotificationDepassementSeuil(double valeurGaz) {
        // Créer un canal de notification si l'appareil utilise Android Oreo ou une version supérieure
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "CanalNotificationGaz";
            CharSequence nomCanal = "Notifications de Gaz";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel canal = new NotificationChannel(channelId, nomCanal, importance);
            NotificationManager gestionnaireNotification = getSystemService(NotificationManager.class);
            gestionnaireNotification.createNotificationChannel(canal);
        }

        // Création de la notification
        NotificationCompat.Builder constructeur = new NotificationCompat.Builder(this, "CanalNotificationGaz")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Alerte Gaz !")
                .setContentText("La valeur de gaz dépasse le seuil : " + valeurGaz)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Afficher la notification
        NotificationManagerCompat gestionnaireNotification = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Considérez d'appeler
            //    ActivityCompat#requestPermissions

            return;
        }
        gestionnaireNotification.notify(1, constructeur.build());
    }

    public void startButtonClick(View view) {
        // Set the threshold value

        String thresholdStr = SeuilEditText.getText().toString();
        if (!thresholdStr.isEmpty()) {
            thresholdValue = Double.parseDouble(thresholdStr) ;

        }

        // Connect to MQTT broker
        connectToMQTTBroker();
    }
}