package com.example.androidsdksampleapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class ScanResultActivity extends Activity
{
	public static final String SCAN_IMAGE_RESULTS = "ScanImageResults";

	public static Fragment create() {
		Fragment fragment = new Fragment();
		return fragment;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);
		Intent callerIntent = getIntent();

		String path = getResults(callerIntent);
		File file=new File(path);
		Log.d("In sra got path",path);
		String arr[]=file.list();
		List l=new ArrayList<>();
		for(String i:arr){
			//if(i.endsWith(".txt")){
				l.add(i);
				Log.d("L array",i);
		//	}
		}
		ArrayAdapter adapter = new ArrayAdapter<String>(this,R.layout.activity_listview, l);

		ListView listView = (ListView) findViewById(R.id.mobile_list);
		listView.setAdapter(adapter);
		/*Uri uri = Uri.parse("file://" + l.get(0));

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(uri);
		startActivity(intent);*/
	}

	private String getResults(Intent intent)
	{
		String results="aa";
		Bundle bundle = intent.getExtras();
		if (bundle != null && bundle.containsKey(SCAN_IMAGE_RESULTS))
		{
			results = bundle.getString(SCAN_IMAGE_RESULTS);
			Log.d("get resukts ",results);



			}
		return results;
		}



}
