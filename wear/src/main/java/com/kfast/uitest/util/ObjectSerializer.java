package com.kfast.uitest.util;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectSerializer {

	public static void saveSerializedObject(Context context, Object object,String filename){
		try
	      {
	         FileOutputStream fileOut = context.openFileOutput(filename, Context.MODE_PRIVATE);
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(object);
	         out.close();
	         fileOut.close();
	      }catch(IOException i) {
	          i.printStackTrace();
	      }
	}
	
	/**
	 * 
	 * @param filename - filename of the serialized file
	 * @param cleanup If true, file will be deleted after inflating 
	 * @return	Object
	 */
	public static Object loadSerializedObject(Context context, String filename, boolean cleanup){
		
		Object object = null;
		try{
			
			File file = context.getFileStreamPath(filename);
			if(file.exists()){
				FileInputStream fileIn = context.openFileInput(filename);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				object = in.readObject();

				in.close();
				fileIn.close();

				if(cleanup){
					context.deleteFile(filename);
				}
			}
			else return null;
	         
	     }
		catch(IOException i){
	         i.printStackTrace();
	         return null;
	    }
		catch(ClassNotFoundException c){
	         c.printStackTrace();
	         return null;
	    }
		return object;
	}
	
}
