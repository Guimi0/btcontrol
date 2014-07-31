/**
 * Copyright (c) 2014 Guimi
 * http://guimi.net
 */
package net.guimi.btcontrol;

import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class WebActivity extends Activity {
    public static final String TAG = "WebActivity";    
	
    // Nuestro navegador
    private WebView navegador;
    // Esta variable nos indica la URL a cargar
    private int pagina;
    private String mURL;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtenemos datos enviados en el "fardo" (Bundle) por la actividad padre 
        Bundle miBundle = this.getIntent().getExtras();
    	String lang = Locale.getDefault().getLanguage();
        
        try {
        	pagina = miBundle.getInt("pagina");
        } catch (NullPointerException e) {
        	// Si no se especifica una URL, cargamos por defecto esta
        	pagina = 0;
        }

        if (lang.equals("es")) {
        	mURL = "file:///android_asset/ayuda";
        } else if (lang.equals("en")) {
        	mURL = "file:///android_asset/ayuda-en";
        }

        switch (pagina) {
        case 0:
        	mURL = mURL +".html";
        	break;
        case 1:
        	mURL = mURL +".html#creditos";
        	break;
        case 2:
        	mURL = mURL +".html#primo";
        	break;
        case 3:
        	mURL = mURL +".html#mando";
        	break;
        }
        
        // Indicamos la distribución de pantalla (layout) a cargar (xml)
        setContentView(R.layout.activity_web);

        // Obtenemos el navegador
        navegador = (WebView) findViewById(R.id.mWebView);
        
        // Cargamos la web de ayuda
    	navegador.loadUrl(mURL);
        //browser.loadData("<html><body>Hola<br><br>Prueba</body></html>", "text/html", "UTF-8");
        
    }
    
    
    @Override
    protected void onResume() {
        super.onResume();
        
        navegador.clearCache(true);
        navegador.reload();
    }


    
    
    
}
