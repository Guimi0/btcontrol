/**
 * Copyright (c) 2014 Guimi
 * http://guimi.net
 */
package net.guimi.btcontrol;

import android.os.Bundle;
import android.provider.Settings.Secure;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Esta clase es la inicial de la aplicación
 * Presenta al usuario las opciones disponibles, básicamente emisor, receptor y configuración
 * 
 * @author guimi
 *
 */
public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    protected static final int REQUEST_ENABLE_BT = 10;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // Indicamos la distribución de pantalla (layout) a cargar (xml)
		setContentView(R.layout.activity_main);

		// Obtenemos el identificado del dispositivo Android
		//DatosGlobales.setAndroid_id(Secure.getString(DatosGlobales.getAppContext().getContentResolver(), Secure.ANDROID_ID));
		DatosGlobales.setAndroid_id(Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID));
        // En el emulador android_id == null
        if (DatosGlobales.getAndroid_id() == null) DatosGlobales.setAndroid_id("Guimi_emulador");
        
        // Obtenemos la versión de la aplicación
        try {
        	PackageInfo miPackageInfo = getPackageManager().getPackageInfo(DatosGlobales.getPaqueteAplicacion(), 0);
        	DatosGlobales.setVersionAplicacion(miPackageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
        	DatosGlobales.setVersionAplicacion("");
        }

		// Hacemos que el texto sea un hipervínculo
		TextView miTextoWeb = (TextView) findViewById(R.id.main_Web);
		Linkify.addLinks(miTextoWeb, Linkify.ALL);

		
        //************************************************************************
        //       SPLASH
        //************************************************************************
        // Solo lanzamos Splash la primera vez que se crea la actividad principal
        // La actividad principal se crea cada vez que el dispositivo se despierta, o rota...
        if (DatosGlobales.isMostrarSplash()) {
        	DatosGlobales.setMostrarSplash(false);
			// Creamos un nuevo "Intent" para el "Splash"
			Intent splashIntent = new Intent(this,SplashActivity.class);
	        // Iniciamos la actividad "Splash"
			startActivity(splashIntent);
        }
		
	}


	//*******************************************************
    //    MÉTODOS PARA LANZAR ACTIVIDADES
	//*******************************************************
    /** Llamado cuando se pulsa el botón Emisor */
    public void metodoPrimo(View view) {
    	Toast.makeText(this, R.string.cargando_control, Toast.LENGTH_SHORT).show();
		// Creamos un nuevo "Intent" para la actividad correspondiente
		Intent miIntent = new Intent(this,PrimoActivity.class);
        // Iniciamos la actividad
		startActivity(miIntent);
    }
    
    /** Llamado cuando se pulsa el botón Web Receptor */
    public void metodoWFD(View view) {
    	/* Botón sin uso, generado para pruebas de desarrollo
		// Creamos un nuevo "Intent" para la actividad correspondiente
		Intent miIntent = new Intent(this,BotonesActivity.class);
        // Iniciamos la actividad
		startActivity(miIntent);
		*/
    }	
    
    /** Llamado cuando se pulsa el botón Web Receptor */
    public void metodoReceptor(View view) {
    	Toast.makeText(this, R.string.cargando_control, Toast.LENGTH_SHORT).show();
        // Creamos un nuevo "Intent" para la actividad correspondiente
		Intent miIntent = new Intent(this,MandoActivity.class);
        // Iniciamos la actividad
		startActivity(miIntent);
    }	

	
    //****************************************************************************
	//    GESTIÓN DE MENÚ
    //****************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Cargamos el menú; esto añade los elementos a la "action bar"
		getMenuInflater().inflate(R.menu.main, menu);
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
    		startActivity(miIntent);
			return true;
        case R.id.main_menu_Ayuda:
			// Creamos un nuevo "Intent" para la ayuda via Web
			webIntent = new Intent(this,WebActivity.class);
			// Generamos un "fardo" (Bundle) con información para la actividad hija
	        miBundle = new Bundle();
		    miBundle.putInt("pagina", 0);
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
            return true;
        }
    }
	
}
