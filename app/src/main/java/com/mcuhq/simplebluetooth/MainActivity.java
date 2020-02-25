package com.mcuhq.simplebluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView mReadBuffer;
    private ImageButton mOffBtn;
    private ImageButton mListPairedDevicesBtn;
    private ImageButton mDiscoverBtn;
    private ImageButton mScanBtn;
    private ImageButton arrowUP;
    private ImageButton arrowLeft;
    private ImageButton arrowRight;
    private ImageButton arrowStop;
    private ImageButton arrowDown;
    private ImageButton sound1;
    private ImageButton sound2;
    private ImageButton sound3;
    private ImageButton sound4;
    private ImageButton sound5;
    private ImageButton arrowHeadRight;
    private ImageButton arrowHeadLeft;

    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private CheckBox mLED1;

    private final String TAG = MainActivity.class.getSimpleName();
    private Handler mHandler;
    private ConnectedThread mConnectedThread;
    private BluetoothSocket mBTSocket = null;

    private static final UUID BTMODULEUUID =  UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier



    private final static int REQUEST_ENABLE_BT = 1;
    private final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // deklaracja elementów GUI
        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mReadBuffer = (TextView) findViewById(R.id.readBuffer); // Tetowe pole odbioru transmisji z urządzenia
        mScanBtn = (ImageButton)findViewById(R.id.scan); // przycisk skanowania urządzeń bluetooth
        mOffBtn = (ImageButton)findViewById(R.id.off); // przycisk wyłączenia bluetooth
        mDiscoverBtn = (ImageButton)findViewById(R.id.discover); // przycisk wyszukiwania
        arrowUP = (ImageButton)findViewById(R.id.Send1);
        arrowLeft = (ImageButton)findViewById(R.id.Send2);
        arrowStop = (ImageButton)findViewById(R.id.Send3);
        arrowRight = (ImageButton)findViewById(R.id.Send4);
        arrowDown = (ImageButton)findViewById(R.id.Send5);
        sound1 = (ImageButton)findViewById(R.id.sound1);
        sound2 = (ImageButton)findViewById(R.id.sound2);
        sound3 = (ImageButton)findViewById(R.id.sound3);
        sound4 = (ImageButton)findViewById(R.id.sound4);
        sound5 = (ImageButton)findViewById(R.id.sound5);
        arrowHeadLeft = (ImageButton)findViewById(R.id.arrowHeadLeft);
        arrowHeadRight = (ImageButton)findViewById(R.id.arrowHeadRight);


        mLED1 = (CheckBox)findViewById(R.id.checkboxLED1);
        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // nie używane, checkbox testowy
        mDevicesListView = (ListView)findViewById(R.id.devicesListView);


        mDevicesListView.setAdapter(mBTArrayAdapter); // prtzypisanie adaptera do listy
        mDevicesListView.setOnItemClickListener(mDeviceClickListener); // przypisanie akcji po kliknięciu w element listy

        // Obsługa pytań o dostęp do bluetooth
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);


        // handler obsługujący komunikację z płytką, nadawanie i odbiór
        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8"); // odczyt bufora przychodzących wiadomości
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    mReadBuffer.setText(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){ // określanie statusu połaczenia
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else{
                        mBluetoothStatus.setText("Connection Failed");
                        Log.w("myLog", ""+msg.arg1+"}}}"+msg.arg2);
                    }

                }
            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

          /*  mLED1.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(mConnectedThread != null) //
                        mConnectedThread.write("1");
                }
            });*/

            // akcja po kliknieciu w przycisk ruchu w przód
            arrowUP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(mConnectedThread!= null){
                        mConnectedThread.write("A"); //  przesłanie kodu ruchu do przodu do urządzenia
                    }
                }

            });

            arrowLeft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread != null){
                        mConnectedThread.write("B");
                    }
                }
            });

            arrowStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread != null){
                        mConnectedThread.write("C");
                    }
                }
            });
            arrowRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread != null){
                        mConnectedThread.write("D");
                    }
                }
            });

            // akcja po kliknieciu w przycisk ruchu w tył
            arrowDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread != null){
                        mConnectedThread.write("E"); //  przesłanie kodu ruchu do tyłu do urządzenia
                    }
                }
            });
            arrowHeadRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread !=null){
                        mConnectedThread.write("F");
                    }
                }
            });

            arrowHeadRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread !=null){
                        mConnectedThread.write("G");
                    }
                }
            });
            arrowHeadRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread !=null){
                        mConnectedThread.write("H");
                    }
                }
            });
            arrowHeadRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread !=null){
                        mConnectedThread.write("I");
                    }
                }
            });
            arrowHeadRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread !=null){
                        mConnectedThread.write("J");
                    }
                }
            });
            arrowHeadRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread !=null){
                        mConnectedThread.write("K");
                    }
                }
            });
            arrowHeadRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mConnectedThread !=null){
                        mConnectedThread.write("L");
                    }
                }
            });

            // akcja po kliknięciu w przycisk skanowania urządzeń
            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v); //uruchomienie bluetooth
                }
            });

            // akcja po kliknięciu w przycisk wyłączenia bluetooth
            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff(v); // wyłaczenie bluetooth
                }
            });

         /*   mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices(v);
                }
            });*/

         // akcja po kliknięciu w przycisk wyszukiwanie urządzeń
            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover(v); // uruchowmienie wyszukiwania
                }
            });
        }
    }

    // uruchamienie bluetooth
    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                mBluetoothStatus.setText("Enabled");
            }
            else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view){
        // jeśłi aktualnie nie wyszukujemy urządzeń - rozpocznij wyszukiweanie, w przeciwnym wypadku, zakończ wyszukiwanie
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // rejestracja znale4zionych urządzeń
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // wypełnianie listy znalezionych urządzeń
    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    // lista sparowanych urządzeń / nie wykorzystywane więcej
    private void listPairedDevices(View view){
        mBTArrayAdapter.clear();
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // tworzenie listy znalezionych urządzeń
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    // akcja po kliknięciu w element na liście - ustanawianie połączenia z urządzeniem
    // 1) uruchamienia bt jeśli nie jest włączone,
    // 2) pobieraqnie adrssu MAC urządzenia
    // 3) uruchamienia wątku łączącego z wybranym urządzeniem
    // 4) uruchamienia wątku komunikacyjnego
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {


            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Pobieranie adresu MAC
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Uruchamienie wątku w celu unikknięcia blokady wątku głównego
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // ustyanawianie połaczenia z bluetooth
                    try {
                        mBTSocket.connect();

                    } catch (IOException e) { // połaczenie nie udane, obsługa błedu
                        try { //próba zamknięcia socketu po nieudanym połaczeniu
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) { // przechwycenie błedu po nie udanej próbie zamknięcia socketu
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {  // po udanym otworzeniu socketu uruchamiany jest wątek komunikacyjny
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();
                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // pobieraqnie wejściowego i wyjściowego strumienia danych, w zmiennych tymczasowych ponieważ strumienie są final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // bufor strumienia danych
            int bytes; // dane zwrócone z read()
            // nasłuchiwanie strumienia
            while (true) {
                try {
                    //odczyt z strumienia danych
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        buffer = new byte[1024];
                        SystemClock.sleep(100); // oczekiwanie na resztę danych
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes); // zapis z bufora
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // przesładnie odebranej wiadomości do widoku
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        // Nadawanie wiadomości
        public void write(String input) {
            byte[] bytes = input.getBytes();  //konwersja tekstu na tablicę bajtów
            try {
                mmOutStream.write(bytes); // przesłanie wiadomości
            } catch (IOException e) { }
        }

       // porzucanie połączenia
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
