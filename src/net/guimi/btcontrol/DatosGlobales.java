/**
 * Copyright (c) 2014 Guimi
 * http://guimi.net
 */
package net.guimi.btcontrol;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * La clase aplicación mantiene datos globales accesibles desde todas las actividades
 * 
 * @author guimi
 *
 */
public class DatosGlobales extends Application {

	//******************************************************
	// DATOS GLOBALES
	//******************************************************

	/** Variable auxiliar que guarda el identificador del dispositivo Android */
    private static String android_id;
	public static String getAndroid_id() {
		return android_id;}
	public static void setAndroid_id(String android_id) {
		DatosGlobales.android_id = android_id;}
    
	
	/** Variable auxiliar que indica el nombre de la aplicacion */
	private static String nombreAplicacion = "BTControl";
	public static String getnombreAplicacion() {
		return nombreAplicacion;}

	
	/** Variable auxiliar que indica el paquete de la aplicacion */
	private static String paqueteAplicacion = "net.guimi.btcontrol";
	public static String getPaqueteAplicacion() {
		return paqueteAplicacion;}
	public static void setPaqueteAplicacion(String paqueteAplicacion) {
		DatosGlobales.paqueteAplicacion = paqueteAplicacion;}
	
	
	/** Variable auxiliar que indica la versión de la aplicación */
    private static String versionAplicacion;
	public static String getVersionAplicacion() {
		return versionAplicacion;}
	public static void setVersionAplicacion(String versionAplicacion) {
		DatosGlobales.versionAplicacion = versionAplicacion;}


	/** Variable auxiliar que mantiene el contexto de la aplicación */
    private static Context context;
    public static Context getAppContext() {
        return DatosGlobales.context;}

	
	/** Variable auxiliar que indica si se debe mostrar la actividad Splash */
	private static boolean mostrarSplash = true;
	public static boolean isMostrarSplash() {
		return mostrarSplash;}
	public static void setMostrarSplash(boolean mostrarSplash) {
		DatosGlobales.mostrarSplash = mostrarSplash;}
	

	/** Variable auxiliar que mantiene la consola */
    private static String consola="";
	public static String getConsola() {
		return consola;}
	public static void setConsola(String consola) {
		DatosGlobales.consola = consola;}

	public static void addConsola(String TAG, String consola) {
		DatosGlobales.consola = DatosGlobales.consola + TAG + ": " + consola + "\n";
	}
	public static void addConsola(String TAG, String consola, boolean debug) {
		addConsola(TAG, consola);
        //Log.v(TAG, "Iniciamos la aplicacion - Verbose"); // mensajes verdes
		/*
		V — Verbose (lowest priority) - Eclipse/LogCat: mensajes en verde
		D — Debug					  - Eclipse/LogCat: mensajes en rojo
		I — Info					  - Eclipse/LogCat: mensajes en negro
		W — Warning
		E — Error
		F — Fatal
		S — Silent (highest priority, on which nothing is ever printed)
		*/
        Log.i(TAG, consola);
	}

    
	//******************************************************
	// FUNCIONES
	//******************************************************

    public void onCreate(){
        super.onCreate();
        DatosGlobales.context = getApplicationContext();
    }


	public static String getLocalIPv4Address() {
	    try {
	        for (Enumeration<NetworkInterface> mNetworkInterfaces = NetworkInterface
	                .getNetworkInterfaces(); mNetworkInterfaces.hasMoreElements();) {
	            NetworkInterface intf = mNetworkInterfaces.nextElement();
	            for (Enumeration<InetAddress> mInetAddresses = intf
	                    .getInetAddresses(); mInetAddresses.hasMoreElements();) {
	                InetAddress mInetAddress = mInetAddresses.nextElement();
	                System.out.println("ip1--:" + mInetAddress);
	                System.out.println("ip2--:" + mInetAddress.getHostAddress());

	                // for getting IPV4 format
	                if (!mInetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(mInetAddress.getHostAddress())) {
	                    return mInetAddress.getHostAddress();
	                }
	            }
	        }
	    } catch (Exception ex) {
	        Log.e("IP Address", ex.toString());
	    }
	    return null;
	}
	
	
}
