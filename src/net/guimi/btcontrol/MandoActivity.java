/**
 * Copyright (c) 2014 Guimi
 * http://guimi.net
 */
package net.guimi.btcontrol;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class MandoActivity extends Activity {
    public static final String TAG = "MandoActivity";    
	/* Niveles de debug
	v — Verbose (lowest priority) - Eclipse/LogCat: mensajes en negro
	d — Debug					  - Eclipse/LogCat: mensajes en azul
	i — Info					  - Eclipse/LogCat: mensajes en verde
	w — Warning	                  - Eclipse/LogCat: mensajes en amarillo
	e — Error					  - Eclipse/LogCat: mensajes en rojo
	f — Fatal
	s — Silent (highest priority, on which nothing is ever printed)
	*/
    public static final boolean DEBUG = false;

    // Esta variable indica si estamos conectados
    private boolean conectado = false;
    // Guarda el ultimo envío realizado para poder comprobar si el nuevo envío es igual
    private String ultimoEnvio;

    // Mi vista
    private MandoView mMandoView;
    // Preferencias
    //--------------
    private SharedPreferences mPrefs;
    
	// BLUETOOTH
	//----------
    // De la documentación Android
	private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static final int REQUEST_ENABLE_BT = 10;
    protected static final int REQUEST_PREFERENCES = 15;

    // Variables para mantener los objetos necesarios para la comunicación
    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> mBluetoothDevices;
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothSocket mBluetoothSocket = null;
    private OutputStream mOutputStream = null;
    
    
    //****************************************************************************
    //       GESTIÓN DE EVENTOS DE ACTIVIDAD
    //****************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (DEBUG) Log.d(TAG, "+++ ON CREATE +++");

		// Generamos una vista
		mMandoView = new MandoView(this);
        // Indicamos la distribución de pantalla utilizando una instancia de MandoView
		setContentView(mMandoView);
		// Indicamos al sistema que mantenga la pantalla encendida
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
        // BLUETOOTH
		//----------
		// A partir de Android 4.3 (android minSdkVersion target 18) se utiliza BluetoothManager
		// pero yo quiero usar 4.2
		
		// Tomamos acceso al adaptador BT local
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Verificamos si existe el adaptador BT local
        if(mBluetoothAdapter==null) {
        	// El dispositivo no dispone de adaptador Bluetooth
        	// En principio el programa solo se debería instalar en dispositivos con BT
        	Log.e(TAG, "El dispositivo no dispone de Blueetooth");
        	Toast.makeText(this, R.string.error_no_BT, Toast.LENGTH_SHORT).show();
        	finish();
        } else {
        	// Verificamos si el adaptador BT está activo
        	if (mBluetoothAdapter.isEnabled()) {
        		if (DEBUG) Log.d(TAG, "... Bluetooth Activo ...");
        	} else {
        		// Si no está activo pedimos que se habilite
        		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        	}
        }

	}
	
	
    @Override
    protected void onResume() {
        super.onResume();
        
		if (DEBUG) Log.d(TAG, "+++ ON RESUME +++");

        // Establecemos escuchas para que el adaptador nos informe de lo que está pasando en cada momento
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        IntentFilter filter4 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filter5 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);
        this.registerReceiver(mReceiver, filter4);
        this.registerReceiver(mReceiver, filter5);

		// Cada vez que retomamos la actividad, intentamos conectar el dispositivo
		// Incluyendo cuando se crea la actividad
		conectaDispositivo();
		
    }

    
    @Override
    protected void onPause() {
    	super.onPause();
    	
		if (DEBUG) Log.d(TAG, "+++ ON PAUSE +++");

		// Cada vez que paramos la actividad desconectamos el dispositivo
		// Incluyendo cuando se finaliza la actividad
		
		// Si tenemos un dispositivo seleccionado 
    	if (mBluetoothDevice != null) {
    		// Si queda algo pendiente en el buffer de salida lo limpiamos
        	if (mOutputStream != null) {
        		try {
        			mOutputStream.flush();
        		} catch (IOException e) {
        			Log.e(TAG, "[onPause()] Error al limpiar el buffer de salida:" + e.getMessage() + ".");
        		}
        	}

    		// Cerramos la conexión
        	try {
        		mBluetoothSocket.close();
        	} catch (IOException e2) {
        		Log.e(TAG, "[onPause()] Error al cerrar la conexión:" + e2.getMessage() + ".");
        	}
    	}

    	// Nos des-registramos como escucha de los eventos BT
        this.unregisterReceiver(mReceiver);

	}
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Respondemos a la actividad REQUEST_ENABLE_BT
        if (requestCode == REQUEST_ENABLE_BT) {
            // Verificamos que la actividad ha finalizado correctamente (Activación de BT)
            if (resultCode == RESULT_OK) {
            	// Esto solo nos indica que ha terminado el proceso de conexión correctamente
            	// pero no nos garantiza que se haya realizado una conexión
        		//Toast.makeText(this, " BT ACTIVADO!! :-D ", Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == RESULT_CANCELED) {
        		Toast.makeText(this, R.string.bt_no_activado, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PREFERENCES) {
    		// Actualizamos las preferencias en la vista
    		mMandoView.leePrefs();
        }
    }

    
    /**
     * Este es el receptor de los mensajes broadcast de BT
     */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				if (DEBUG) Log.e(TAG, "+++ Device found +++");
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            	if (DEBUG) Log.e(TAG, "+++ Device is now connected +++");
            	conectado = true;
            	ponPrefs();
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            	if (DEBUG) Log.e(TAG, "+++ Done searching +++");
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
            	if (DEBUG) Log.e(TAG, "+++ Device is about to disconnect +++");
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            	if (DEBUG) Log.e(TAG, "+++ Device has disconnected +++");
            	conectado = false;
            } 
        }

    };
    
	
    //****************************************************************************
	//    GESTIÓN DE MENÚ
    //****************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Cargamos el menú; esto añade los elementos a la "action bar"
		getMenuInflater().inflate(R.menu.main, menu);

		// Incluimos en el menú una entrada por cada dispositivo BT emparejado (Bonded)
		//-----------------------------------------------------------------------------
		// Tomamos la lista de dispositivos emparejados		
		mBluetoothDevices = mBluetoothAdapter.getBondedDevices();
        // Si hay alguno...
        if (mBluetoothDevices.size() > 0) {
        	// Generamos entradas de menú
        	int i = 0;
        	for (BluetoothDevice device : mBluetoothDevices) {
        		// Incluimos un número único, que se corresponde con el orden en el vector,
        		// además del nombre del dispositivo
        		menu.add(0, i, 0, device.getName());
        		i++;
        	}
        }

		// Se podría incluir una opción para desconectar el BT, pero da más problemas de lo que parece
		// Así que dejamos la desconexión solo en onPause

        return true;
	}
	
    @Override
    public boolean onPrepareOptionsMenu (Menu menu){
        super.onPrepareOptionsMenu(menu);

        // Marcamos las entradas de menú de dispositivos como visibles o no dependiendo de si estamos conectados
	    for (int i = 0; i < mBluetoothDevices.toArray().length; i++) {
	    	menu.findItem(i).setVisible(!conectado);
        }
		
        return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		// Variables necesarias para los intent de ayuda (WebActivity)
    	Intent webIntent;
    	Bundle miBundle;
    	
        // Gestionamos el elemento seleccionado
        switch (item.getItemId()) {
        case R.id.main_menu_Configuracion:
    		// Creamos un nuevo "Intent" para la actividad correspondiente
    		Intent miIntent = new Intent(this,PreferenciasActivity.class);
            // Iniciamos la actividad
    		startActivityForResult(miIntent, REQUEST_PREFERENCES);
            return true;
        case R.id.main_menu_Ayuda:
			// Creamos un nuevo "Intent" para la ayuda via Web
			webIntent = new Intent(this,WebActivity.class);
			// Generamos un "fardo" (Bundle) con información para la actividad hija
	        miBundle = new Bundle();
		    miBundle.putInt("pagina", 3);
	        webIntent.putExtras(miBundle);
	        // Iniciamos la actividad "Web"
			startActivity(webIntent);
			return true;
        case R.id.main_menu_Info:
			// Creamos un nuevo "Intent" para la ayuda via Web
			webIntent = new Intent(this,WebActivity.class);
			// Generamos un "fardo" (Bundle) con información para la actividad hija
	        miBundle = new Bundle();
		    miBundle.putInt("pagina", 1);
	        webIntent.putExtras(miBundle);
	        // Iniciamos la actividad "Web"
			startActivity(webIntent);
            return true;
        default:
        	// Tomamos el dispositivo seleccionado
    		mBluetoothDevice = (BluetoothDevice) mBluetoothDevices.toArray()[item.getItemId()];
    		// Intentamos conectar el dispositivo
    		conectaDispositivo();
            return super.onOptionsItemSelected(item);
        }
    }
	
    
    //****************************************************************************
	//    FUNCIONES AUXILIARES
    //****************************************************************************
    /*
     * Esta función se encarga de realizar una conexión con el dispositivo seleccionado previamente
     * 
     * La desconexión y conexión mediante estas funciones no termina de funcionar bien
     * 
    private void desconectaDispositivo() {
		// Si tenemos un dispositivo seleccionado 
    	if (mBluetoothDevice != null) {
    		// Si queda algo pendiente en el buffer de salida lo limpiamos
        	if (mOutputStream != null) {
        		try {
        			mOutputStream.flush();
        		} catch (IOException e) {
        			Log.e(TAG, "[onPause()] Error al limpiar el buffer de salida:" + e.getMessage() + ".");
        		}
        	}

    		// Cerramos la conexión
        	try {
        		mBluetoothSocket.close();
        	} catch (IOException e2) {
        		Log.e(TAG, "[onPause()] Error al cerrar la conexión:" + e2.getMessage() + ".");
        	}
    	}

    	// Nos des-registramos como escucha de los eventos BT
        this.unregisterReceiver(mReceiver);
    }
     */

    
    /**
     * Esta función se encarga de realizar una conexión con el dispositivo seleccionado previamente
     */
    private void conectaDispositivo() {
		if (DEBUG) Log.d(TAG, "+++ conectaDispositivo +++");

		// Dirección MAC del dispositivo a conectar
    	String BTMAC;
    	
		// Si no hay ningún dispositivo seleccionado
    	if (mBluetoothDevice == null) {
    		// Comprobamos si debemos auto-conectar
    		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    		if (mPrefs.getBoolean("Auto_Conectar", false)) {
    			// Tomamos la MAC del último dispositivo conectado (si existe)
    			BTMAC = mPrefs.getString("MAC", null);
        		// Si no hay ninguno en preferencias, salimos 
            	if (BTMAC == null || BTMAC == "") {
            		return;
            	} else {
                	// Tomamos el dispositivo remoto por su MAC
            		mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(BTMAC);
            	}
    		} else {
    			// No hemos seleccionado dispositivo y no debemos auto-conectar
        		//Toast.makeText(this, R.string.utilice_menu, Toast.LENGTH_SHORT).show();
        		return;
    		}
    	} else {
    		// Tomamos la MAC del dispositivo seleccionado
    		BTMAC = mBluetoothDevice.getAddress();
    	}
		if (DEBUG) Log.i(TAG, "MAC:"+BTMAC);

    	// Intentamos conectar con el dispositivo remoto utilizando el UUID de SPP de la documentación de Android.
        try {
        	mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
        } catch (IOException e) {
            Log.e(TAG, "[conectaDispositivo()] Error al generar el socket: " + e.getMessage() + ".");
        }

        // Antes de establecer la conexión desactivamos el procedimiento de descubrimiento
        mBluetoothAdapter.cancelDiscovery();
    	
        // Establecemos la conexión.
        // Esto bloquea la aplicación y lo que se recomienda es hacerlo en un hilo aparte.
        if (DEBUG) Log.d(TAG, "... Conectando a "+mBluetoothDevice.getName()+" ...");
        Toast.makeText(this, getResources().getString(R.string.conectando_a) +" "+getNombreConectado()+"... ", Toast.LENGTH_SHORT).show();
        try {
        	mBluetoothSocket.connect();
        	if (DEBUG) Log.d(TAG, "... Conexión establecida ...");
        } catch (IOException e) {
        	try {
        		// En caso de error cerramos la conexión
        		mBluetoothSocket.close();
        	} catch (IOException e2) {
        		Log.e(TAG, "[conectaDispositivo()] Error al cerrar la conexión durante un fallo de conexión:" + e2.getMessage() + ".");
        	}
        }
    	
        // Create a data stream so we can talk to server.
        if (DEBUG) Log.d(TAG, "... Conectando la salida ...");
     
        try {
        	mOutputStream = mBluetoothSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "[conectaDispositivo()] Error al generar el OutputStream:" + e.getMessage() + ".");
        }
        
        //Toast.makeText(this, " Conectado a "+mBluetoothDevice.getName()+" :-) ", Toast.LENGTH_SHORT).show();
        // Si llegamos aquí quiere decir que no hemos encontrado ningún error
        // ...pero podríamos no tener una conexión correcta
    	
    }
    
    
    /**
     * Esta función se encarga de enviar mensajes al dispositivo remoto
     * @param mensaje
     */
    private void enviaMensajeBT(String mensaje) {
        if (DEBUG) Log.d(TAG, "Enviando: '" + mensaje + "'");
    	
    	// Verificamos si tenemos un dispositivo remoto
    	if (mBluetoothDevice == null) {
    		// No podemos llamar a Toast desde un hilo que no sea el principal
    		// Usamos un Handler
    		//Toast.makeText(this, R.string.utilice_menu, Toast.LENGTH_SHORT).show();
    		/*
        	Message msg = handler.obtainMessage();
        	msg.arg1 = 1;
        	handler.sendMessage(msg);
        	*/
    		return;
    	}
    	
        byte[] bytesMensaje = mensaje.getBytes();

        /*
        // Generamos un final de linea
		//0x0D
		//0x0A
		//0x0D0A
    	byte[] finDeLinea;
    	finDeLinea = new byte[2];
    	finDeLinea[0] = 0x0D;
    	finDeLinea[1] = 0x0A;
    	*/

        // Intentamos realizar el envío del mensaje
        try {
        	mOutputStream.write(bytesMensaje);
        	//mOutputStream.write(finDeLinea);
        } catch (IOException e) {
    		// No podemos llamar a Toast desde un hilo que no sea el principal
    		// Usamos un Handler
    		//Toast.makeText(this, R.string.error_envio, Toast.LENGTH_SHORT).show();
        	Message msg = handler.obtainMessage();
        	msg.arg1 = 2;
        	handler.sendMessage(msg);
            Log.e(TAG, "[enviaMensaje] Error al enviar mensaje. Si este error persiste pruebe a volver a emparejar el dispositivo: " + e.getMessage() + ".");
        }
        
    }

	/*
     * Cuando se lanza un Toast desde la propia actividad, si no existe este Handler el Toast es ignorado
     * Cuando se lanza un Toast desde la vista, si no existe este Handler la aplicación da el error:
     * Can't create handler inside thread that has not called Looper.prepare()
     * Desde la vista lanzamos Toast usando
       	Message msg = handler.obtainMessage();
    	msg.arg1 = 1;
    	handler.sendMessage(msg);
     */
    // Como el Handler utiliza MessageQueue para otro hilo que no es el principal no hay peligro de pérdidas
    @SuppressLint("HandlerLeak")
	private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	switch (msg.arg1){
        		case 1:
        			Toast.makeText(getApplicationContext(),R.string.utilice_menu, Toast.LENGTH_SHORT).show();
        			break;
        		case 2:
        			Toast.makeText(getApplicationContext(),R.string.error_envio, Toast.LENGTH_SHORT).show();
        			break;
        	}
        }
    };

    
	// Esta función guarda en las preferencias la MAC del último dispositivo conectado
    private void ponPrefs() {
		if (DEBUG) Log.d(TAG, "ponPrefs MAC:"+getMacConectado());
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		Editor editor = mPrefs.edit();
		editor.putString("MAC", getMacConectado());
		editor.commit();
    }

    
    //****************************************************************************
	//    FUNCIONES PÚBLICAS
    //****************************************************************************
    public boolean isConectado() {return conectado;}

    /**
     * Esta función pública se encarga de que se envíe por BT la cadena indicada
     * @param cadena
     */
    public void enviaCadena(String cadena) {
    	// No reenviamos los números
    	if ((cadena == ultimoEnvio) && (cadena == "1" || cadena == "2" || cadena == "3" || cadena == "4" || cadena == "5"
    			|| cadena == "6" || cadena == "7" || cadena == "8" || cadena == "9")) {
    		// No lo reenviamos
    	} else {
    		enviaMensajeBT(cadena);
        	ultimoEnvio = cadena;
    	}
    }

    public String getNombreConectado() {
    	if (mBluetoothDevice == null) { return null; }
    	else { return mBluetoothDevice.getName(); }
    }
    
    
    private String getMacConectado() {
    	if (mBluetoothDevice == null) { return null; }
    	else { return mBluetoothDevice.getAddress(); }
    }

    
}
