/**
 * Copyright (c) 2014 Guimi
 * http://guimi.net
 */
package net.guimi.btcontrol;

import android.app.Activity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Esta clase muestra brevemente información de la aplicación al inicio de la misma (Splash)
 * 
 * @author guimi
 *
 */
public class SplashActivity extends Activity implements OnTouchListener {
	/** Indica si el Splash sigue en pantalla **/
	protected boolean enSplash = true;
	/** Indica la duración en ms del Splash **/
	protected int tiempoSplash = 3000;
	
    /**
     * Sobreescribimos la función de creación de la actividad. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Indicamos la distribución de pantalla (layout) a cargar (xml)
        setContentView(R.layout.activity_splash);
        
        // Obtenemos la vista principal (en el fichero xml se indica con 'id') 
        LinearLayout miSplash = (LinearLayout) findViewById(R.id.splashLayout);
        // Capturamos el evento "onTouch"
        miSplash.setOnTouchListener(this);
        
        // Ponemos el título
		TextView miTextoTitulo = (TextView) findViewById(R.id.main_titulo);
		miTextoTitulo.setText(DatosGlobales.getnombreAplicacion() + " " + DatosGlobales.getVersionAplicacion());

		// Hacemos que el texto sea un hipervínculo
		TextView miTextoWeb = (TextView) findViewById(R.id.splash_web);
		Linkify.addLinks(miTextoWeb, Linkify.ALL);
		
        // Hilo para controlar el tiempo de splash
        Thread hiloSplash = new Thread() {
            @Override
            public void run() {
                try {
                    int esperado = 0;
                    while(enSplash && (esperado < tiempoSplash)) {
                        sleep(100);
                        if(enSplash) {
                        	esperado += 100;
                        }
                    }
                } catch(InterruptedException e) {
                    // do nothing
                } finally {
                    finish();
                }
            }
        };
        // Lanzamos el hilo
        hiloSplash.start();

    }

    //************************************************************************
    //       TOQUE EN LA PANTALLA
    //************************************************************************
    public boolean onTouch(View miVista, MotionEvent miEvento) {
    	// Indicamos al hilo que termine el Splash
    	enSplash = false;
    	
    	// tells the system that we handled the event and no further processing is required
        return (false);
    }

}
