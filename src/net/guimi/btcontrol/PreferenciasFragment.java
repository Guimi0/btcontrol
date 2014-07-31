/**
 * Copyright (c) 2014 Guimi
 * http://guimi.net
 */
package net.guimi.btcontrol;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Este fragmento gestiona la selección de preferencias
 * 
 * @author guimi
 *
 */
public class PreferenciasFragment  extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cargamos las preferencias desde el XML
        addPreferencesFromResource(R.xml.preferencias);
    }
}
