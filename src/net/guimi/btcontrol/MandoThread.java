/**
 * Copyright (c) 2014 Guimi
 * http://guimi.net
 */
package net.guimi.btcontrol;

import android.graphics.Canvas;
import android.util.Log;

public class MandoThread extends Thread {
    public static final String TAG = "MandoThread";    
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
    
    // Establecemos un retardo en milisegundos entre iteraciones para no saturar innecesariamente el dispositivo
	static final int RETARDO = 10;

    // Modelo AVT (Activity, View, Thread)
    //--------------------------------------
    // Vista que controla la UI
	private MandoView mMandoView;

    // Estado
    //--------------------------------------
	private boolean enMarcha;
	public void setEnMarcha(boolean enMarcha) {this.enMarcha = enMarcha;}
	public boolean getEnMarcha() {return enMarcha;}
	
    //Nuestro Constructor recibe como parametros la referencia a SurfaceHolder y nuestra SurfaceView
	public MandoThread(MandoView view) {
		if (DEBUG) Log.d(TAG, "+++ ON CREATE +++");

		this.mMandoView = view;
		
		// Inicialmente el hilo está parado
		enMarcha = false;
	}
	
    @Override
    public void run() {
        Canvas mCanvas;
        while (enMarcha) {
        	mCanvas = null;
            try {
            	// Comenzamos a editar los pixels de la superficie
            	mCanvas = mMandoView.getHolder().lockCanvas();
                synchronized (mMandoView.getHolder()) {
                	sleep(RETARDO);
            		// Llamamos a la función que gestiona la UI
                	mMandoView.gestionaPantalla(mCanvas);
                }
            } catch (NullPointerException e) {
            	// Esto sucede al pausar la actividad, ya que se intenta pintar usando un mMandoView
            	// que ya no existe
				e.printStackTrace();
            } catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
            	// Desbloqueamos y enviamos el lienzo en la claúsula "finally"
            	// De esta forma nos aseguramos de no dejar la superficie en un estado inconsistente
            	// si se produce una excepción durante el juego
                if (mCanvas != null) {
                	mMandoView.getHolder().unlockCanvasAndPost(mCanvas);
                }
            }
        }
    }
	
}
