package com.example.androidsdksampleapp;

import java.io.File;

import androidx.fragment.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ScanImageViewFragment extends Fragment implements View.OnTouchListener
{
	public static final String ARG_PAGE = "page";
	public static final String ARG_PATH = "image";

	private int mPageNumber;
	private File mFile;

	public static ScanImageViewFragment create(int pageNumber, File previewPath)
	{

		ScanImageViewFragment fragment = new ScanImageViewFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_PAGE, pageNumber);
		args.putString(ARG_PATH, previewPath.getAbsolutePath());
		fragment.setArguments(args);
		return fragment;
	}

	public ScanImageViewFragment()
	{
	}

	public static Fragment create() {
		ScanImageViewFragment fragment = new ScanImageViewFragment();

		return fragment;
	}

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
//		mPageNumber = getArguments().getInt("12");
		//mFile = new File(getArguments().getString("/storage/"));
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate the layout containing a title and body text.
		ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.brother_ui_fragment_preview_panel, container, false);

		// Set the title view to show the page number.
		TextView tv = (TextView)rootView.findViewById(R.id.brother_ui_tv_preview_pagetitle);
		tv.setText(getString(R.string.brother_ui_str_preview_title, mPageNumber + 1));

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = false;
		options.inSampleSize = 1;
//		Bitmap bmp = BitmapFactory.decodeFile(mFile.getPath(), options);

		ImageView iv = (ImageView)rootView.findViewById(R.id.brother_ui_iv_preview_image);
		//iv.setImageBitmap(bmp);

		return rootView;
	}

	/**
	 * Returns the page number represented by this fragment object.
	 */
	public int getPageNumber()
	{
		return mPageNumber;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return false;
	}
}
