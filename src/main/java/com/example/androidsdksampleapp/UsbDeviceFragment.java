package com.example.androidsdksampleapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.brother.sdk.common.ConnectorDescriptor;
import com.brother.sdk.common.ConnectorDescriptor.Function;
import com.brother.sdk.common.ConnectorManager.OnDiscoverConnectorListener;
import com.brother.sdk.common.IConnector;
import com.brother.sdk.common.device.CountrySpec;
import com.brother.sdk.common.device.Device;
import com.brother.sdk.usb.BrUsbDevice;
import com.brother.sdk.usb.discovery.UsbConnectorDiscovery;

import java.util.ArrayList;
import java.util.List;

public class UsbDeviceFragment extends Fragment
{
	private Handler mHandler = new Handler();

	// [Brother Comment]
	// ConnectorDescriptor is temporary handler to create IConnector which is interface and has all properties to use our devices.
	private List<ConnectorDescriptor> mDescriptors;
	// [Brother Comment]
	// UsbConnectorDiscovery can find our device connected with USB.
	private UsbConnectorDiscovery mDiscovery;

	private View mRootView;
	private FindDeviceAdapter mAdapter;
	private ProgressDialog mProgressDialog = null;
	private Button mDiscoveryStartButton;
	private Button mDiscoveryStopButton;
	private OnDiscoverConnectorListener mDiscoverConnectorListener = new OnDiscoverConnectorListener()
	{
		@Override
		public void onDiscover(ConnectorDescriptor descriptor)
		{
			if (descriptor.support(Function.Print) || descriptor.support(Function.Scan))
			{
				if (!mDescriptors.contains(descriptor))
				{
					final ConnectorDescriptor fDescriptor = descriptor;
					mHandler.post(new Runnable()
					{
						public void run()
						{
							mDescriptors.add(fDescriptor);
							mAdapter.notifyDataSetChanged();
						}
					});
				}
			}
		}
	};

	/**
	 *
	 */
	public static UsbDeviceFragment create()
	{
		UsbDeviceFragment fragment = new UsbDeviceFragment();
		return fragment;
	}

	public UsbDeviceFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mRootView = (View)inflater.inflate(R.layout.fragment_device, container, false);

		mDescriptors = new ArrayList<ConnectorDescriptor>();
		mDiscovery = new UsbConnectorDiscovery();

