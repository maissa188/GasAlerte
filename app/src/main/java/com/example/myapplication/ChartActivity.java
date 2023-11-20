package com.example.myapplication;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
public class ChartActivity extends AppCompatActivity {
    private LineChart lineChart;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        lineChart = findViewById(R.id.lineChart);

        // Retrieve gas values from the intent
        ArrayList<String> gasValues = getIntent().getStringArrayListExtra("Valeurs du gaz");

        if (gasValues != null && !gasValues.isEmpty()) {
            updateChart(gasValues);
        } else {
            // Handle case when no gas values are passed
            // You can show a message or take appropriate action
        }

    }

    private void updateChart(ArrayList<String> gasValues) {
        ArrayList<Entry> entries = new ArrayList<>();

        for (int i = 0; i < gasValues.size(); i++) {
            String gasValueStr = gasValues.get(i);
            float gasValue = Float.parseFloat(gasValueStr.replaceAll("[^\\d.]", ""));
            entries.add(new Entry(i, gasValue));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Gas Values");
        LineData lineData = new LineData(dataSet);

        lineChart.setData(lineData);
        lineChart.getDescription().setText("Gas Values Over Time");
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.invalidate(); // Refresh the chart
    }
}
