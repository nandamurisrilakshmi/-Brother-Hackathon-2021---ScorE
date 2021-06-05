package com.example.androidsdksampleapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Locale;

import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.brother.sdk.BrotherAndroidLib;
import com.brother.sdk.common.IConnector;
import com.brother.sdk.network.NetworkControllerManager;
import com.brother.sdk.network.wifi.WifiNetworkController;
import com.brother.sdk.usb.UsbControllerManager;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;

import android.app.Activity;
//import android.app.ActionBar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
//import android.app.FragmentManager;
import androidx.fragment.app.FragmentManager;
import android.app.FragmentTransaction;
//import androidx.fragment.app.FragmentTransaction;

//import androidx.legacy.app.FragmentPagerAdapter;


import android.os.Bundle;
import android.os.Environment;

//import androidx.legacy.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;


public class MainActivity extends AppCompatActivity implements ActionBar.TabListener, MainActivityContext
{
	/**
	 * The {@link PagerAdapter} that will provide
	 * fragments for each of the sections. We use a {@link FragmentPagerAdapter}
	 * derivative, which will keep every
	 * loaded fragment in memory. If this becomes too memory intensive, it
	 * may be best to switch to a
	 * {@link }.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	// [Brother Comment]
	// IConnector is the interface to handle our device, which is created by ConnectorDescriptor.
	private IConnector mConnector;
	private File mTemporaryWorkFolder;

	/**
	 *
	 * @param savedInstanceState
	 */
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

		try {
			// Add these lines to add the AWSCognitoAuthPlugin and AWSS3StoragePlugin plugins
			Amplify.addPlugin(new AWSCognitoAuthPlugin());
			Amplify.addPlugin(new AWSS3StoragePlugin());
			Amplify.configure(getApplicationContext());

			Log.i("MyAmplifyApp", "Initialized Amplify");
		} catch (AmplifyException error) {
			Log.e("MyAmplifyApp", "Could not initialize Amplify", error);
		}

		// [Brother Comment]
		// Before the use of our SDK, BrotherAndroidLib must be initialized by Application Context.
		// Otherwise, it could cause Runtime Exception at many places in our library.
		BrotherAndroidLib.initialize(getApplicationContext());

		// Initialize Temporary Folder
		String packageName = getPackageName();
		String path = String.format("%s/Android/data/%s/files", Environment.getExternalStorageDirectory().getPath(), packageName);
		mTemporaryWorkFolder = new File(path);
		maintenanceCacheFolder(mTemporaryWorkFolder);

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager ());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager)findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
		{
			@Override public void onPageSelected(int position)
			{
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++)
		{
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}
	}

	@Override protected void onStart()
	{
		super.onStart();
		//UsbDebug.initialize(new File(String.format("%s/DebugUsb", Environment.getExternalStorageDirectory())));
		// [Brother Comment]
		// UsbController must be started before Activity start for USB connection.
		UsbControllerManager.getUsbController().startControl();
	}

	@Override protected void onStop()
	{
		super.onStop();
		// [Brother Comment]
		// UsbController must be stopped before Activity stop for USB connection, otherwise it might cause resource leak.
		UsbControllerManager.getUsbController().stopControl();
		//UsbDebug.terminate();
	}

	@Override protected void onPause()
	{
		super.onPause();
	}

	@Override protected void onDestroy()
	{
		super.onDestroy();
	}

	@Override public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}



	@Override
	public void onTabSelected(ActionBar.Tab tab, androidx.fragment.app.FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());

	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, androidx.fragment.app.FragmentTransaction ft) {

	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, androidx.fragment.app.FragmentTransaction ft) {

	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter
	{
		private static final int WIFIDEVICE_PAGE = 1;
		private static final int USBDEVICE_PAGE = 2;
		private static final int PRINT_PAGE = 0;
		private static final int SCAN_PAGE = 3;

		public SectionsPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override public Fragment getItem(int position)
		{
			// getItem is called to instantiate the fragment for the given page.
			// Return a PlaceholderFragment (defined as a static inner class below).
			if (position == WIFIDEVICE_PAGE)
			{
				WifiNetworkController controller = NetworkControllerManager.getWifiNetworkController();
				ArrayList<String> broadcastAddresses = new ArrayList<String>();
				broadcastAddresses.add("172.16.33.79");
				broadcastAddresses.add(controller.getBroadcastAddress().getHostAddress());
				return DeviceFragment.create(broadcastAddresses);
			}
			else if (position == USBDEVICE_PAGE)
			{
				return UsbDeviceFragment.create();
			}
			else if (position == PRINT_PAGE)
			{
				return ScanResultActivity.create();
			}
			else if (position == SCAN_PAGE)
			{
				return ScanFragment.create();
			}
			return PlaceholderFragment.newInstance(position + 1);
		}

		@Override public int getCount()
		{
			// Show 2 total pages.
			return 4;
		}

		@Override public CharSequence getPageTitle(int position)
		{
			Locale l = Locale.getDefault();
			switch (position)
			{
				case WIFIDEVICE_PAGE:
					return "Wifi".toUpperCase(l);
				case USBDEVICE_PAGE:
					return "USB".toUpperCase(l);
				case PRINT_PAGE:
					return "Scanned Files".toUpperCase(l);
				case SCAN_PAGE:
					return getString(R.string.title_scan).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment
	{
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section
		 * number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber)
		{
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment()
		{
		}

		@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}



	/**
	 *
	 */
	private void maintenanceCacheFolder(File temporaryFolder)
	{
		try
		{
			if (!temporaryFolder.exists())
			{
				temporaryFolder.mkdirs();
			}

			File[] files = temporaryFolder.listFiles();
			if (files != null)
			{
				for (int cnt = 0; cnt < files.length; ++cnt)
				{
					files[cnt].delete();
				}
			}
		}
		catch (Exception e)
		{
		}
	}

	@Override public void onConnectorSelected(IConnector connector)
	{
		mConnector = connector;
	}

	@Override public IConnector getConnector()
	{
		return mConnector;
	}

	@Override public File getWorkingFolder()
	{
		return mTemporaryWorkFolder;
	}
}
