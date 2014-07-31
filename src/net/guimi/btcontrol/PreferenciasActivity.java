/**
 * Copyright (c) 2014 Guimi
 * http://guimi.net
 */
package net.guimi.btcontrol;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

public class PreferenciasActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mostramos el fragmento como contenido principal
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferenciasFragment())
                .commit();
    }
    

    private void resetValues() {
    	// http://www.devlog.en.alt-area.org/?p=1209

		PreferenceManager
    		.getDefaultSharedPreferences(this)
    		.edit()
    		.clear()
    		.commit();
    	PreferenceManager.setDefaultValues(this, R.xml.preferencias, true);

    	finish();
    	overridePendingTransition(0, 0);
    	startActivity(getIntent());
    	overridePendingTransition(0, 0);
    }

    
    //****************************************************************************
	//    GESTIÓN DE MENÚ
    //****************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Cargamos el menú; esto añade los elementos a la "action bar"
		getMenuInflater().inflate(R.menu.preferencias, menu);
        return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Gestionamos el elemento seleccionado
        switch (item.getItemId()) {
        case R.id.preferencias_menu_Reset:
        	resetValues();
            return true;
        default:
            return true;
        }
	}

}
