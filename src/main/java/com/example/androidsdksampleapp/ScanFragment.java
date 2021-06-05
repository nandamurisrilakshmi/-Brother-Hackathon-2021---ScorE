package com.example.androidsdksampleapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.brother.sdk.common.IConnector;
import com.brother.sdk.common.Job.JobState;
import com.brother.sdk.common.device.ColorProcessing;
import com.brother.sdk.common.device.Device;
import com.brother.sdk.common.device.Duplex;
import com.brother.sdk.common.device.MediaSize;
import com.brother.sdk.common.device.Resolution;
import com.brother.sdk.common.device.scanner.ScanPaperSource;
import com.brother.sdk.common.device.scanner.ScanSpecialMode;
import com.brother.sdk.common.device.scanner.ScanType;
import com.brother.sdk.scan.ScanJob;
import com.brother.sdk.scan.ScanJobController;
import com.brother.sdk.scan.ScanParameters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.AutoText;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ScanFragment extends Fragment implements View.OnTouchListener
{
	private Handler mHandler = new Handler();

	private View mRootView;
	private final static int REQUEST_SCANRESULTSHOW = 0;
	private Thread mScanExecutionThread = null;
	private ProgressDialog mProgressDialog;
	private Button mScanButton;
	private File mWorkingFolder;
	private TextView mDocSizeItem;
	private TextView mScanColorItem;
	private TextView mDuplexItem;
    private String sImage;
	// [Brother Comment]
	// ScanParameters is used with ScanJob which would be submitted to IConnector to make scan execute to our device.
	private ScanParameters mScanParameters = new ScanParameters();
	// [Brother Comment]
	// ScanJob is used to make scan execute to our device with IConnector.
	private ScanJob mScanJob = null;
	Bundle bundle = new Bundle();

	/**
	 * 
	 */
	public static ScanFragment create()
	{
		ScanFragment fragment = new ScanFragment();
		return fragment;
	}

	/**
	 *
	 */
	public ScanFragment()
	{
	}

	/**
	 *
	 * @param inflater
	 * @param container
	 * @param savedInstanceState
	 * @return
	 */
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mRootView = (View)inflater.inflate(R.layout.fragment_scan, container, false);

		mDocSizeItem = (TextView)mRootView.findViewById(R.id.setting_scan_documentsize_item);
		mScanColorItem = (TextView)mRootView.findViewById(R.id.setting_scan_color_item);
		mDuplexItem = (TextView)mRootView.findViewById(R.id.setting_scan_duplex_item);

		// [Brother Comment]
		// These scan parameters are initialized when this view is created.
		mScanParameters.documentSize = MediaSize.A4;
		mScanParameters.colorType = ColorProcessing.FullColor;
		mScanParameters.duplex = Duplex.Simplex;
		LinearLayout documentSize = (LinearLayout)mRootView.findViewById(R.id.setting_scan_documentsize);
		documentSize.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				IConnector connector = getConnector();
				Device device = getBrotherDevice();
				if (connector != null && device != null && device.scanner != null)
				{
					final CharSequence autoText = "Auto";
					// [Brother Comment]
					// Each paper sources (Flatbed, ADF Front and ADF Back) has each Document Size capabilities.
					//  Flatbed : Device.scanner.capabilities.documentSizes(ScanType.FlatbedScan)
					//  ADF front : Device.scanner.capabilities.documentSizes(ScanType.ADFSimplexScan)
					//  ADF Back : Device.scanner.capabilities.documentSizes(ScanType.ADFDuplexScan)
					List<MediaSize> params = null;
					if (mScanParameters.duplex == Duplex.Simplex)
					{
						params = device.scanner.capabilities.documentSizes.get(ScanType.ADFSimplexScan);
					}
					else
					{
						params = device.scanner.capabilities.documentSizes.get(ScanType.ADFDuplexScan);
					}

					final boolean bAutoDocumentSizeScan = device.scanner.capabilities.specialScanModes.contains(ScanSpecialMode.CORNER_SCAN) || device.scanner.autoDocumentSizeScanSupport;

					CharSequence[] docSizes = null;
					if (bAutoDocumentSizeScan)
					{
						docSizes = new CharSequence[params.size() + 1];
						int index = 0;
						for (MediaSize m : params)
						{
							docSizes[index++] = m.name;
						}

						// [Brother Comment]
						// Auto Document Size scan is not included in Document Size capabilities, so if UI has "Auto Document Size" option in Document Size, it should be added by yourself.
						docSizes[index] = autoText;
					}
					else
					{
						docSizes = new CharSequence[params.size()];
						int index = 0;
						for (MediaSize m : params)
						{
							docSizes[index++] = m.name;
						}
					}

					final List<MediaSize> mediaSizes = params;
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setTitle(R.string.setting_scan_documentsize);
					builder.setItems(docSizes, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							if (mediaSizes.size() == which && bAutoDocumentSizeScan)
							{
								mScanParameters.autoDocumentSizeScan = true;
								mScanParameters.documentSize = MediaSize.Letter; // Anything is OK.
								mDocSizeItem.setText(autoText);
							}
							else
							{
								mScanParameters.autoDocumentSizeScan = false;
								mScanParameters.documentSize = mediaSizes.get(which);
								mDocSizeItem.setText(mediaSizes.get(which).name);
							}
						}
					});
					builder.show();
				}
			}
		});
		documentSize.setOnTouchListener(this);

		LinearLayout scanColor = (LinearLayout)mRootView.findViewById(R.id.setting_scan_color);
		scanColor.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				IConnector connector = getConnector();
				Device device = getBrotherDevice();
				if (connector != null && device != null && device.scanner != null)
				{
					final CharSequence[] colors = new CharSequence[]{"Color", "Color (Fast)", "Black and White"};

					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setTitle(R.string.setting_scan_color);
					builder.setItems(colors, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							switch (which)
							{
								default:
								case 0:
									mScanParameters.colorType = ColorProcessing.FullColor;
									mScanParameters.resolution = new Resolution(300, 300);
									break;
								case 1:
									mScanParameters.colorType = ColorProcessing.FullColor;
									mScanParameters.resolution = new Resolution(100, 100);
									break;
								case 2:
									mScanParameters.colorType = ColorProcessing.BlackAndWhite;
									mScanParameters.resolution = new Resolution(200, 200);
									break;
							}
							mScanColorItem.setText(colors[which]);
						}
					});
					builder.show();
				}
			}
		});
		scanColor.setOnTouchListener(this);

		LinearLayout duplex = (LinearLayout)mRootView.findViewById(R.id.setting_scan_duplex);
		duplex.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				IConnector connector = getConnector();
				Device device = getBrotherDevice();
				if (connector != null && device != null && device.scanner != null)
				{
					final CharSequence[] duplex = new CharSequence[]{"Off", "Flip On Long Edge", "Flip On Short Edge"};

					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setTitle(R.string.setting_scan_duplex);
					builder.setItems(duplex, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							switch (which)
							{
								default:
								case 0:
									mScanParameters.duplex = Duplex.Simplex;
									break;
								case 1:
									mScanParameters.duplex = Duplex.FlipOnLongEdge;
									break;
								case 2:
									mScanParameters.duplex = Duplex.FlipOnShortEdge;
									break;
							}
							mDuplexItem.setText(duplex[which]);
						}
					});
					builder.show();
				}
			}
		});
		duplex.setOnTouchListener(this);

		//
		mScanButton = (Button)mRootView.findViewById(R.id.button_scan);
		mScanButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				IConnector connector = null;
				Activity activity = getActivity();
				if (activity instanceof MainActivityContext)
				{
					MainActivityContext controller = (MainActivityContext) activity;
					connector = controller.getConnector();
				}

				if (connector != null)
				{
					final IConnector fConnector = connector;
					if (mScanExecutionThread == null && mProgressDialog == null)
					{
						mProgressDialog = createProgressDialog();
						mProgressDialog.show();
						mScanExecutionThread = new Thread(new Runnable()
						{
							@Override
							public void run()
							{
								executeScan(fConnector);
							}
						});
						mScanExecutionThread.start();
					}
				}
			}
		});

		Activity activity = getActivity();
		if (activity instanceof MainActivityContext)
		{
			MainActivityContext controller = (MainActivityContext) activity;
			mWorkingFolder = controller.getWorkingFolder();
			Log.d("working folder",mWorkingFolder.getPath());
		}

		return mRootView;
	}

	// [Brother Comment]
	// IConnector can handle connection to our device.
	private IConnector getConnector()
	{
		Activity activity = getActivity();
		if (activity instanceof MainActivityContext)
		{
			MainActivityContext controller = (MainActivityContext) activity;
			return controller.getConnector();
		}
		return null;
	}

	// [Brother Comment]
	// Device has our device capabilities which of device has been associated with IConnector..
	private Device getBrotherDevice()
	{
		IConnector connector = getConnector();
		if (connector != null)
		{
			return connector.getDevice();
		}
		return null;
	}

	/**
	 *
	 * @param aView
	 * @param event
	 * @return
	 */
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

	/**
	 *
	 * @param
	 */
	private void uploadFile(String scannedImagePath) {
	/*	Log.d("upload file","success");


		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(scannedImagePath));
			writer.append("Example file contents");
			writer.close();
		} catch (Exception exception) {
			Log.e("MyAmplifyApp", "Upload failed", exception);
		}

		Amplify.Storage.uploadFile(
				"ExampleKey",
				exampleFile,
				result -> Log.i("MyAmplifyApp", "Successfully uploaded: " + result.getKey()),
				storageFailure -> Log.e("MyAmplifyApp", "Upload failed", storageFailure)
		);*/
            sImage = scannedImagePath;
			File exampleFile = new File(scannedImagePath);
Log.d("file",exampleFile.getPath());


			Amplify.Storage.uploadFile(
					"ExampleKey",
					exampleFile,
					result -> Log.i("MyAmplifyApp", "Successfully uploaded: " + result.getKey()),
					storageFailure -> Log.e("MyAmplifyApp", "Upload failed", storageFailure)
			);

	/*	try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
			Thread.currentThread().interrupt();
		}*/
		Log.d("sleep done",mWorkingFolder.getPath());
		String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

		Log.d("date and time", date);

		Amplify.Storage.downloadFile(
						"ExampleKey",
						new File(mWorkingFolder.getPath() + "/example "+date+".txt"),
						StorageDownloadFileOptions.defaultInstance(),
						progress -> Log.i("MyAmplifyApp", "Fraction completed: " + progress.getFractionCompleted()),
						result -> Log.i("MyAmplifyApp", "Successfully downloaded: " + result.getFile().getName()),
						error -> Log.e("MyAmplifyApp",  "Download Failure", error)
				);
		new CountDownTimer(2000, 100) {

			public void onTick(long millisUntilFinished) {

			}

			public void onFinish() {
				Log.d("done timer start intent",bundle.toString());
				Intent _intent = new Intent(getActivity(), ScanResultActivity.class);
				_intent.putExtras(bundle);
				startActivityForResult(_intent, REQUEST_SCANRESULTSHOW);
			}
		}.start();

		}

	private void executeScan(IConnector connector)
	{
		final ArrayList<String> fScanResults = new ArrayList<String>();
		final Map<Integer, String> fscanResultsMap = new HashMap<Integer, String>(); // [Brother Comment] This hash map manages "PageIndex" and "ScanImagePath".
		JobState jobState = JobState.ErrorJob;
		try
		{
			mScanJob = new ScanJob(mScanParameters, getActivity(), new ScanJobController(mWorkingFolder)
			{
				// [Brother Comment]
				// The "value" is progress value in scan processing which is between 0 to 100 per page.
				@Override public void onUpdateProcessProgress(int value)
				{
					// TODO Auto-generated method stub
				}

				// [Brother Comment]
				// This callback would not be called if any response has not come from our device.
				@Override public void onNotifyProcessAlive()
				{
					// TODO Auto-generated method stub
				}

				// [Brother Comment]
				// This callback would be called when scanned image has been retained.
				@Override public void onImageReadToFile(String scannedImagePath, int pageIndex)
				{
					fscanResultsMap.put(pageIndex, scannedImagePath);
					Log.d("scanfile path",scannedImagePath);
					sImage=scannedImagePath;
					//uploadFile(scannedImagePath);
				}
			});
			// [Brother Comment]
			// The process has been executed synchronously, so in almost cases you should implement the call of IConnector.submit(ScanJob) in Thread.
			jobState = connector.submit(mScanJob);
		}
		catch (Exception e)
		{
		}
		finally
		{
			final JobState fJobState = jobState;
			final ScanJob fScanJob = mScanJob;
			mScanExecutionThread = null;
			mScanJob = null;
			mHandler.post(new Runnable()
			{
				@Override public void run()
				{
					if (fJobState == JobState.SuccessJob)
					{
						//Bundle bundle = new Bundle();

						// [Brother Comment]
						// This process sorts the keys(=PageIndex) of the hash map.
						List<Integer> sortedkeys = new ArrayList<Integer>(fscanResultsMap.keySet());
						Collections.sort(sortedkeys);

						// [Brother Comment]
						// This process stores sorted values(=ScanImagePaths) in the list by using sorted keys(=PageIndex).
						// (To make the results of Duplex scanning the correct order)
						for (Integer nKey: sortedkeys) {
							fScanResults.add(fscanResultsMap.get(nKey));
						}

						bundle.putString(ScanResultActivity.SCAN_IMAGE_RESULTS, mWorkingFolder.getAbsolutePath());
						uploadFile(sImage);
						/*Intent _intent = new Intent(getActivity(), ScanResultActivity.class);
						_intent.putExtras(bundle);
						startActivityForResult(_intent, REQUEST_SCANRESULTSHOW);*/
					}
					else
					{
						showDialogWithString(getString(R.string.error_failtoscan_with_status) + fScanJob.getStatus().toString(), getString(R.string.error_title));
					}
					
					if (mProgressDialog != null)
					{
						mProgressDialog.dismiss();
						mProgressDialog = null;
					}
				}
			});
		}
	}

	/**
	 *
	 * @param message
	 * @param title
	 */
	private void showDialogWithString(final String message, final String title)
	{
		DialogFragment fragment = new DialogFragment()
		{
			@Override public Dialog onCreateDialog(Bundle savedInstanceState)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				if (title != null)
				{
					builder.setTitle(title);
				}
				builder.setMessage(message);
				builder.setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener()
				{
					@Override public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				});
				return builder.create();
			}
		};
		fragment.show(getFragmentManager(), "ScanErrorDialogWithMessage");
	}

	/**
	 *
	 * @param messageID
	 * @param titleID
	 */
	private void showDialog(final int messageID, final int titleID)
	{
		DialogFragment fragment = new DialogFragment()
		{
			@Override public Dialog onCreateDialog(Bundle savedInstanceState)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				if (titleID > 0)
				{
					builder.setTitle(titleID);
				}
				builder.setMessage(messageID);
				builder.setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener()
				{
					@Override public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				});
				return builder.create();
			}
		};
		fragment.show(getFragmentManager(), "ScanErrorDialog");
	}

	/**
	 * 
	 * @return
	 */
	private ProgressDialog createProgressDialog()
	{
		ProgressDialog progressDialog = new ProgressDialog(getActivity());
		progressDialog.setMessage("Processing...");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);
		progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener()
		{
			@Override public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				new Thread(new Runnable()
				{
					public void run()
					{
						// [Brother Comment]
						// ScanJob can be cancelled with ScanJob.cancel() call.
						if (mScanJob != null)
						{
							mScanJob.cancel();
						}
					}
				}).start();
			}
		});
		return progressDialog;
	}

	/**
	 * 
	 */
	@Override public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_SCANRESULTSHOW:
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
				break;
		}
	}
}
