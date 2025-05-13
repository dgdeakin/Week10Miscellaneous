package com.example.week10miscellaneous;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherApp extends AppCompatActivity {

    EditText editText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_weather_app);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button checkWeatherButton = findViewById(R.id.checkWeatherButton);
        editText = findViewById(R.id.editTextTextCity);
        TextView textViewWeatherResult = findViewById(R.id.textViewWeatherResult);

        RequestQueue queue = Volley.newRequestQueue(this);

        checkWeatherButton.setOnClickListener(v -> {
            String jsonObjectUrl = "https://api.openweathermap.org/data/2.5/weather?q="+editText.getText().toString()+"&appid=e1471c9fa2c09a964798ce6b507a535d";

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, jsonObjectUrl, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray jsonArray = response.getJSONArray("weather");
                                for (int i=0; i < jsonArray.length(); i++){
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    textViewWeatherResult.setText(jsonObject.getString("description"));
                                }
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });

            queue.add(jsonObjectRequest);

        });




    }

}