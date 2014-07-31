/**
 * Copyright (c) 2014 Guimi
 * http://guimi.net
 */
package net.guimi.btcontrol;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PrimoView extends SurfaceView implements SurfaceHolder.Callback {
    public static final String TAG = "PrimoView";    
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

    // Modelo AVT (Activity, View, Thread)
    //--------------------------------------
    // Actividad que carga esta vista
    private PrimoActivity mPrimoActivity;
    // Hilo que gestiona el ciclo de interacción
    private PrimoThread mPrimoThread;
    
    // Preferencias y señales
    //----------------------------
    private SharedPreferences mPrefs;
    private String senN0, senPA, senPI, senPD;
    
    // Estados
    //--------------------------------------
	// Variables que indican las coordenadas en donde se ha tocado la pantalla por última vez
	public int toquePosicion_x, toquePosicion_y;
	// Variable que indica si hay que gestionar un toque de pantalla
	public boolean toque;
    // Variables para guardar el contenido de los botones (posiciones)
    int contenidoBotones[] = new int[16];
    // Variable donde componemos el mensaje a enviar
	private String mensaje = null; 
	// variable que indica el modo en que nos encontramos
	// Modos: 0 - Tablero; 1 - Selección pieza; 2 - Envío mensaje
    private int modo = 0;
    // Variable que recoge si en cada momento hay un botón pulsado
	private boolean botonPulsado = false;
	// Variable que recoge el código del botón pulsado
	// De 0 a 15 son las posiciones; 16 es el botón de ejecutar; 17 significa ningun botón
	private int botonPulsadoCodigo=17;

    // Dibujo
    //--------------------------------------
	// Elementos de dibujo
	private Paint paintCirculo = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintRectVerde = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintRectSeleccion = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintTextoInfo = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintEnv = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintToque = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintTextoDebug = new Paint();
	private RectF rectanguloVerde; // Necesario para dibujar el rectángulo verde de acciones
	//private RectF rectanguloSeleccion; // Rectangulo que tapa el tablero durante la selección de pieza
	private Bitmap bitmapFondo = null; 
	private Bitmap bitmapFirma = null; 
	private Bitmap bitmapLogo = null; 
	private Bitmap bitmapPrimoAccion = null; 
	private Bitmap bitmapPrimoFlecha = null; 
	private Bitmap bitmapPrimoGiro = null; 
	private Bitmap bitmapPrimoPosicion = null; 
	private Bitmap bitmapPrimoDelante = null; 
	private Bitmap bitmapPrimoIzquierda = null; 
	private Bitmap bitmapPrimoDerecha = null; 
	private Bitmap bitmapPrimoFlecha_i = null; 
	private Bitmap bitmapPrimoGiro_i = null; 
	private Bitmap bitmapPrimoPosicion_i = null; 
	private Bitmap bitmapPrimoDelante_i = null; 
	private Bitmap bitmapPrimoIzquierda_i = null; 
	private Bitmap bitmapPrimoDerecha_i = null; 
	// Variables para describir los elementos a dibujar
	private int canvasAncho, canvasAlto, rejilla;
	private int chivatoConectado_radio, chivatoConectado_x, chivatoConectado_y;
    private int mTexto_y, circuloToque_radio;

    
    // Función de creación de la vista
	public PrimoView(PrimoActivity primoActivity) {
		super(primoActivity);
		
		if (DEBUG) Log.d(TAG, "+++ ON CREATE +++");

		// Obtenemos mi actividad
		mPrimoActivity = primoActivity;
		
        // Añadimos esta pantalla (vista) a la pila SurfaceHolder
        getHolder().addCallback(this);
        
        // Establecemos parámetros de dibujo
        definePaints();
		
        // Leemos las preferencias y tomamos las señales
        leePrefs();
        
        // Generamos nuestro hilo de juego
		mPrimoThread = new PrimoThread(this);
	}
	
	
	//*******************************************************
    //    MÉTODOS AUTOMÁTICOS
	//*******************************************************
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		if (DEBUG) Log.v(TAG, "+++ surfaceChanged +++");
		// No hacemos nada de particular
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		if (DEBUG) Log.d(TAG, "+++ surfaceCreated +++");

    	// Verifico si el hilo ya está vivo
        if (!mPrimoThread.isAlive()) {
        	// Si no, lo creamos
        	mPrimoThread = new PrimoThread(this);
        }
        
       // Ponemos el hilo en marcha
		mPrimoThread.setEnMarcha(true);
		mPrimoThread.start();

		// Inicializamos el contenido de los botones
		for (int i=0;i<contenidoBotones.length;i++) {contenidoBotones[i]=0;}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		if (DEBUG) Log.d(TAG, "+++ surfaceDestroyed +++");

    	// Generamos una variable auxiliar que nos indica si debemos reintentar matar el proceso
        boolean reintentar = true;

        // Le decimos al hilo que termine y espere a acabar,
    	//   si no lo hiciésemos, el usuario podría tocar la superficie después de que volvamos
        mPrimoThread.setEnMarcha(false);
        while (reintentar) {
            try {
            	mPrimoThread.join();
                reintentar = false;
            } catch (InterruptedException e) {
                // Probamos una y otra vez hasta que lo consigamos
            }
        }
        
		if (DEBUG) Log.d(TAG, "+++ Hilo terminado +++");
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Leemos el toque
		toquePosicion_x = (int) event.getX();
		toquePosicion_y = (int) event.getY();

		//if (DEBUG) Log.i(TAG, "onTouchEvent ("+toquePosicion_x+","+toquePosicion_y+")");

		// Registramos si estamos en toque
		// Lo ponemos en true en el momento de toque (down) y lo ponemos a false en el momento de levantado (up) y cuando nos interesa durante el programa
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:// Cuando se toca la pantalla
				toque = true;
				//if (DEBUG) Log.e(TAG, "onTouchEvent ACTION_DOWN - Se toco la pantalla ");
				break;
			case MotionEvent.ACTION_MOVE:// Cuando se desplaza el dedo por la pantalla
				//toque = true;//Si ponemos esto no podemos forzar toque=false
				//if (DEBUG) Log.e(TAG, "onTouchEvent ACTION_MOVE - Nos desplazamos por la pantalla ");
				break;
			case MotionEvent.ACTION_UP:// Cuando levantamos el dedo de la pantalla que estabamos toque
				toque = false;
				//if (DEBUG) Log.e(TAG, "onTouchEvent ACTION_UP - Ya no tocamos la pantalla ");
				break;
			case MotionEvent.ACTION_CANCEL:
				toque = false;
				//if (DEBUG) Log.e(TAG, "onTouchEvent ACTION_CANCEL");
				break;
			case MotionEvent.ACTION_OUTSIDE:
				toque = false;
				//if (DEBUG) Log.e(TAG, "onTouchEvent ACTION_OUTSIDE");
				break;
			default:
				break;
		}

		return true;
	}
	
	/**
	 * Cada vez que tenemos un nuevo tamaño de canvas, redimensionamos todos los elementos de dibujo para adaptarnos
	 */
	@Override
	protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
	    super.onSizeChanged(width, height, oldwidth, oldheight);

		// CALCULO DE DIMENSIONES Y POSICIONES
		//------------------------------------
	    // Rejilla
	    //------------
		// Dimensiones del Canvas
		canvasAncho = width;
		canvasAlto = height;
		// Generamos una rejilla en base al tamaño del canvas.
		// Dividimos el alto en 37 partes y sumamos uno (tenemos margen). Al estar la pantalla en horizontal nos basta con controlar el alto.
		// Todos los elementos serán proporcionales a la rejilla creada (y al canvas)
		rejilla = canvasAlto/37;
		rejilla++;
		// TODO Comprobar 100% si nos cabe el dibujo. A no ser que el ancho sea solo ligeramente mayor que el alto (pantalla casi cuadrada) no hay problema
		//if (rejilla*60 < canvasAncho){...}
		
		
		// Ajustamos tamaños de los elementos de dibujo
		//----------------------------------------------
		// Paints y figuras
		//-------------------
		paintRectVerde.setStrokeWidth(rejilla/2);
		paintToque.setStrokeWidth(rejilla/3);
		circuloToque_radio = rejilla*3;
		// Rectangulo necesario para dibujar el rectángulo verde de acciones
		rectanguloVerde = new RectF(rejilla*8, rejilla*27, rejilla*46, rejilla*35);
		// Rectangulo necesario para dibujar el rectángulo de selección de pieza
		//rectanguloSeleccion = new RectF(rejilla*4, rejilla*1, rejilla*55, rejilla*35);

		// Chivato
		//--------------
        chivatoConectado_radio = canvasAlto/30;
        chivatoConectado_x = canvasAncho - (chivatoConectado_radio*2);
        chivatoConectado_y = chivatoConectado_radio*2;

		// Texto
		//-------
		// Ajustamos el tamaño de letra a la cuadrícula
	    int size = 0;       
		Rect bounds = new Rect();
		int alturaTexto = 0;
		float alturaObjetivo = (float)rejilla*1.25f;
		paintTextoInfo.setTextSize(size);
	    do {
	    	paintTextoInfo.setTextSize(++ size);
	    	paintTextoInfo.getTextBounds("A", 0, "A".length(), bounds);
			alturaTexto = bounds.bottom - bounds.top;
			mTexto_y=chivatoConectado_y+(chivatoConectado_radio*2)+alturaTexto;
	    } while(alturaTexto < alturaObjetivo);
		// Texto para debug
		paintTextoDebug.setTextSize(size);

		// Escalamos las Imágenes
		//-----------------------------
		// Generamos un bitmap para escalar las imágenes
		Bitmap unscaledBitmap = null;
		
		// Imagen del fondo
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.light_wood_texture);
		bitmapFondo = Bitmap.createScaledBitmap(unscaledBitmap, canvasAncho, canvasAlto, true);

		// Imágenes logo y firma
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
		bitmapLogo = Bitmap.createScaledBitmap(unscaledBitmap, canvasAncho*2/5, ((unscaledBitmap.getHeight()*(canvasAncho*2/5))/unscaledBitmap.getWidth()), true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.guimi_firma);
		bitmapFirma = Bitmap.createScaledBitmap(unscaledBitmap, ((unscaledBitmap.getWidth()*(rejilla*3/2))/unscaledBitmap.getHeight()), rejilla*3/2, true);
	
		// Imagenes posición y flechas
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_posicion_d);
		bitmapPrimoPosicion = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_flecha_d);
		bitmapPrimoFlecha = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_giro_1);
		bitmapPrimoGiro = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*5, rejilla*9, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_posicion_i);
		bitmapPrimoPosicion_i = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_flecha_i);
		bitmapPrimoFlecha_i = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_giro_2);
		bitmapPrimoGiro_i = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*5, rejilla*9, true);
		
		// Imágenes comandos
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_accion);
		bitmapPrimoAccion = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_recto_d);
		bitmapPrimoDelante = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_izquierda_d);
		bitmapPrimoIzquierda = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_derecha_d);
		bitmapPrimoDerecha = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_recto_i);
		bitmapPrimoDelante_i = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_izquierda_i);
		bitmapPrimoIzquierda_i = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_derecha_i);
		bitmapPrimoDerecha_i = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
				
	}

	
	//*******************************************************
    //    LÓGICA DE PROGRAMA
	//*******************************************************
	public void gestionaPantalla (Canvas canvas) {		
		// FONDO Y FIRMA
		//------------------
		canvas.drawBitmap(bitmapFondo, 0, 0, null);
		canvas.drawBitmap(bitmapFirma, canvasAncho - bitmapFirma.getWidth() -rejilla, canvasAlto - bitmapFirma.getHeight() -rejilla, null);

		
		// DEBUG
		//---------
		if (DEBUG) {
			paintTextoDebug.setColor(getResources().getColor(R.color.negro));
			canvas.drawText("Canvas:"+canvasAncho+"x"+canvasAlto, 20, 30, paintTextoDebug);
			canvas.drawText("RejillaAncho:"+rejilla, 20, 60, paintTextoDebug);
			if (toque) paintTextoDebug.setColor(getResources().getColor(R.color.rojo));
			canvas.drawText("Toque:"+toquePosicion_x+"x"+toquePosicion_y, 20, 90, paintTextoDebug);
			canvas.drawText("Mensaje:"+mensaje, 20, 120, paintTextoDebug);
		}

		
		// CHIVATO
		//-----------
		paintCirculo.setColor(getResources().getColor(R.color.gris_osc));
		canvas.drawCircle(chivatoConectado_x, chivatoConectado_y, chivatoConectado_radio+5, paintCirculo);
		if (mPrimoActivity.isConectado()) {
			canvas.drawText(mPrimoActivity.getNombreConectado(), chivatoConectado_x+chivatoConectado_radio, mTexto_y, paintTextoInfo);
			paintCirculo.setColor(getResources().getColor(R.color.azul));
		} else {
			if (DEBUG) canvas.drawText("NO Conectado", chivatoConectado_x+chivatoConectado_radio, mTexto_y, paintTextoInfo);
			paintCirculo.setColor(getResources().getColor(R.color.rojo_osc));
		}
		canvas.drawCircle(chivatoConectado_x, chivatoConectado_y, chivatoConectado_radio, paintCirculo);


		// Según el modo nos comportamos de forma diferente
		// Para detectar si se está toque una posición, marcamos que
		// de inicio no estamos pulsando ningún botón
		botonPulsado = false;
		if (modo == 0) { // Estamos mostrando el tablero
			//------------------------------------------------------------------------------------------------------------------------------
			// MODO TABLERO
			//------------------------------------------------------------------------------------------------------------------------------
			
			// FLECHAS Y RECUADRO
			//---------------------
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*15, rejilla*2, null);
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*25, rejilla*2, null);
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*35, rejilla*2, null);
			canvas.drawBitmap(bitmapPrimoGiro, rejilla*45, rejilla*4, null);
			canvas.drawBitmap(bitmapPrimoFlecha_i, rejilla*15, rejilla*11, null);
			canvas.drawBitmap(bitmapPrimoFlecha_i, rejilla*25, rejilla*11, null);
			canvas.drawBitmap(bitmapPrimoFlecha_i, rejilla*35, rejilla*11, null);
			canvas.drawBitmap(bitmapPrimoGiro_i, rejilla*5, rejilla*13, null);
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*15, rejilla*20, null);
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*25, rejilla*20, null);
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*35, rejilla*20, null);
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*45, rejilla*20, null);
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*15, rejilla*29, null);
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*25, rejilla*29, null);
			canvas.drawBitmap(bitmapPrimoFlecha, rejilla*35, rejilla*29, null);
			canvas.drawRoundRect(rectanguloVerde, rejilla, rejilla, paintRectVerde);

			// POSICIONES
			//------------
			// Para cada botón miramos si está pulsado y lo indicamos si procede
			if (toque) {
				// PRIMERA FILA
				if ((toquePosicion_x >= rejilla*10 && toquePosicion_x <= rejilla*14)
					&& (toquePosicion_y >= rejilla*2 && toquePosicion_y <= rejilla*6)) {
					botonPulsadoCodigo=0; botonPulsado = true;
					canvas.drawCircle(rejilla*12, rejilla*4, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*20 && toquePosicion_x <= rejilla*24)
						&& (toquePosicion_y >= rejilla*2 && toquePosicion_y <= rejilla*6)) {
						botonPulsadoCodigo=1; botonPulsado = true;
						canvas.drawCircle(rejilla*22, rejilla*4, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*30 && toquePosicion_x <= rejilla*34)
						&& (toquePosicion_y >= rejilla*2 && toquePosicion_y <= rejilla*6)) {
						botonPulsadoCodigo=2; botonPulsado = true;
						canvas.drawCircle(rejilla*32, rejilla*4, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*40 && toquePosicion_x <= rejilla*44)
						&& (toquePosicion_y >= rejilla*2 && toquePosicion_y <= rejilla*6)) {
						botonPulsadoCodigo=3; botonPulsado = true;
						canvas.drawCircle(rejilla*42, rejilla*4, circuloToque_radio, paintToque);
						
				// SEGUNDA FILA (VA AL REVÉS)
				} else if ((toquePosicion_x >= rejilla*10 && toquePosicion_x <= rejilla*14)
						&& (toquePosicion_y >= rejilla*11 && toquePosicion_y <= rejilla*15)) {
						botonPulsadoCodigo=7; botonPulsado = true;
						canvas.drawCircle(rejilla*12, rejilla*13, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*20 && toquePosicion_x <= rejilla*24)
						&& (toquePosicion_y >= rejilla*11 && toquePosicion_y <= rejilla*15)) {
						botonPulsadoCodigo=6; botonPulsado = true;
						canvas.drawCircle(rejilla*22, rejilla*13, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*30 && toquePosicion_x <= rejilla*34)
						&& (toquePosicion_y >= rejilla*11 && toquePosicion_y <= rejilla*15)) {
						botonPulsadoCodigo=5; botonPulsado = true;
						canvas.drawCircle(rejilla*32, rejilla*13, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*40 && toquePosicion_x <= rejilla*44)
						&& (toquePosicion_y >= rejilla*11 && toquePosicion_y <= rejilla*15)) {
						botonPulsadoCodigo=4; botonPulsado = true;
						canvas.drawCircle(rejilla*42, rejilla*13, circuloToque_radio, paintToque);
						
				// TERCERA FILA
				} else if ((toquePosicion_x >= rejilla*10 && toquePosicion_x <= rejilla*14)
						&& (toquePosicion_y >= rejilla*20 && toquePosicion_y <= rejilla*24)) {
						botonPulsadoCodigo=8; botonPulsado = true;
						canvas.drawCircle(rejilla*12, rejilla*22, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*20 && toquePosicion_x <= rejilla*24)
						&& (toquePosicion_y >= rejilla*20 && toquePosicion_y <= rejilla*24)) {
						botonPulsadoCodigo=9; botonPulsado = true;
						canvas.drawCircle(rejilla*22, rejilla*22, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*30 && toquePosicion_x <= rejilla*34)
						&& (toquePosicion_y >= rejilla*20 && toquePosicion_y <= rejilla*24)) {
						botonPulsadoCodigo=10; botonPulsado = true;
						canvas.drawCircle(rejilla*32, rejilla*22, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*40 && toquePosicion_x <= rejilla*44)
						&& (toquePosicion_y >= rejilla*20 && toquePosicion_y <= rejilla*24)) {
						botonPulsadoCodigo=11; botonPulsado = true;
						canvas.drawCircle(rejilla*42, rejilla*22, circuloToque_radio, paintToque);
						

				// CUARTA FILA (ACCIÓN)
				} else if ((toquePosicion_x >= rejilla*10 && toquePosicion_x <= rejilla*14)
						&& (toquePosicion_y >= rejilla*29 && toquePosicion_y <= rejilla*33)) {
						botonPulsadoCodigo=12; botonPulsado = true;
						canvas.drawCircle(rejilla*12, rejilla*31, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*20 && toquePosicion_x <= rejilla*24)
						&& (toquePosicion_y >= rejilla*29 && toquePosicion_y <= rejilla*33)) {
						botonPulsadoCodigo=13; botonPulsado = true;
						canvas.drawCircle(rejilla*22, rejilla*31, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*30 && toquePosicion_x <= rejilla*34)
						&& (toquePosicion_y >= rejilla*29 && toquePosicion_y <= rejilla*33)) {
						botonPulsadoCodigo=14; botonPulsado = true;
						canvas.drawCircle(rejilla*32, rejilla*31, circuloToque_radio, paintToque);
				} else if ((toquePosicion_x >= rejilla*40 && toquePosicion_x <= rejilla*44)
						&& (toquePosicion_y >= rejilla*29 && toquePosicion_y <= rejilla*33)) {
						botonPulsadoCodigo=15; botonPulsado = true;
						canvas.drawCircle(rejilla*42, rejilla*31, circuloToque_radio, paintToque);
						
				// BOTÓN DE EJECUCIÓN
				} else if ((toquePosicion_x >= rejilla*50 && toquePosicion_x <= rejilla*54)
						&& (toquePosicion_y >= rejilla*20 && toquePosicion_y <= rejilla*24)) {
						botonPulsadoCodigo=16; botonPulsado = true;
						canvas.drawCircle(rejilla*52, rejilla*22, circuloToque_radio, paintToque);
				}
			}
			
			// Dibujamos las posiciones
			// PRIMERA FILA
			dibujaPosicion(0, canvas, rejilla*10, rejilla*2);
			dibujaPosicion(1, canvas, rejilla*20, rejilla*2);
			dibujaPosicion(2, canvas, rejilla*30, rejilla*2);
			dibujaPosicion(3, canvas, rejilla*40, rejilla*2);
			// SEGUNDA FILA (VA AL REVÉS)
			dibujaPosicion_i(7, canvas, rejilla*10, rejilla*11);
			dibujaPosicion_i(6, canvas, rejilla*20, rejilla*11);
			dibujaPosicion_i(5, canvas, rejilla*30, rejilla*11);
			dibujaPosicion_i(4, canvas, rejilla*40, rejilla*11);
			// TERCERA FILA
			dibujaPosicion(8, canvas, rejilla*10, rejilla*20);
			dibujaPosicion(9, canvas, rejilla*20, rejilla*20);
			dibujaPosicion(10, canvas, rejilla*30, rejilla*20);
			dibujaPosicion(11, canvas, rejilla*40, rejilla*20);
			// CUARTA FILA (ACCIÓN)
			dibujaPosicion(12, canvas, rejilla*10, rejilla*29);
			dibujaPosicion(13, canvas, rejilla*20, rejilla*29);
			dibujaPosicion(14, canvas, rejilla*30, rejilla*29);
			dibujaPosicion(15, canvas, rejilla*40, rejilla*29);


			// Comprobamos si hemos soltado un botón de posición (codigo <16)
			// Es decir miramos si estaba pulsado y ya no
			if (!botonPulsado && botonPulsadoCodigo<16) {
				// Pasamos a modo de selección de pieza para la posición
				// Cambiamos de modo y forzamos que haya que dejar de  tocar antes de reconocer la señal de toque de nuevo
				modo = 1; toque = false;
			}


			// Comprobamos si hemos soltado el botón de ejecución (codigo 16)
			// Es decir miramos si estaba pulsado y ya no
			if (botonPulsadoCodigo==16 && !botonPulsado) {
				// Componemos la Acción (botones 12  a 15)
				String accion = "";
				for (int i=12;i<=15;i++) {
					switch (contenidoBotones[i]) {
						case 1: accion = accion + senPA; break;
						case 2: accion = accion + senPI; break;
						case 3: accion = accion + senPD; break;
					}
				}

				// Inicializamos el mensaje, borrando el anterior si lo hubiese
				mensaje = "";
				// Componemos el mensaje
				for (int i=0;i<=11;i++) {
					switch (contenidoBotones[i]) {
						case 1: mensaje = mensaje + senPA; break;
						case 2: mensaje = mensaje + senPI; break;
						case 3: mensaje = mensaje + senPD; break;
						case 4: mensaje = mensaje + accion; break;
					}
				}
				// Añadimos la señal de fin de mensaje
				mensaje = mensaje + senN0;
				
				// Marcamos que no hay boton pendiente
				botonPulsadoCodigo = 17;

				// Pintamos de azul el círculo de acción mientras enviamos
				paintCirculo.setColor(getResources().getColor(R.color.azul));
				// Dibujamos el botón de acción antes de hacer el envío
				canvas.drawCircle(rejilla*52, rejilla*22, rejilla*2, paintCirculo);

				// Enviamos el mensaje  
				mPrimoActivity.enviaCadena(mensaje);

				// El envío es muy rápido y el robot no lo será tanto. Por eso ponemos un modo de notificación de envío
				// Cambiamos de modo y forzamos que haya que dejar de  tocar antes de reconocer la señal de toque de nuevo
				modo = 2; toque = false;
				
			} else { // No acabamos de soltar el botón
				// Dibujamos el botón de acción
				paintCirculo.setColor(getResources().getColor(R.color.rojo));
				canvas.drawCircle(rejilla*52, rejilla*22, rejilla*2, paintCirculo);
			}

		} else if (modo == 1) { // Estamos seleccionando una pieza
			//------------------------------------------------------------------------------------------------------------------------------
			// MODO SELECCION DE PIEZA
			//------------------------------------------------------------------------------------------------------------------------------
			// Dibujamos un rectangulo en pantalla
			//canvas.drawRoundRect(rectanguloSeleccion, rejilla, rejilla, paintRectSeleccion);

			// Cargamos el mensaje
			paintTextoInfo.setTextAlign(Paint.Align.LEFT);
			canvas.drawText(getResources().getString(R.string.seleccion_pieza), rejilla*5, rejilla*6, paintTextoInfo);
			paintTextoInfo.setTextAlign(Paint.Align.RIGHT);

			// Dibujamos las piezas a seleccionar
			canvas.drawBitmap(bitmapPrimoPosicion, rejilla*10, rejilla*17, null);
			canvas.drawBitmap(bitmapPrimoDelante, rejilla*18, rejilla*17, null);
			canvas.drawBitmap(bitmapPrimoIzquierda, rejilla*26, rejilla*17, null);
			canvas.drawBitmap(bitmapPrimoDerecha, rejilla*34, rejilla*17, null);
			// El botón de acción solo se puede seleccionar en las primeras 12 posiciones
			if (botonPulsadoCodigo<12) canvas.drawBitmap(bitmapPrimoAccion, rejilla*42, rejilla*17, null);

			// Gestionamos el toque sobre una de las piezas
			if (toque && toquePosicion_y >= rejilla*17 && toquePosicion_y <= rejilla*22) {
				if (toquePosicion_x >= rejilla*10 && toquePosicion_x <= rejilla*14) {
					// Indicamos el nuevo contenido del botón (posición)
					contenidoBotones[botonPulsadoCodigo] = 0;
					// Borramos el código de botón pulsado
					botonPulsadoCodigo = 17;
					// Cambiamos de modo y forzamos que haya que dejar de  tocar antes de reconocer la señal de toque de nuevo
					modo = 0; toque = false;
				} else if (toquePosicion_x >= rejilla*18 && toquePosicion_x <= rejilla*22) {
					// Indicamos el nuevo contenido del botón (posición)
					contenidoBotones[botonPulsadoCodigo] = 1;
					// Borramos el código de botón pulsado
					botonPulsadoCodigo = 17;
					// Cambiamos de modo y forzamos que haya que dejar de  tocar antes de reconocer la señal de toque de nuevo
					modo = 0; toque = false;
				} else if (toquePosicion_x >= rejilla*26 && toquePosicion_x <= rejilla*30) {
					// Indicamos el nuevo contenido del botón (posición)
					contenidoBotones[botonPulsadoCodigo] = 2;
					// Borramos el código de botón pulsado
					botonPulsadoCodigo = 17;
					// Cambiamos de modo y forzamos que haya que dejar de  tocar antes de reconocer la señal de toque de nuevo
					modo = 0; toque = false;
				} else if (toquePosicion_x >= rejilla*34 && toquePosicion_x <= rejilla*38) {
					// Indicamos el nuevo contenido del botón (posición)
					contenidoBotones[botonPulsadoCodigo] = 3;
					// Borramos el código de botón pulsado
					botonPulsadoCodigo = 17;
					// Cambiamos de modo y forzamos que haya que dejar de  tocar antes de reconocer la señal de toque de nuevo
					modo = 0; toque = false;
				} else if (toquePosicion_x >= rejilla*42 && toquePosicion_x <= rejilla*46 && botonPulsadoCodigo<12) {
					// Indicamos el nuevo contenido del botón (posición)
					contenidoBotones[botonPulsadoCodigo] = 4;
					// Borramos el código de botón pulsado
					botonPulsadoCodigo = 17;
					// Cambiamos de modo y forzamos que haya que dejar de  tocar antes de reconocer la señal de toque de nuevo
					modo = 0; toque = false;
				}
			}


		} else if (modo == 2){ // Estamos mostrando la pantalla de envío de mensaje
			//------------------------------------------------------------------------------------------------------------------------------
			// MODO ENVÍO DE MENSAJE
			//------------------------------------------------------------------------------------------------------------------------------
			// Dibujamos un rectangulo en pantalla
			//canvas.drawRoundRect(rectanguloSeleccion, rejilla, rejilla, paintRectSeleccion);

			// Cargamos el mensaje
			paintTextoInfo.setTextAlign(Paint.Align.LEFT);
			canvas.drawText(getResources().getString(R.string.enviando_mensaje), rejilla*5, rejilla*6, paintTextoInfo);
			canvas.drawText(getResources().getString(R.string.toque_para_continuar), rejilla*5, rejilla*8, paintTextoInfo);
			paintTextoInfo.setTextAlign(Paint.Align.RIGHT);
			
			// Dibujamos el logo de la aplicación
			canvas.drawBitmap(bitmapLogo, (canvasAncho/2)-(bitmapLogo.getWidth()/2), (canvasAlto/2)-(bitmapLogo.getHeight()/2), null);

			// Al tocar la pantalla salimos del modo
			if (toque) {
				// Cambiamos de modo y forzamos que haya que dejar de  tocar antes de reconocer la señal de toque de nuevo
				modo = 0; toque = false;
			}

		}

		
	}

	
	//*******************************************************
    //    FUNCIONES AUXILIARES
	//*******************************************************
    // Establecemos parámetros de dibujo
	private void definePaints() {
		if (DEBUG) Log.d(TAG, "+++ definePaints +++");

		// Caracteristicas Paint de los círculos y circunferencias
		paintCirculo.setStyle(Paint.Style.FILL);
		
		paintToque.setStyle(Paint.Style.STROKE);
		paintToque.setColor(getResources().getColor(R.color.rojo_osc));
		
		paintRectVerde.setStyle(Paint.Style.STROKE);
		paintRectVerde.setColor(getResources().getColor(R.color.verde));

		paintRectSeleccion.setStyle(Paint.Style.FILL);
		paintRectSeleccion.setColor(getResources().getColor(R.color.marron_cla));

		paintTextoInfo.setTextAlign(Paint.Align.RIGHT);
		paintTextoInfo.setFakeBoldText(true);
		paintTextoInfo.setColor(getResources().getColor(R.color.negro));

		paintEnv.setStyle(Paint.Style.FILL);
		paintEnv.setColor(getResources().getColor(R.color.verde_cla));

		paintTextoDebug.setTextAlign(Paint.Align.LEFT);

	}

	// Esta función dibuja una posición en base a su contenido 
	private void dibujaPosicion(int i, Canvas canvas, int x, int y) {
		// Para cada posición comprobamos su valor y realizamos el dibujo adecuado
		switch(contenidoBotones[i]) {
		case 0:
			canvas.drawBitmap(bitmapPrimoPosicion, x, y, null);
			break;
		case 1:
			canvas.drawBitmap(bitmapPrimoDelante, x, y, null);
			break;
		case 2:
			canvas.drawBitmap(bitmapPrimoIzquierda, x, y, null);
			break;
		case 3:
			canvas.drawBitmap(bitmapPrimoDerecha, x, y, null);
			break;
		case 4:
			canvas.drawBitmap(bitmapPrimoAccion, x, y, null);
			break;
				
		}
	}
	
	// Esta función dibuja una posición en base a su contenido (invertida)
	private void dibujaPosicion_i(int i, Canvas canvas, int x, int y) {
		// Para cada posición comprobamos su valor y realizamos el dibujo adecuado
		switch(contenidoBotones[i]) {
		case 0:
			canvas.drawBitmap(bitmapPrimoPosicion_i, x, y, null);
			break;
		case 1:
			canvas.drawBitmap(bitmapPrimoDelante_i, x, y, null);
			break;
		case 2:
			canvas.drawBitmap(bitmapPrimoIzquierda_i, x, y, null);
			break;
		case 3:
			canvas.drawBitmap(bitmapPrimoDerecha_i, x, y, null);
			break;
		case 4:
			canvas.drawBitmap(bitmapPrimoAccion, x, y, null);
			break;
				
		}
	}

    //****************************************************************************
	//    GESTIÓN DE PEREFERENCIAS
    //****************************************************************************
    public void leePrefs() {
		if (DEBUG) Log.d(TAG, "+++ leePrefs +++");

		mPrefs = PreferenceManager.getDefaultSharedPreferences(mPrimoActivity);
		
    	senN0 = mPrefs.getString("N0", "0");
    	
    	senPA = mPrefs.getString("PA", "V");
    	senPI = mPrefs.getString("PI", "W");
    	senPD = mPrefs.getString("PD", "Y");

    }
	
}
