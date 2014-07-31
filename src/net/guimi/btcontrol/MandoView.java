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

public class MandoView extends SurfaceView implements SurfaceHolder.Callback {
    public static final String TAG = "MandoView";   
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
    private MandoActivity mMandoActivity;
    // Hilo que gestiona el ciclo de interacción    
    private MandoThread mMandoThread;

    // Preferencias y señales
    //----------------------------
    private SharedPreferences mPrefs;
    private String senN1, senN2, senN3, senN4, senN5, senN6, senN7, senN8, senN9;
    private String senFA, senFB, senFC, senFD, senFE, senFF, senFG, senFH, senFI, senFJ;
    private String senPA, senPB, senPI, senPD, senPS;
    
    // Estados
    //--------------------------------------
	// Variables que indican las coordenadas en donde se ha tocado la pantalla por última vez
	public int toquePosicion_x, toquePosicion_y;
	// Variable que indica si hay que gestionar un toque de pantalla
	public boolean toque;
	// Variable que indica si estamos enviando un número (control circular)
	public boolean enviandoNumero;
    // Variable que recoge si en cada momento hay un botón pulsado
	private boolean botonPulsado = false;
	// Variables que indican si un botón está pulsado
	private boolean botonPrimoAPulsado = false;
	private boolean botonPrimoBPulsado = false;
	private boolean botonPrimoIPulsado = false;
	private boolean botonPrimoDPulsado = false;
	private boolean botonPrimoSPulsado = false;
	private boolean botonAPulsado = false;
	private boolean botonBPulsado = false;
	private boolean botonCPulsado = false;
	private boolean botonDPulsado = false;
	private boolean botonEPulsado = false;
	private boolean botonFPulsado = false;
	private boolean botonGPulsado = false;
	private boolean botonHPulsado = false;
	private boolean botonIPulsado = false;
	private boolean botonJPulsado = false;

	
    // Dibujo
    //--------------------------------------
	// Elementos de dibujo
	private Paint paintCircunferencia = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintCirculo = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintTextoBotones = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintTextoInfo = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintEnv = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintTextoDebug = new Paint();
    private RectF mRectF = new RectF();
	private Bitmap bitmapFondo = null; 
	private Bitmap bitmapFirma = null; 
	private Bitmap bitmapBoton = null; 
	private Bitmap bitmapBotonPulsado = null; 
	private Bitmap bitmapPrimoArriba = null; 
	private Bitmap bitmapPrimoAbajo = null; 
	private Bitmap bitmapPrimoIzquierda = null; 
	private Bitmap bitmapPrimoDerecha = null; 
	private Bitmap bitmapPrimoStop = null; 
	private Bitmap bitmapPrimoArribaPulsado = null; 
	private Bitmap bitmapPrimoAbajoPulsado = null; 
	private Bitmap bitmapPrimoIzquierdaPulsado = null; 
	private Bitmap bitmapPrimoDerechaPulsado = null; 
	private Bitmap bitmapPrimoStopPulsado = null; 
	// Variables para describir los elementos a dibujar
	private int canvasAncho, canvasAlto, rejilla;
	private int bitmapBotonAncho, bitmapBotonAlto;
	private int botonA_x, botonB_x, botonC_x, botonD_x, botonE_x, botonesFila1_y;
	private int botonF_x, botonG_x, botonH_x, botonI_x, botonJ_x, botonesFila2_y;
	private int circulo0_x, circulo0_y, circulo0_radio;
	private int circulo1_x, circulo1_y, circulo11_radio, circulo12_radio;
	private int circuloToque_radio;
	private int chivatoConectado_radio, chivatoConectado_x, chivatoConectado_y;
	private int primo_arriba_x, primo_arriba_y, primo_abajo_x, primo_abajo_y, primo_derecha_x, primo_derecha_y, primo_izquierda_x, primo_izquierda_y, primo_stop_x, primo_stop_y;
	private int divisiones1_x1, divisiones1_x2, divisiones1_y1, divisiones1_y2;
    private float arcoAnguloInicial;
    private float arcoGrados = 45;
    private int mTexto_x, mTexto_y;
    
    
    // Función de creación de la vista
	public MandoView(MandoActivity mandoActivity) {
		super(mandoActivity);
		
		if (DEBUG) Log.d(TAG, "+++ ON CREATE +++");

		// Obtenemos mi actividad
		mMandoActivity = mandoActivity;

        // Añadimos esta pantalla (vista) a la pila SurfaceHolder
        getHolder().addCallback(this);
        
        // Establecemos parámetros de dibujo
        definePaints();
		
        // Leemos las preferencias y tomamos las señales
        leePrefs();
        
        // Generamos nuestro hilo de juego
		mMandoThread = new MandoThread(this);
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
        if (!mMandoThread.isAlive()) {
        	// Si no, lo creamos
        	mMandoThread = new MandoThread(this);
        }
        
       // Ponemos el hilo en marcha
		mMandoThread.setEnMarcha(true);
		mMandoThread.start();

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		if (DEBUG) Log.d(TAG, "+++ surfaceDestroyed +++");

    	// Generamos una variable auxiliar que nos indica si debemos reintentar matar el proceso
        boolean reintentar = true;

        // Le decimos al hilo que termine y espere a acabar,
    	//   si no lo hiciésemos, el usuario podría tocar la superficie después de que volvamos
        mMandoThread.setEnMarcha(false);
        while (reintentar) {
            try {
            	mMandoThread.join();
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

		// Escalamos las Imágenes
		//-----------------------------
		// Imagen de fondo y firma
		Bitmap unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.light_wood_texture);
		bitmapFondo = Bitmap.createScaledBitmap(unscaledBitmap, canvasAncho, canvasAlto, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.guimi_firma);
		bitmapFirma = Bitmap.createScaledBitmap(unscaledBitmap, ((unscaledBitmap.getWidth()*(rejilla*3/2))/unscaledBitmap.getHeight()), rejilla*3/2, true);
		// Imágenes de los botones
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mando_boton);
		bitmapBoton = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*6, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mando_boton_pulsado);
		bitmapBotonPulsado = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*6, rejilla*4, true);
		// Imágenes de los botones de primo
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_recto_a);
		bitmapPrimoArriba = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_recto_b);
		bitmapPrimoAbajo = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_izquierda_a);
		bitmapPrimoIzquierda = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_derecha_a);
		bitmapPrimoDerecha = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_stop);
		bitmapPrimoStop = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_recto_a_pulsado);
		bitmapPrimoArribaPulsado = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_recto_b_pulsado);
		bitmapPrimoAbajoPulsado = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_izquierda_a_pulsado);
		bitmapPrimoIzquierdaPulsado = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_derecha_a_pulsado);
		bitmapPrimoDerechaPulsado = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);
		unscaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.primo_stop_pulsado);
		bitmapPrimoStopPulsado = Bitmap.createScaledBitmap(unscaledBitmap, rejilla*4, rejilla*4, true);

		
		// CIRCULOS 1.x
		//--------------
		// Dimensiones del circulo 1 exterior (circulo 1.1)
		// recordemos que la pantalla está apaisada
		circulo11_radio = (int) (canvasAlto*0.25);
		circulo1_x = canvasAncho - circulo11_radio - circulo11_radio*1/5 -25;
		circulo1_y = circulo11_radio + circulo11_radio*1/5 + 15;
		paintCircunferencia.setStrokeWidth(circulo11_radio*2/5);
		
		// Dimensiones del circulo 1.2
		circulo12_radio = circulo11_radio/2;
		
		// Dimensiones del circulo interior
		circuloToque_radio = circulo11_radio*2/5;

		// Dividimos el circulo 1 exterior en una cuadrícula de 3x3 secciones
		divisiones1_x1 = circulo1_x - (circulo11_radio/3);
		divisiones1_x2 = circulo1_x + (circulo11_radio/3);
		divisiones1_y1 = circulo1_y - (circulo11_radio/3);
		divisiones1_y2 = circulo1_y + (circulo11_radio/3);
		
		// Rectángulo en el que se inscribe el circulo 1.1, usado para dibujar los arcos
        mRectF = new RectF(circulo1_x - circulo11_radio, circulo1_y - circulo11_radio, circulo1_x + circulo11_radio, circulo1_y + circulo11_radio);

		// CIRCULO 0 (PRIMO)
		//-------------------
		// Dimensiones de las imagenes
		circulo0_radio = circulo11_radio;
		circulo0_x = circulo0_radio + circulo0_radio*1/5 +25;
		circulo0_y = circulo1_y;

		// Dividimos el circulo 0 en una cuadrícula de 3x3 secciones
		primo_arriba_x = circulo0_x - (rejilla*2);
		primo_arriba_y = circulo0_y - circulo0_radio;
		primo_abajo_x = circulo0_x - (rejilla*2);
		primo_abajo_y = circulo0_y + circulo0_radio - rejilla*4;
		primo_izquierda_x = circulo0_x - circulo0_radio;
		primo_izquierda_y = circulo0_y - (rejilla*2);
		primo_derecha_x = circulo0_x + circulo0_radio - rejilla*4;
		primo_derecha_y = circulo0_y - (rejilla*2);
		primo_stop_x = circulo0_x - (rejilla*2);
		primo_stop_y = circulo0_y - (rejilla*2);

		// CHIVATOS
		//--------------
        chivatoConectado_x = canvasAncho/2;
        chivatoConectado_y = circulo1_y;
        chivatoConectado_radio = canvasAlto/30;

        // BOTONES
        //---------
		// Dimensiones de la imagen Botón
		bitmapBotonAncho = bitmapBoton.getWidth();
		bitmapBotonAlto = bitmapBoton.getHeight();
		botonesFila1_y = canvasAlto - (bitmapBotonAlto*2) - 60;
		botonesFila2_y = canvasAlto - bitmapBotonAlto - 30;
		botonA_x = canvasAncho/7; botonF_x = canvasAncho/7;
		botonB_x = canvasAncho*2/7; botonG_x = canvasAncho*2/7;
		botonC_x = canvasAncho*3/7; botonH_x = canvasAncho*3/7;
		botonD_x = canvasAncho*4/7; botonI_x = canvasAncho*4/7;
		botonE_x = canvasAncho*5/7; botonJ_x = canvasAncho*5/7;

		
		// TEXTO
		//-------
		// Ajustamos el tamaño de letra al alto que nos permiten los botones
	    int size = 0;       
		Rect bounds = new Rect();
		int alturaTexto = 0;
		float alturaObjetivo = (float)bitmapBotonAlto*0.5f;
    	paintTextoBotones.setTextSize(size);
	    do {
	    	paintTextoBotones.setTextSize(++ size);
			paintTextoBotones.getTextBounds("A", 0, "A".length(), bounds);
			alturaTexto = bounds.bottom - bounds.top;
			mTexto_y=bounds.bottom+((bitmapBotonAlto-(bounds.bottom-bounds.top))/2);
			mTexto_x=(bitmapBotonAncho/2);
	    } while(alturaTexto < alturaObjetivo);
		/*
		//Para ajustar a ancho lo haríamos así
	    int size = 0;       
    	paintTextoBotones.setTextSize(size);
	    do {
	    	paintTextoBotones.setTextSize(++ size);
	    } while(paintTextoBotones.measureText("A") < bitmapBotonAncho/2);
	    */
	    
	    paintTextoInfo.setTextSize(size/2);	    
		paintTextoDebug.setTextSize(size/2);

	}
	
	
	//*******************************************************
    //    LÓGICA DE PROGRAMA
	//*******************************************************
	public void gestionaPantalla (Canvas canvas) {		
		// FONDO Y FIRMA
		//------------------
		canvas.drawBitmap(bitmapFondo, 0, 0, null);
		canvas.drawBitmap(bitmapFirma, canvasAncho - bitmapFirma.getWidth() - rejilla, canvasAlto - bitmapFirma.getHeight() - rejilla, null);

		
		// CÍRCULO 0
		//------------
		// Dibujamos el círculo 0
		paintCircunferencia.setColor(getResources().getColor(R.color.gris));
		canvas.drawCircle(circulo0_x, circulo0_y, circulo0_radio, paintCircunferencia);

		
		// CHIVATOS
		//-----------
		paintCirculo.setColor(getResources().getColor(R.color.gris_osc));
		canvas.drawCircle(chivatoConectado_x, chivatoConectado_y, chivatoConectado_radio+5, paintCirculo);
		if (mMandoActivity.isConectado()) {
			paintCirculo.setColor(getResources().getColor(R.color.azul));
			canvas.drawText(mMandoActivity.getNombreConectado(), chivatoConectado_x, chivatoConectado_y - chivatoConectado_radio-25, paintTextoInfo);
		} else {
			paintCirculo.setColor(getResources().getColor(R.color.rojo_osc));
		}
		canvas.drawCircle(chivatoConectado_x, chivatoConectado_y, chivatoConectado_radio, paintCirculo);

		paintCirculo.setColor(getResources().getColor(R.color.gris_osc));
		canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio+5, paintCirculo);
		paintCirculo.setColor(getResources().getColor(R.color.gris));
		canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintCirculo);

		
		// CÍRCULOS 1.x
		//---------------
		// Dibujamos los círculos 1.x
		// Circulo exterior
		paintCircunferencia.setColor(getResources().getColor(R.color.gris));
		canvas.drawCircle(circulo1_x, circulo1_y, circulo11_radio, paintCircunferencia);
		// Arcos
		paintCircunferencia.setColor(getResources().getColor(R.color.gris_osc));
		arcoAnguloInicial = (float) -157.5;
        // El cuarto parametro de drawArc indica si dibujar solo el arco o también el "quesito"
        canvas.drawArc(mRectF, arcoAnguloInicial, arcoGrados, false, paintCircunferencia);
		paintCirculo.setColor(getResources().getColor(R.color.gris_osc));
		arcoAnguloInicial = (float) -67.5;
        canvas.drawArc(mRectF, arcoAnguloInicial, arcoGrados, false, paintCircunferencia);
		arcoAnguloInicial = (float) 22.5;
        canvas.drawArc(mRectF, arcoAnguloInicial, arcoGrados, false, paintCircunferencia);
		arcoAnguloInicial = (float) 112.5;
        canvas.drawArc(mRectF, arcoAnguloInicial, arcoGrados, false, paintCircunferencia);
        // Circulos interiores
		paintCirculo.setColor(getResources().getColor(R.color.marron));
		canvas.drawCircle(circulo1_x, circulo1_y, circulo12_radio, paintCirculo);
		
		
		// Dibujamos el círculo de toque
		// Si tocamos dentro del circulo exterior, el centro es el punto de toque
		// si no, el centro es el centro del círculo exterior
		// Para calcular si estamos dentro del círculo calculamos la distancia (el radio) desde el centro al toque,
		// y si es menor que el radio del círculo estamos dentro
		// http://en.wikipedia.org/wiki/Circle#Equations
		if (toque && 
				(Math.hypot(circulo1_x-toquePosicion_x, circulo1_y-toquePosicion_y)<circulo11_radio)){
			
			paintCirculo.setColor(getResources().getColor(R.color.marron_cla));

			// Dividimos el circulo en 9 secciones y calculamos en cual de ellas estamos
			// Calculamos en qué parte de la cuadrícula estamos
			// Recordemos que estamos con la pantalla en apaisado, por eso fila y columna se intercambian
			if (toquePosicion_x < divisiones1_x1) { // Primera fila
				if (toquePosicion_y < divisiones1_y1) { // Primera columna
					canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
					enviandoNumero = true;
					mMandoActivity.enviaCadena(senN1);
				} else if (toquePosicion_y < divisiones1_y2) { // Segunda columna
					canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
					enviandoNumero = true;
					mMandoActivity.enviaCadena(senN4);
				} else { // Tercera columna
					canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
					enviandoNumero = true;
					mMandoActivity.enviaCadena(senN7);
				}
			} else if (toquePosicion_x < divisiones1_x2) { // Segunda fila
				if (toquePosicion_y < divisiones1_y1) { // Primera columna
					canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
					enviandoNumero = true;
					mMandoActivity.enviaCadena(senN2);
				} else if (toquePosicion_y < divisiones1_y2) { // Segunda columna
					canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
					enviandoNumero = true;
					//paintCirculo.setColor(getResources().getColor(R.color.marroncla));
					mMandoActivity.enviaCadena(senN5);
				} else { // Tercera columna
					canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
					enviandoNumero = true;
					mMandoActivity.enviaCadena(senN8);
				}
			} else { // Tercera fila
				if (toquePosicion_y < divisiones1_y1) { // Primera columna
					canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
					enviandoNumero = true;
					mMandoActivity.enviaCadena(senN3);
				} else if (toquePosicion_y < divisiones1_y2) { // Segunda columna
					canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
					enviandoNumero = true;
					mMandoActivity.enviaCadena(senN6);
				} else { // Tercera columna
					canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
					enviandoNumero = true;
					mMandoActivity.enviaCadena(senN9);
				}
			}

			canvas.drawCircle(toquePosicion_x, toquePosicion_y, circuloToque_radio, paintCirculo);

			
		} else {
			// Si estábamos enviando un número y dejamos de hacerlo,
			// enviamos la señal 5
			if (enviandoNumero) {
				mMandoActivity.enviaCadena(senN5);
				enviandoNumero = false;
			}
			paintCirculo.setColor(getResources().getColor(R.color.marron_cla));
			canvas.drawCircle(circulo1_x, circulo1_y, circuloToque_radio, paintCirculo);
		}


		// BOTONES
		//------------
		// Si estamos toque dentro de un botón lo indicamos
		botonPulsado = false;
		// Para cada botón miramos si está pulsado
		if (toque) {
			if ((toquePosicion_x > primo_arriba_x && toquePosicion_x < primo_arriba_x + bitmapBotonAncho)
				&&
				(toquePosicion_y > primo_arriba_y && toquePosicion_y < primo_arriba_y + bitmapBotonAlto)) {
				botonPrimoAPulsado = true; botonPulsado = true;				
			} else if ((toquePosicion_x > primo_abajo_x && toquePosicion_x < primo_abajo_x + bitmapBotonAncho)
					&&
					(toquePosicion_y > primo_abajo_y && toquePosicion_y < primo_abajo_y + bitmapBotonAlto)) {
				botonPrimoBPulsado = true; botonPulsado = true;
			} else if ((toquePosicion_x > primo_izquierda_x && toquePosicion_x < primo_izquierda_x + bitmapBotonAncho)
					&&
					(toquePosicion_y > primo_izquierda_y && toquePosicion_y < primo_izquierda_y + bitmapBotonAlto)) {
				botonPrimoIPulsado = true; botonPulsado = true;				
			} else if ((toquePosicion_x > primo_derecha_x && toquePosicion_x < primo_derecha_x + bitmapBotonAncho)
					&&
					(toquePosicion_y > primo_derecha_y && toquePosicion_y < primo_derecha_y + bitmapBotonAlto)) {
				botonPrimoDPulsado = true; botonPulsado = true;				
			} else if ((toquePosicion_x > primo_stop_x && toquePosicion_x < primo_stop_x + bitmapBotonAncho)
					&&
					(toquePosicion_y > primo_stop_y && toquePosicion_y < primo_stop_y + bitmapBotonAlto)) {
				botonPrimoSPulsado = true; botonPulsado = true;
			} else if (toquePosicion_y > botonesFila1_y && toquePosicion_y < botonesFila1_y + bitmapBotonAlto) { // primera fila
				if (toquePosicion_x < botonA_x + bitmapBotonAncho) {
					botonAPulsado = true; botonPulsado = true;
				} else if (toquePosicion_x > botonB_x && toquePosicion_x < botonB_x + bitmapBotonAncho) {
					botonBPulsado = true; botonPulsado = true;
				} else if (toquePosicion_x > botonC_x && toquePosicion_x < botonC_x + bitmapBotonAncho) {
					botonCPulsado = true; botonPulsado = true;
				} else if (toquePosicion_x > botonD_x && toquePosicion_x < botonD_x + bitmapBotonAncho) {
					botonDPulsado = true; botonPulsado = true;
				} else if (toquePosicion_x > botonE_x && toquePosicion_x < botonE_x + bitmapBotonAncho) {
					botonEPulsado = true; botonPulsado = true;
				}
			} else if (toquePosicion_y > botonesFila2_y && toquePosicion_y < botonesFila2_y + bitmapBotonAlto) { // segunda fila
				if (toquePosicion_x < botonF_x + bitmapBotonAncho) {
					botonFPulsado = true; botonPulsado = true;
				} else if (toquePosicion_x > botonG_x && toquePosicion_x < botonG_x + bitmapBotonAncho) {
					botonGPulsado = true; botonPulsado = true;
				} else if (toquePosicion_x > botonH_x && toquePosicion_x < botonH_x + bitmapBotonAncho) {
					botonHPulsado = true; botonPulsado = true;
				} else if (toquePosicion_x > botonI_x && toquePosicion_x < botonI_x + bitmapBotonAncho) {
					botonIPulsado = true; botonPulsado = true;
				} else if (toquePosicion_x > botonJ_x && toquePosicion_x < botonJ_x + bitmapBotonAncho) {
					botonJPulsado = true; botonPulsado = true;
				}
			}
		}


		// Para cada botón comprobamos si se estaba pulsando y si se sigue pulsando
		// Y en base a ello se dibuja un botón u otro y se envía la señal si procede
		if (botonPrimoAPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapPrimoArribaPulsado, primo_arriba_x, primo_arriba_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapPrimoArriba, primo_arriba_x, primo_arriba_y, null);
				// Enviamos la señal correspondiente
				botonPrimoAPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senPA);
			}
		} else {
			canvas.drawBitmap(bitmapPrimoArriba, primo_arriba_x, primo_arriba_y, null);
		}
		
		if (botonPrimoBPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapPrimoAbajoPulsado, primo_abajo_x, primo_abajo_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapPrimoAbajo, primo_abajo_x, primo_abajo_y, null);
				// Enviamos la señal correspondiente
				botonPrimoBPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senPB);
			}
		} else {
			canvas.drawBitmap(bitmapPrimoAbajo, primo_abajo_x, primo_abajo_y, null);
		}
		
		if (botonPrimoIPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapPrimoIzquierdaPulsado, primo_izquierda_x, primo_izquierda_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapPrimoIzquierda, primo_izquierda_x, primo_izquierda_y, null);
				// Enviamos la señal correspondiente
				botonPrimoIPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senPI);
			}
		} else {
			canvas.drawBitmap(bitmapPrimoIzquierda, primo_izquierda_x, primo_izquierda_y, null);
		}
		
		if (botonPrimoDPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapPrimoDerechaPulsado, primo_derecha_x, primo_derecha_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapPrimoDerecha, primo_derecha_x, primo_derecha_y, null);
				// Enviamos la señal correspondiente
				botonPrimoDPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senPD);
			}
		} else {
			canvas.drawBitmap(bitmapPrimoDerecha, primo_derecha_x, primo_derecha_y, null);
		}
		
		if (botonPrimoSPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapPrimoStopPulsado, primo_stop_x, primo_stop_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapPrimoStop, primo_stop_x, primo_stop_y, null);
				// Enviamos la señal correspondiente
				botonPrimoSPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senPS);
			}
		} else {
			canvas.drawBitmap(bitmapPrimoStop, primo_stop_x, primo_stop_y, null);
		}
		
		if (botonAPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonA_x, botonesFila1_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonA_x, botonesFila1_y, null);
				// Enviamos la señal correspondiente
				botonAPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFA);				
			}
		} else {
			canvas.drawBitmap(bitmapBoton, botonA_x, botonesFila1_y, null);
		}

		if (botonBPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonB_x, botonesFila1_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonB_x, botonesFila1_y, null);
				// Enviamos la señal correspondiente
				botonBPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFB);
			}
		} else { canvas.drawBitmap(bitmapBoton, botonB_x, botonesFila1_y, null); }

		if (botonCPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonC_x, botonesFila1_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonC_x, botonesFila1_y, null);
				// Enviamos la señal correspondiente
				botonCPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFC);				
			}
		} else { canvas.drawBitmap(bitmapBoton, botonC_x, botonesFila1_y, null); }

		if (botonDPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonD_x, botonesFila1_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonD_x, botonesFila1_y, null);
				// Enviamos la señal correspondiente
				botonDPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFD);				
			}
		} else { canvas.drawBitmap(bitmapBoton, botonD_x, botonesFila1_y, null); }

		if (botonEPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonE_x, botonesFila1_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonE_x, botonesFila1_y, null);
				// Enviamos la señal correspondiente
				botonEPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFE);				
			}
		} else { canvas.drawBitmap(bitmapBoton, botonE_x, botonesFila1_y, null); }

		if (botonFPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonF_x, botonesFila2_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonF_x, botonesFila2_y, null);
				// Enviamos la señal correspondiente
				botonFPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFF);				
			}
		} else { canvas.drawBitmap(bitmapBoton, botonF_x, botonesFila2_y, null); }

		if (botonGPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonG_x, botonesFila2_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonG_x, botonesFila2_y, null);
				// Enviamos la señal correspondiente
				botonGPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFG);				
			}
		} else { canvas.drawBitmap(bitmapBoton, botonG_x, botonesFila2_y, null); }

		if (botonHPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonH_x, botonesFila2_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonH_x, botonesFila2_y, null);
				// Enviamos la señal correspondiente
				botonHPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFH);				
			}
		} else { canvas.drawBitmap(bitmapBoton, botonH_x, botonesFila2_y, null); }

		if (botonIPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonI_x, botonesFila2_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonI_x, botonesFila2_y, null);
				// Enviamos la señal correspondiente
				botonIPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFI);				
			}
		} else { canvas.drawBitmap(bitmapBoton, botonI_x, botonesFila2_y, null); }

		if (botonJPulsado) {
			if (botonPulsado) { // El botón está pulsado
				canvas.drawBitmap(bitmapBotonPulsado, botonJ_x, botonesFila2_y, null);
			} else { // El botón estaba pulsado pero ya no lo está
				canvas.drawBitmap(bitmapBoton, botonJ_x, botonesFila2_y, null);
				// Enviamos la señal correspondiente
				botonJPulsado = false;
				canvas.drawCircle(chivatoConectado_x, chivatoConectado_y+(chivatoConectado_radio*2)+30, chivatoConectado_radio, paintEnv);
				mMandoActivity.enviaCadena(senFJ);				
			}
		} else { canvas.drawBitmap(bitmapBoton, botonJ_x, botonesFila2_y, null); }


		canvas.drawText("A", botonA_x+mTexto_x, botonesFila1_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);
		canvas.drawText("B", botonB_x+mTexto_x, botonesFila1_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);
		canvas.drawText("C", botonC_x+mTexto_x, botonesFila1_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);
		canvas.drawText("D", botonD_x+mTexto_x, botonesFila1_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);
		canvas.drawText("E", botonE_x+mTexto_x, botonesFila1_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);
		canvas.drawText("F", botonF_x+mTexto_x, botonesFila2_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);
		canvas.drawText("G", botonG_x+mTexto_x, botonesFila2_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);
		canvas.drawText("H", botonH_x+mTexto_x, botonesFila2_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);
		canvas.drawText("I", botonI_x+mTexto_x, botonesFila2_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);
		canvas.drawText("J", botonJ_x+mTexto_x, botonesFila2_y+bitmapBotonAlto-mTexto_y, paintTextoBotones);

		
		// DEBUG
		//---------
		if (DEBUG) {
			paintTextoDebug.setColor(getResources().getColor(R.color.negro));
			canvas.drawText("Canvas:"+canvasAncho+"x"+canvasAlto, 20, 30, paintTextoDebug);
			canvas.drawText("Diametro CirculoExterior:"+circulo11_radio*2, 20, 60, paintTextoDebug);
			//canvas.drawText("Divisiones (x....,y....):"+divisiones1_x0+","+divisiones1_x1+","+divisiones1_x2+","+divisiones1_x3+"x"+divisiones1_y0+","+divisiones1_y1+","+divisiones1_y2+","+divisiones1_y3, 20, 150, paintTextoDebug);
			if (toque) paintTextoDebug.setColor(getResources().getColor(R.color.rojo));
			canvas.drawText("Toque:"+toquePosicion_x+"x"+toquePosicion_y, 20, 90, paintTextoDebug);
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
		paintCircunferencia.setStyle(Paint.Style.STROKE);

		// Caracteristicas Paint del texto
		paintTextoBotones.setColor(getResources().getColor(R.color.negro));
		paintTextoBotones.setTextAlign(Paint.Align.CENTER);
		paintTextoBotones.setFakeBoldText(true);
		
		paintTextoInfo.setTextAlign(Paint.Align.CENTER);
		paintTextoInfo.setFakeBoldText(true);
		paintTextoInfo.setColor(getResources().getColor(R.color.negro));

		paintEnv.setStyle(Paint.Style.FILL);
		paintEnv.setColor(getResources().getColor(R.color.verde_cla));

		paintTextoDebug.setTextAlign(Paint.Align.LEFT);

	}


    //****************************************************************************
	//    GESTIÓN DE PEREFERENCIAS
    //****************************************************************************
    public void leePrefs() {
		if (DEBUG) Log.d(TAG, "+++ leePrefs +++");

		mPrefs = PreferenceManager.getDefaultSharedPreferences(mMandoActivity);
		
    	senN1 = mPrefs.getString("N1", "1");
    	senN2 = mPrefs.getString("N2", "2");
    	senN3 = mPrefs.getString("N3", "3");
    	senN4 = mPrefs.getString("N4", "4");
    	senN5 = mPrefs.getString("N5", "5");
    	senN6 = mPrefs.getString("N6", "6");
    	senN7 = mPrefs.getString("N7", "7");
    	senN8 = mPrefs.getString("N8", "8");
    	senN9 = mPrefs.getString("N9", "9");
    	
    	senFA = mPrefs.getString("FA", "A");
    	senFB = mPrefs.getString("FB", "B");
    	senFC = mPrefs.getString("FC", "C");
    	senFD = mPrefs.getString("FD", "D");
    	senFE = mPrefs.getString("FE", "E");
    	senFF = mPrefs.getString("FF", "F");
    	senFG = mPrefs.getString("FG", "G");
    	senFH = mPrefs.getString("FH", "H");
    	senFI = mPrefs.getString("FI", "I");
    	senFJ = mPrefs.getString("FJ", "J");

    	senPA = mPrefs.getString("PA", "V");
    	senPI = mPrefs.getString("PI", "W");
    	senPS = mPrefs.getString("PS", "X");
    	senPD = mPrefs.getString("PD", "Y");
    	senPB = mPrefs.getString("PB", "Z");
    }

	
}
