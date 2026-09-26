package dev.mfarm.com.mfarm.dao;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;


import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebView;

import dev.mfarm.com.mfarm.MainActivity;

public class PullDatabase {
	Context context;

	  public PullDatabase()
	  {

	  }
	  public void getDatabase(){
		  Log.v("PullDatabase", "You are now trying to pull the database");	  
		  try {
              File sd = Environment.getExternalStorageDirectory();
              File data = Environment.getDataDirectory();
              
              String pName = this.getClass().getPackage().getName();
              
              if (sd.canWrite()) {
            	  
            	 // String packageName = context.getPackageName();
          		//DB_PATH = "/data/data/"+packageName+"/databases/";
            	  
            	  Log.v("PullDatabse", "Can write sd card "+pName);
            	  String currentDBPath = "/data/data/"+pName+"/databases/"+ MainActivity.DB_NAME;
                  //String currentDBPath = "/data/" + getPackageName() + "/databases/yourdatabasename";
                  String backupDBPath = MainActivity.DB_NAME;
                  File currentDB = new File(currentDBPath);
                  File backupDB = new File(sd, backupDBPath);

                  if (currentDB.exists()) {
                	  Log.v("PullDatabase", "current db exists");
                      FileChannel src = new FileInputStream(currentDB).getChannel();
                      FileChannel dst = new FileOutputStream(backupDB).getChannel();
                      dst.transferFrom(src, 0, src.size());
                      src.close();
                      dst.close();
                      Log.v("PullDatabase", "Database pulled successfully");
                      //Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();
                  }
                  else{
                	  Log.v("PullDatabase", "current database does not exist");
                  } 	
              }
              else
            	  Log.v("PullDatabse", "Cannot write the sd card");
          } catch (Exception e) {

          }
	  }
}
