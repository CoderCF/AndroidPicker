package com.cf.androidpicker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.cf.androidpickerlibrary.AddressPicker;
import com.cf.androidpickerlibrary.DatePicker;

public class MainActivity extends AppCompatActivity {
    private Button btn_address;
    private Button btn_date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_address = (Button) findViewById(R.id.btn_address);
        btn_date = (Button) findViewById(R.id.btn_date);

        btn_address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AddressPicker picker = new AddressPicker(MainActivity.this);

                picker.setAddressListener(new AddressPicker.OnAddressListener() {
                    @Override
                    public void onAddressSelected(String province, String city, String area) {
                        btn_address.setText(province+"-"+city+"-"+area);
                    }
                });

                picker.show();

            }
        });

        btn_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DatePicker picker = new DatePicker(MainActivity.this);

                picker.setDateListener(new DatePicker.OnDateCListener() {

                    @Override
                    public void onDateSelected(String year, String month, String day) {
                        btn_date.setText(year+"-"+month+"-"+day);
                    }
                });
                picker.show();
            }
        });

    }

}
