/*
This file is part of Piller.

Piller is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Piller is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Piller. If not, see <http://www.gnu.org/licenses/>.

Copyright 2015, Giulio Fagioli, Lorenzo Salani
*/
package com.chummy.jezebel.darkmaterial.colors;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.app.*;
import android.view.*;

public class ThemeActivity extends ActionBarActivity {

    String ThemeName;
    String ThemePackage;
    String ThemeColor;
    String ThemeMotto;
    String ThemeDarkColor;
    String ThemeAccentColor;
    String ThemeHighlightColor;
    String FileName;
    ImageButton installButton;
	boolean isFileCopied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_activity_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.include2);
        setSupportActionBar(toolbar);
        Intent intent = getIntent();
        if (intent != null) {
            String ThemeArray[] = intent.getStringArrayExtra("ThemeArray");
            ThemeName = ThemeArray[0];
            ThemePackage = ThemeArray[1];
            ThemeColor = ThemeArray[2];
            ThemeDarkColor = ThemeArray[3];
            ThemeAccentColor = ThemeArray[4];
            ThemeHighlightColor = ThemeArray[5];
            ThemeMotto = ThemeArray[6];
        }

        Window window = this.getWindow();
        window.setStatusBarColor(Color.parseColor(ThemeDarkColor));
        window.setNavigationBarColor(Color.parseColor(ThemeColor));
        toolbar.setBackgroundColor(Color.parseColor(ThemeColor));
        toolbar.setSubtitle(ThemeName);


        FileName = ThemeName + ".apk";


        installButton = (ImageButton) findViewById(R.id.fab);

        Drawable fabBackground = getResources().getDrawable(R.drawable.fab_background);
        fabBackground.setColorFilter(Color.parseColor(ThemeHighlightColor), PorterDuff.Mode.ADD);

        installButton.setBackground(fabBackground);
        // installButton.setImageDrawable(getResources().getDrawable(R.drawable.plus));


        if (PackageInstalled(ThemePackage)) {
            installButton.setClickable(true);
            installButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_uninstall_icon));
            installButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Uninstall(ThemePackage);
                }
            });

        } else {
            installButton.setClickable(true);
            installButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_install_icon));
            installButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDialog();
                }
            });
        }

        AssetManager am = getAssets();
        InputStream inputStream = null;
        LinearLayout myGallery = (LinearLayout) findViewById(R.id.gallery_image);

        try {
            String themes[] = am.list("Images/" + ThemeName);//Names of all the images in Images/yourtheme folder
            for (int i = 0; i < themes.length; i++) {
                inputStream = am.open("Images/" + ThemeName + "/" + themes[i]);//ThemeName is the name present in theme_names arrays
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ImageView photo = new ImageView(this);
                photo.setScaleType(ImageView.ScaleType.FIT_XY);
                photo.setImageBitmap(bitmap);
                myGallery.addView(photo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PackageInstalled(ThemePackage)) {
            //clean of the /Themes/ folder
            installButton.setClickable(true);
            installButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_uninstall_icon));
            installButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Uninstall(ThemePackage);
                }
            });
            delete(new File(Environment.getExternalStorageDirectory() + "/Themes/"));
        } else {
            installButton.setClickable(true);
            installButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_install_icon));
            installButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDialog();
                }
            });

        }
    }

    protected void showDialog() {
        CopyThemeTask task = new CopyThemeTask();//Copy the selected theme in /Themes/
        task.execute();
		
		long toastTime = new java.util.Date().getTime(); //get time before loop

		//We need to do something in this loop to keep from having an anr error
		//So toast is probably best bet over Thread.sleep()
		while(!this.isFileCopied) {
			long cur = new java.util.Date().getTime(); //get time NOW
			if((toastTime + 1000) >= cur) { //Only after 1 second can we show a toast
				toastTime = cur;
				Toast.makeText(this, this.getString(R.string.copying), Toast.LENGTH_SHORT).show();
			}
		}
		
		//Watch for installation, when it's installed, we can delete old stuff
		CheckInstallationTask check = new CheckInstallationTask();
		check.execute();
		
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Themes/" + FileName)), "application/vnd.android.package-archive");
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
				
          this.isFileCopied = false;
		  
		  
		  
    }

    void delete(File file) {

        if (file.isDirectory())
            for (File child : file.listFiles()) {
                File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
                child.renameTo(to);
                delete(child);
            }

        file.delete();  // delete child file or empty directory
    }

    public boolean PackageInstalled(String target_package) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(target_package, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    /*
        public void OpenThemeInSettings(String target_package) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.putExtra("pkgName", target_package);
            intent.setComponent(new ComponentName("org.cyanogenmod.theme.chooser", "org.cyanogenmod.theme.chooser.ChooserActivity"));
            startActivity(intent);
        }
    */
    public void Uninstall(String targetPackage) {
        Uri packageUri = Uri.parse("package:" + targetPackage);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE,
                packageUri);
        startActivity(uninstallIntent);
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_rate) {
            Rate("https://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName());
            Toast.makeText(getApplicationContext(), this.getResources().getString(R.string.rate_thanks), Toast.LENGTH_SHORT).show();
        }
        if (id == R.id.menu_share) {
            Share("https://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName());
        }
        if (id == R.id.menu_developer) {
            Link(this.getResources().getString(R.string.developer_site));

        }
        if (id == R.id.menu_mail) {
            Mail(this.getResources().getString(R.string.app_name), this.getResources().getString(R.string.email_address));

        }
        if (id == R.id.community) {
            Link(this.getResources().getString(R.string.community_link));

        }

        return super.onOptionsItemSelected(item);
    }

    public void Share(String playStoreLink) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, playStoreLink);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    public void Rate(String playStoreLink) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(playStoreLink));
        startActivity(browserIntent);
    }

    public void Mail(String themeName, String email) {
        Intent mailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + email));
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, themeName);
        startActivity(mailIntent);
    }

    public void Link(String link) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        startActivity(browserIntent);
    }

    private class CopyThemeTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... filename) {
            String response = "";
            AssetManager assetManager = getAssets();
            System.out.println("File name => " + FileName);
            InputStream in = null;
            OutputStream out = null;
            try {
                File wallpaperDirectory = new File(Environment.getExternalStorageDirectory() + "/Themes/");
                wallpaperDirectory.mkdirs();

                in = assetManager.open("Files/" + FileName);   // if files resides inside the "Files" directory itself
                System.out.println(FileName);
                out = new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/Themes/" + FileName);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
				ThemeActivity.this.isFileCopied = true;
				response = "Done!";

            } catch (Exception e) {
                Log.e("tag", "Failed to copy asset file: " + FileName, e);
				ThemeActivity.this.isFileCopied = false;
				response = "Failed!";
            }

		
            return response;
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            //dismissDialog();
        }
    }
	
	
	private class CheckInstallationTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... filename) {
            String response = "";
                while(!ThemeActivity.this.PackageInstalled(ThemeActivity.this.ThemePackage)) {
					; //Stall while not installed
				}
				//Now it's installed, we can delete
			    ThemeActivity.this.deleteFile(ThemeActivity.this.FileName);
				return response;
        }
	}
}

