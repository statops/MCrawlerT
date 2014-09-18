package kit.Intent;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class AndroidManifestParser {

	private final static int LEVEL_TOP = 0;
	private final static int LEVEL_INSIDE_MANIFEST = 1;
	private final static int LEVEL_INSIDE_APPLICATION = 2;
	private final static int LEVEL_INSIDE_APP_COMPONENT = 3;
	private final static int LEVEL_INSIDE_INTENT_FILTER = 4;

	public interface ManifestErrorHandler extends ErrorHandler {
		/**
		 * Handles a parsing error and an optional line number.
		 * 
		 * @param exception
		 * @param lineNumber
		 */
		void handleError(Exception exception, int lineNumber);

		/**
		 * Checks that a class is valid and can be used in the Android Manifest.
		 * <p/>
		 * Errors are put as {@link IMarker} on the manifest file.
		 * 
		 * @param locator
		 * @param className
		 *            the fully qualified name of the class to test.
		 * @param superClassName
		 *            the fully qualified name of the class it is supposed to
		 *            extend.
		 * @param testVisibility
		 *            if <code>true</code>, the method will check the visibility
		 *            of the class or of its constructors.
		 */
		void checkClass(Locator locator, String className,
				String superClassName, boolean testVisibility);
	}

	/**
	 * XML error & data handler used when parsing the AndroidManifest.xml file.
	 * <p/>
	 * During parsing this will fill up the {@link ManifestData} object given to
	 * the constructor and call out errors to the given
	 * {@link ManifestErrorHandler}.
	 */
	private static class ManifestHandler extends DefaultHandler {

		private static final String CLASS_BROADCASTRECEIVER = "android.content.BroadcastReceiver";
		private static final String CLASS_CONTENTPROVIDER = "android.content.ContentProvider";
		private static final String CLASS_ACTIVITY = "android.app.Activity";
		private static final Object NS_RESOURCES = "http://schemas.android.com/apk/res/android";

		private final ManifestData mManifestData;
		private final ManifestErrorHandler mErrorHandler;
		private int mCurrentLevel = 0;
		private int mValidLevel = 0;
		private AndroidManifestComponent mComponent = null;
		private MCrawlerTIntent mCurrentIntent = null;
		private String mCurrentType = null;
		private Locator mLocator;

		/**
		 * Creates a new {@link ManifestHandler}.
		 * 
		 * @param manifestFile
		 *            The manifest file being parsed. Can be null.
		 * @param errorListener
		 *            An optional error listener.
		 * @param gatherData
		 *            True if data should be gathered.
		 * @param javaProject
		 *            The java project holding the manifest file. Can be null.
		 * @param markErrors
		 *            True if errors should be marked as Eclipse Markers on the
		 *            resource.
		 */
		ManifestHandler(IAbstractFile manifestFile, ManifestData manifestData,
				ManifestErrorHandler errorHandler) {
			super();
			mManifestData = manifestData;
			mErrorHandler = errorHandler;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax
		 * .Locator)
		 */
		@Override
		public void setDocumentLocator(Locator locator) {
			mLocator = locator;
			super.setDocumentLocator(locator);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
		 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			try {
				if (mManifestData == null) {
					return;
				}

				// if we're at a valid level
				if (mValidLevel == mCurrentLevel) {
					String value;
					switch (mValidLevel) {
					case LEVEL_TOP:

						if (AndroidManifest.NODE_MANIFEST.equals(localName)) {
							// lets get the package name.
							mManifestData.mPackage = getAttributeValue(
									attributes,
									AndroidManifest.ATTRIBUTE_PACKAGE, false /* hasNamespace */);

							// and the versionCode
							String tmp = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_VERSIONCODE, true);
							if (tmp != null) {
								try {
									mManifestData.mVersionCode = Integer
											.valueOf(tmp);
								} catch (NumberFormatException e) {
									// keep null in the field.
								}
							}
							mValidLevel++;
						}
						break;
					case LEVEL_INSIDE_MANIFEST:
						/*
						 * System.out.println("localname  : " +
						 * LEVEL_INSIDE_MANIFEST + "  " + localName + "\n");
						 */
						if (AndroidManifest.NODE_APPLICATION.equals(localName)) {
							value = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_PROCESS, true /* hasNamespace */);
							if (value != null) {
								mManifestData.addProcessName(value);
							}

							value = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_DEBUGGABLE, true /* hasNamespace */);
							if (value != null) {
								mManifestData.mDebuggable = Boolean
										.parseBoolean(value);
							}
							value = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_ENABLED, true /* hasNamespace */);
							if (value != null) {
								mManifestData.mEnabled = Boolean
										.parseBoolean(value);
							}
							value = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_PERMISSION, true /* hasNamespace */);
							if (value != null) {
								mManifestData
										.setApplicationPermissionName(value);
							}

							mValidLevel++;
						}
						break;
					case LEVEL_INSIDE_APPLICATION:
						/*
						 * System.out.println("localname  : " +
						 * LEVEL_INSIDE_APPLICATION + "  " + localName + "\n");
						 */
						if (AndroidManifest.NODE_ACTIVITY.equals(localName)) {
							mCurrentType = AndroidManifest.NODE_ACTIVITY;
							processActivity_Service_Node(attributes);

							mValidLevel++;
						} else if (AndroidManifest.NODE_SERVICE
								.equals(localName)) {

							mCurrentType = AndroidManifest.NODE_SERVICE;
							processActivity_Service_Node(attributes);
							mValidLevel++;
						} else if (AndroidManifest.NODE_RECEIVER
								.equals(localName)) {
							mCurrentType = AndroidManifest.NODE_RECEIVER;
							processReceiverNode(attributes);
							mValidLevel++;
						} else if (AndroidManifest.NODE_PROVIDER
								.equals(localName)) {
							mCurrentType = AndroidManifest.NODE_PROVIDER;
							processProviderNode(attributes);
							mValidLevel++;
						}
						break;
					case LEVEL_INSIDE_APP_COMPONENT:
						/**
						 * only process this level if we are in an activity or
						 * in a service
						 */
						if (mComponent != null
								&& AndroidManifest.NODE_INTENT
										.equals(localName)) {
							mComponent.setHasIntentFilter(true);
							mValidLevel++;
						}
						break;
					case LEVEL_INSIDE_INTENT_FILTER:
						if (mCurrentIntent == null) {
							mCurrentIntent = new MCrawlerTIntent();
						}
						if (AndroidManifest.NODE_ACTION.equals(localName)) {
							String action = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_NAME, true /* hasNamespace */);

							/**
							 * each action must represent one intent
							 */
							mCurrentIntent.addAction(action);
							mCurrentIntent.setComponentName(mComponent
									.getName());
						}
						if (AndroidManifest.NODE_CATEGORY.equals(localName)) {
							/**
							 * Add category to the Intent
							 */
							String category = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_NAME, true /* hasNamespace */);
							mCurrentIntent.addCategory(category);
						}
						if (AndroidManifest.NODE_DATA.equals(localName)) {
							/**
							 * Add data to the Intent
							 */
							String host = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_HOST, true);
							String scheme = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_SCHEME, true);
							String path = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_PATH, true);
							String mimetype = getAttributeValue(attributes,
									AndroidManifest.ATTRIBUTE_MIMETYPE, true);
							MCrawlerTIntent.IntentData data = new MCrawlerTIntent.IntentData(
									host, scheme, mimetype, path);
							mCurrentIntent.setIntentData(data);
						}

						break;
					}
				}

				mCurrentLevel++;
			} finally {
				super.startElement(uri, localName, name, attributes);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			try {
				if (mManifestData == null) {
					return;
				}

				// decrement the levels.
				if (mValidLevel == mCurrentLevel) {
					mValidLevel--;
				}
				mCurrentLevel--;

				// if we're at a valid level
				// process the end of the element
				if (mValidLevel == mCurrentLevel) {
					switch (mValidLevel) {
					case LEVEL_INSIDE_APPLICATION:
						mManifestData.addComponent(mComponent);
						break;
					case LEVEL_INSIDE_APP_COMPONENT:
						if (mComponent != null) {
							mComponent.setIntent(mCurrentIntent);
						}
						mCurrentIntent = null;
					default:
						break;
					}

				}
			} finally {
				super.endElement(uri, localName, name);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException
		 * )
		 */
		@Override
		public void error(SAXParseException e) {
			if (mErrorHandler != null) {
				mErrorHandler.handleError(e, e.getLineNumber());

			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.
		 * SAXParseException)
		 */
		@Override
		public void fatalError(SAXParseException e) {
			if (mErrorHandler != null) {
				mErrorHandler.handleError(e, e.getLineNumber());
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException
		 * )
		 */
		@Override
		public void warning(SAXParseException e) throws SAXException {
			if (mErrorHandler != null) {
				mErrorHandler.warning(e);
			}
		}

		/**
		 * Processes the activity node.
		 * 
		 * @param attributes
		 *            the attributes for the activity node.
		 */
		private void processActivity_Service_Node(Attributes attributes) {
			// lets get the activity name, and add it to the list
			String activityName = getAttributeValue(attributes,
					AndroidManifest.ATTRIBUTE_NAME, true /* hasNamespace */);
			if (activityName != null) {
				activityName = AndroidManifest.combinePackageAndClassName(
						mManifestData.mPackage, activityName);
				// get the exported flag.
				String exportedStr = getAttributeValue(attributes,
						AndroidManifest.ATTRIBUTE_EXPORTED, true);
				boolean exported = (exportedStr != null)
						&& ((exportedStr.equalsIgnoreCase("true")));
				mComponent = new AndroidManifestComponent(activityName,
						exported, mCurrentType, mManifestData.getPackage());
				if (mErrorHandler != null) {
					mErrorHandler.checkClass(mLocator, activityName,
							CLASS_ACTIVITY, true /* testVisibility */);
				}
			} else {
				mComponent = null;
			}
			addProcess(attributes);
		}

		/**
		 * Process the provider node
		 * 
		 * @param attributes
		 *            the attributes for the provider node.
		 */
		private void processProviderNode(Attributes attributes) {

			String providerName = getAttributeValue(attributes,
					AndroidManifest.ATTRIBUTE_NAME, true /* hasNamespace */);
			if (providerName != null) {
				providerName = AndroidManifest.combinePackageAndClassName(
						mManifestData.mPackage, providerName);
				mComponent = new AndroidManifestComponent(providerName,
						mCurrentType, mManifestData.getPackage());
				// get the exported flag.
				String authorities = getAttributeValue(attributes,
						AndroidManifest.ATTRIBUTE_AUTHORITIES, true);
				mComponent.setProperties(AndroidManifest.ATTRIBUTE_AUTHORITIES,
						authorities);
				if (mErrorHandler != null) {
					mErrorHandler.checkClass(mLocator, providerName,
							CLASS_CONTENTPROVIDER, true /* testVisibility */);
				}
			} else {
				mComponent = null;
			}
			addProcess(attributes);
		}

		/**
		 * Process the receiver node
		 * 
		 * @param attributes
		 *            the attributes for the receiver node.
		 */
		private void processReceiverNode(Attributes attributes) {

			String receiverName = getAttributeValue(attributes,
					AndroidManifest.ATTRIBUTE_NAME, true /* hasNamespace */);
			if (receiverName != null) {
				receiverName = AndroidManifest.combinePackageAndClassName(
						mManifestData.mPackage, receiverName);

				mComponent = new AndroidManifestComponent(receiverName,
						mCurrentType, mManifestData.getPackage());

				if (mErrorHandler != null) {
					mErrorHandler.checkClass(mLocator, receiverName,
							CLASS_BROADCASTRECEIVER, true /* testVisibility */);
				}
			} else {

				mComponent = null;
			}

			addProcess(attributes);
		}

		private void addProcess(Attributes attributes) {
			String processName = getAttributeValue(attributes,
					AndroidManifest.ATTRIBUTE_PROCESS, true /* hasNamespace */);
			if (processName != null) {
				mManifestData.addProcessName(processName);
			}

		}

		/**
		 * Searches through the attributes list for a particular one and returns
		 * its value.
		 * 
		 * @param attributes
		 *            the attribute list to search through
		 * @param attributeName
		 *            the name of the attribute to look for.
		 * @param hasNamespace
		 *            Indicates whether the attribute has an android namespace.
		 * @return a String with the value or null if the attribute was not
		 *         found.
		 * @see SdkConstants#NS_RESOURCES
		 */
		private String getAttributeValue(Attributes attributes,
				String attributeName, boolean hasNamespace) {
			int count = attributes.getLength();
			for (int i = 0; i < count; i++) {
				if (attributeName.equals(attributes.getLocalName(i))
						&& ((hasNamespace && NS_RESOURCES.equals(attributes
								.getURI(i))) || (hasNamespace == false && attributes
								.getURI(i).length() == 0))) {
					return attributes.getValue(i);
				}
			}

			return null;
		}

	}

	private final static SAXParserFactory sParserFactory;

	static {
		sParserFactory = SAXParserFactory.newInstance();
		sParserFactory.setNamespaceAware(true);
	}

	/**
	 * Parses the Android Manifest, and returns a {@link ManifestData} object
	 * containing the result of the parsing.
	 * 
	 * @param manifestFile
	 *            the {@link IAbstractFile} representing the manifest file.
	 * @param gatherData
	 *            indicates whether the parsing will extract data from the
	 *            manifest. If false the method will always return null.
	 * @param errorHandler
	 *            an optional errorHandler.
	 * @return
	 * @throws StreamException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static ManifestData parse(IAbstractFile manifestFile,
			boolean gatherData, ManifestErrorHandler errorHandler)
			throws SAXException, IOException, StreamException,
			ParserConfigurationException {
		if (manifestFile != null) {
			SAXParser parser = sParserFactory.newSAXParser();

			ManifestData data = null;
			if (gatherData) {
				data = new ManifestData();
			}

			ManifestHandler manifestHandler = new ManifestHandler(manifestFile,
					data, errorHandler);
			parser.parse(new InputSource(manifestFile.getContents()),
					manifestHandler);

			return data;
		}

		return null;
	}

	/**
	 * Parses the Android Manifest, and returns an object containing the result
	 * of the parsing.
	 * 
	 * <p/>
	 * This is the equivalent of calling
	 * 
	 * <pre>
	 * parse(manifestFile, true, null)
	 * </pre>
	 * 
	 * @param manifestFile
	 *            the manifest file to parse.
	 * @throws ParserConfigurationException
	 * @throws StreamException
	 * @throws IOException
	 * @throws SAXException
	 */
	public static ManifestData parse(IAbstractFile manifestFile)
			throws SAXException, IOException, StreamException,
			ParserConfigurationException {
		return parse(manifestFile, true, null);
	}

	public static ManifestData parse(IAbstractFolder projectFolder)
			throws SAXException, IOException, StreamException,
			ParserConfigurationException {
		IAbstractFile manifestFile = AndroidManifest.getManifest(projectFolder);
		if (manifestFile == null) {
			throw new FileNotFoundException();
		}

		return parse(manifestFile, true, null);
	}

	/**
	 * Parses the Android Manifest from an {@link InputStream}, and returns a
	 * {@link ManifestData} object containing the result of the parsing.
	 * 
	 * @param manifestFileStream
	 *            the {@link InputStream} representing the manifest file.
	 * @return
	 * @throws StreamException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static ManifestData parse(InputStream manifestFileStream)
			throws SAXException, IOException, StreamException,
			ParserConfigurationException {
		if (manifestFileStream != null) {
			SAXParser parser = sParserFactory.newSAXParser();

			ManifestData data = new ManifestData();

			ManifestHandler manifestHandler = new ManifestHandler(null, data,
					null);

			parser.parse(new InputSource(manifestFileStream), manifestHandler);

			return data;
		}

		return null;
	}
}
