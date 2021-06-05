package com.example.androidsdksampleapp;

import java.io.File;

import com.brother.sdk.common.IConnector;

public interface MainActivityContext
{
	public void onConnectorSelected(IConnector connector);

	public IConnector getConnector();

	public File getWorkingFolder();
}
