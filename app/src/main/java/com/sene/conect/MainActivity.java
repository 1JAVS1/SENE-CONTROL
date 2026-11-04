package com.sene.conect;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends Activity {
    private static final UUID SERVICE=UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID TX=UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private BluetoothAdapter adapter; private BluetoothLeScanner scanner; private BluetoothGatt gatt;
    private final Map<String,BluetoothDevice> found=new ConcurrentHashMap<>();
    private final List<String> routes=new ArrayList<>(); private final List<String> saved=new ArrayList<>();
    private LinearLayout root,body; private TextView status,connected; private EditText routeInput;
    private BluetoothDevice active; private boolean scanning=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final int blue=Color.rgb(18,73,132), green=Color.rgb(25,165,91);
    private final ScanCallback scanCallback=new ScanCallback(){
        @Override public void onScanResult(int type, ScanResult result){ BluetoothDevice d=result.getDevice(); String n=d.getName(); if(n==null)n="SENE"; found.put(d.getAddress(),d); handler.post(()->renderDevices()); }
        @Override public void onScanFailed(int e){handler.post(()->setStatus("No se pudo buscar dispositivos BLE ("+e+")"));}
    };
    private final BluetoothGattCallback gattCallback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int s,int state){
            if(state==BluetoothProfile.STATE_CONNECTED){g.discoverServices();}
            else if(state==BluetoothProfile.STATE_DISCONNECTED){handler.post(()->{connected.setText("Sin enlace");setStatus("Bluetooth desconectado.");});}
        }
        @Override public void onServicesDiscovered(BluetoothGatt g,int s){
            BluetoothGattService svc=g.getService(SERVICE);
            handler.post(()->{if(svc!=null){connected.setText("Enlazado con SENE");setStatus("Listo para enviar rutas.");}else setStatus("Ese dispositivo no acepta rutas SENE.");});
        }
    };
    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(blue); getWindow().setNavigationBarColor(Color.BLACK);
        BluetoothManager bm=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE); adapter=bm.getAdapter(); if(adapter!=null)scanner=adapter.getBluetoothLeScanner();
        getPreferences(0).getStringSet("routes",new HashSet<>()).forEach(routes::add);
        buildUi(); requestBtPermissions();
    }
    private void buildUi(){
        root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.rgb(245,248,251));
        TextView brand=new TextView(this);brand.setText("SENE  •  CONTROL");brand.setTextColor(Color.WHITE);brand.setTextSize(25);brand.setTypeface(null,Typeface.BOLD);brand.setGravity(17);brand.setBackgroundColor(green);brand.setPadding(12,25,12,25);root.addView(brand);
        TextView subtitle=new TextView(this);subtitle.setText("Conecta tus dispositivos y administra tus rutas");subtitle.setTextColor(blue);subtitle.setTextSize(15);subtitle.setPadding(18,18,18,8);root.addView(subtitle);
        connected=new TextView(this);connected.setText("Sin enlace");connected.setTextColor(blue);connected.setTextSize(16);connected.setPadding(18,8,18,8);root.addView(connected);
        status=new TextView(this);status.setText("Activa Bluetooth en el celular.");status.setTextColor(Color.DKGRAY);status.setPadding(18,4,18,10);root.addView(status);
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(1);body.setPadding(16,8,16,24);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        section("MIS DISPOSITIVOS"); button("＋  Agregar dispositivo",green,()->startScan()); TextView devs=new TextView(this);devs.setTag("devs");body.addView(devs); 
        section("MIS RUTAS"); routeInput=new EditText(this);routeInput.setHint("Escribe una ruta");routeInput.setSingleLine(true);body.addView(routeInput);
        button("Guardar ruta",blue,()->saveRoute()); button("Enviar rutas a SENE",green,()->sendRoutes());
        TextView list=new TextView(this);list.setTag("routes");list.setTextColor(blue);list.setTextSize(15);list.setPadding(8,12,8,12);body.addView(list);renderRoutes();
        setContentView(root);
    }
    private void section(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(blue);t.setTextSize(17);t.setTypeface(null,Typeface.BOLD);t.setPadding(4,18,4,8);body.addView(t);}
    private void button(String s,int color,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));b.setMinHeight(54);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,7,0,7);body.addView(b,p);b.setOnClickListener(v->r.run());}
    private void setStatus(String s){status.setText(s);}
    private void requestBtPermissions(){if(Build.VERSION.SDK_INT>=31){if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},10);}else if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},10);}
    private void startScan(){if(adapter==null||!adapter.isEnabled()){setStatus("Activa Bluetooth en el celular.");return;}requestBtPermissions();if(scanner==null)scanner=adapter.getBluetoothLeScanner();found.clear();scanning=true;setStatus("Buscando dispositivos...");try{scanner.startScan(scanCallback);handler.postDelayed(()->{if(scanning){scanner.stopScan(scanCallback);scanning=false;setStatus(found.isEmpty()?"No se encontraron dispositivos BLE.":"Selecciona SENE cuando aparezca en la lista.");}},12000);}catch(SecurityException e){setStatus("Permiso Bluetooth requerido.");}}
    private void renderDevices(){TextView v=(TextView)body.findViewWithTag("devs");if(v==null)return;v.setText("");for(BluetoothDevice d:found.values()){String n=d.getName();if(n==null)n="Dispositivo SENE";Button b=new Button(this);b.setText(n+"  •  "+d.getAddress());b.setAllCaps(false);v.append(n+"  "+d.getAddress()+"\n");b.setOnClickListener(x->connect(d));body.addView(b,body.indexOfChild(v)+1);}}
    private void connect(BluetoothDevice d){try{if(gatt!=null)gatt.close();active=d;connected.setText("Conectando...");setStatus("Buscando servicios SENE...");gatt=d.connectGatt(this,false,gattCallback);}catch(SecurityException e){setStatus("Permiso Bluetooth requerido.");}}
    private void saveRoute(){String s=routeInput.getText().toString().trim();if(s.isEmpty())return;if(routes.size()>=20){setStatus("Rutas guardadas. Maximo 20.");return;}routes.add(s);getPreferences(0).edit().putStringSet("routes",new HashSet<>(routes)).apply();routeInput.setText("");renderRoutes();setStatus("Ruta guardada.");}
    private void renderRoutes(){TextView v=(TextView)body.findViewWithTag("routes");if(v==null)return;StringBuilder b=new StringBuilder();for(int i=0;i<routes.size();i++)b.append(i+1).append(". ").append(routes.get(i)).append("\n");v.setText(b.length()==0?"No hay rutas guardadas.":b.toString());}
    private void sendRoutes(){if(gatt==null||gatt.getService(SERVICE)==null){setStatus("Primero enlaza con SENE.");return;}if(routes.isEmpty()){setStatus("Guarda al menos una ruta.");return;}try{BluetoothGattCharacteristic c=gatt.getService(SERVICE).getCharacteristic(TX);if(c==null){setStatus("Ese dispositivo no acepta rutas SENE.");return;}String payload=String.join(".",routes);c.setValue(payload.getBytes(StandardCharsets.UTF_8));c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);boolean ok=gatt.writeCharacteristic(c);setStatus(ok?"Enviando rutas...":"No se pudo enviar.");}catch(SecurityException e){setStatus("Permiso Bluetooth requerido.");}}
    @Override protected void onDestroy(){super.onDestroy();try{if(scanner!=null&&scanning)scanner.stopScan(scanCallback);if(gatt!=null)gatt.close();}catch(Exception ignored){}}
}