		// Set up Device List adapter
		mAdapter = new FindDeviceAdapter(getActivity(), R.layout.generic_find_device_item, mDescriptors, new OnDeviceSelectListener()
		{
			@Override
			public void onDeviceSelect(ConnectorDescriptor descriptor)
			{
				if (descriptor != null && mProgressDialog == null)
				{
					final ConnectorDescriptor fDescriptor = descriptor;
					mProgressDialog = createProgressDialog();
					mProgressDialog.show();
					Thread thread = new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							validateDevice(fDescriptor);
						}
					});
					thread.start();
				}
			}
		});

		ListView view = (ListView)mRootView.findViewById(R.id.device_list);
		view.setAdapter(mAdapter);
		view.requestFocus();

		//
		mDiscoveryStartButton = (Button)mRootView.findViewById(R.id.button_startdiscovery);
		mDiscoveryStartButton.setEnabled(true);
		mDiscoveryStartButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				mHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						mDiscoveryStartButton.setEnabled(false);
						mDiscoveryStopButton.setEnabled(true);
						mDiscovery.startDiscover(mDiscoverConnectorListener);
					}
				});
			}
		});

		//
		mDiscoveryStopButton = (Button)mRootView.findViewById(R.id.button_stopdiscovery);
		mDiscoveryStopButton.setEnabled(false);
		mDiscoveryStopButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				mHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						mDiscoveryStartButton.setEnabled(true);
						mDiscoveryStopButton.setEnabled(false);
						mDiscovery.stopDiscover();
					}
				});
			}
		});

		Activity activity = getActivity();
		if (activity instanceof MainActivityContext)
		{
			MainActivityContext controller = (MainActivityContext)activity;
			IConnector connector = controller.getConnector();
			if (connector != null)
			{
				updateConnectorInfo(connector);
			}
		}

		return mRootView;
	}

	public void showDialog(final String message, final String title)
	{
		DialogFragment fragment = new DialogFragment()
		{
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				if (title != null && !title.equals(""))
				{
					builder.setTitle(title);
				}
				builder.setMessage(message);
				builder.setNegativeButton("Close", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				});
				return builder.create();
			}
		};
		fragment.show(getFragmentManager(), "DeviceErrorDialog");
	}

	/**
	 * @return
	 */
	private ProgressDialog createProgressDialog()
	{
		ProgressDialog progressDialog = new ProgressDialog(getActivity());
		progressDialog.setMessage("Processing...");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		return progressDialog;
	}

	/**
	 * @param descriptor
	 */
	private void validateDevice(ConnectorDescriptor descriptor)
	{
		IConnector connector = null;
		try
		{
			connector = descriptor.createConnector(CountrySpec.fromISO_3166_1_Alpha2(getActivity().getResources().getConfiguration().locale.getCountry()));
			if (connector != null)
			{
				Activity activity = getActivity();
				if (activity instanceof MainActivityContext)
				{
					MainActivityContext controller = (MainActivityContext)activity;
					controller.onConnectorSelected(connector);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			final IConnector fConnector = connector;
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (mProgressDialog != null)
					{
						mProgressDialog.dismiss();
						mProgressDialog = null;
					}

					if (fConnector != null)
					{
						updateConnectorInfo(fConnector);
					}
					else
					{
						showDialog(getActivity().getResources().getString(R.string.error_devicevalidation), "Error");
					}
				}
			});
		}
	}

	private void updateConnectorInfo(IConnector connector)
	{
		TextView textView = (TextView)mRootView.findViewById(R.id.ipaddress);
		Object object = connector.getConnectorIdentifier();
		if (object instanceof BrUsbDevice)
		{
			BrUsbDevice usbDevice = (BrUsbDevice)object;
			textView.setText(usbDevice.mConnectionID);
		}
		else if (object instanceof String)
		{
			textView.setText((String)object);
		}
		else
		{
			textView.setText("Unclassified object");
		}

		Device device = connector.getDevice();
		TextView modelNameView = (TextView)mRootView.findViewById(R.id.ipaddresstitle);
		modelNameView.setText(device.modelName);
	}

	/**
	 *
	 */
	private interface OnDeviceSelectListener
	{
		void onDeviceSelect(ConnectorDescriptor descriptor);
	}

	/**
	 *
	 */
	private class FindDeviceAdapter extends ArrayAdapter<ConnectorDescriptor> implements OnClickListener,
			OnTouchListener
	{
		private final LayoutInflater mInflater;
		private final List<ConnectorDescriptor> mDeviceList;
		private final int mItemViewResourceId;
		private final OnDeviceSelectListener mListener;

		public FindDeviceAdapter(Context context, int viewResourceId, List<ConnectorDescriptor> deviceList, OnDeviceSelectListener listener)
		{
			super(context, viewResourceId, deviceList);

			mListener = listener;
			mItemViewResourceId = viewResourceId;
			mDeviceList = deviceList;
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder viewHolder = null;
			View view = convertView;

			// get internal data to draw.
			ConnectorDescriptor infoDevice;
			synchronized (mDeviceList)
			{
				infoDevice = mDeviceList.get(position);
			}

			if (view == null)
			{
				viewHolder = new ViewHolder();
				view = mInflater.inflate(mItemViewResourceId, null);
				view.setClickable(true);
				view.setFocusable(true);
				view.setEnabled(true);

				// set event listener.
				view.setOnClickListener(this);
				view.setOnTouchListener(this);

				viewHolder.mModelNameView = (TextView)view.findViewById(R.id.generic_find_printer_item_modelname);
				viewHolder.mAddressView = (TextView)view.findViewById(R.id.generic_find_printer_item_address);
				view.setTag(viewHolder);
			}
			else
			{
				viewHolder = (ViewHolder)view.getTag();
			}
			viewHolder.mDescriptor = infoDevice;

			String modelName = infoDevice.getModelName();
			String ipAddress = infoDevice.getDescriptorIdentifier();

			// null check
			if (modelName == null)
			{
				modelName = "";
			}
			if (ipAddress == null)
			{
				ipAddress = "";
			}
			viewHolder.mModelNameView.setText(modelName);
			viewHolder.mAddressView.setText(ipAddress);

			return view;
		}

		public void onClick(View view)
		{
			ViewHolder viewHolder = (ViewHolder)view.getTag();
			ConnectorDescriptor descriptor = viewHolder.mDescriptor;
			mListener.onDeviceSelect(descriptor);
		}

		public boolean onTouch(View aView, MotionEvent event)
		{
			switch (event.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					aView.setBackgroundResource(android.R.drawable.list_selector_background);
					break;
				case MotionEvent.ACTION_UP:
					aView.setBackgroundResource(android.R.color.transparent);
					break;
				default:
					break;
			}
			return false;
		}

		private class ViewHolder
		{
			TextView mModelNameView;
			TextView mAddressView;
			ConnectorDescriptor mDescriptor;
		}
	}
}
