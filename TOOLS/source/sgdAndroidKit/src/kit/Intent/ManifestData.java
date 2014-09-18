package kit.Intent;

/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class containing the manifest info obtained during the parsing.
 */
public final class ManifestData {

	/**
	 * Value returned by {@link #getMinSdkVersion()} when the value of the
	 * minSdkVersion attribute in the manifest is a codename and not an integer
	 * value.
	 */
	public final static int MIN_SDK_CODENAME = 0;

	/**
	 * Value returned by {@link #getGlEsVersion()} when there are no
	 * <uses-feature> node with the attribute glEsVersion set.
	 */
	public final static int GL_ES_VERSION_NOT_SET = -1;
	Set<String> mProcesses = null;
	/** Application package */
	String mPackage;
	/** Application version Code, null if the attribute is not present. */
	Integer mVersionCode = null;
	/** List of all components */
	private final ArrayList<AndroidManifestComponent> mComponents = new ArrayList<AndroidManifestComponent>();

	/** debuggable attribute value. If null, the attribute is not present. */
	Boolean mDebuggable = null;
	/** API level requirement. if null the attribute was not present. */
	private String mMinSdkVersionString = null;
	/**
	 * API level requirement. Default is 1 even if missing. If value is a
	 * codename, then it'll be 0 instead.
	 */
	private int mMinSdkVersion = 1;
	private int mTargetSdkVersion = 0;
	/** List of all instrumentations declared by the manifest */
	final ArrayList<Instrumentation> mInstrumentations = new ArrayList<Instrumentation>();

	/** enabled attribute value of Application level */
	Boolean mEnabled = null;

	/** permission attribute value of Application level */
	String mPermission = null;

	/**
	 * Instrumentation info obtained from manifest
	 */
	public final static class Instrumentation {
		private final String mName;
		private final String mTargetPackage;

		Instrumentation(String name, String targetPackage) {
			mName = name;
			mTargetPackage = targetPackage;
		}

		/**
		 * Returns the fully qualified instrumentation class name
		 */
		public String getName() {
			return mName;
		}

		/**
		 * Returns the Android app package that is the target of this
		 * instrumentation
		 */
		public String getTargetPackage() {
			return mTargetPackage;
		}
	}

	/**
	 * Returns the package defined in the manifest, if found.
	 * 
	 * @return The package name or null if not found.
	 */
	public String getPackage() {
		return mPackage;
	}

	/**
	 * Returns the versionCode value defined in the manifest, if found, null
	 * otherwise.
	 * 
	 * @return the versionCode or null if not found.
	 */
	public Integer getVersionCode() {
		return mVersionCode;
	}

	/**
	 * Returns the <code>debuggable</code> attribute value or null if it is not
	 * set.
	 */
	public Boolean getDebuggable() {
		return mDebuggable;
	}

	/**
	 * Returns the <code>minSdkVersion</code> attribute, or null if it's not
	 * set.
	 */
	public String getMinSdkVersionString() {
		return mMinSdkVersionString;
	}

	/**
	 * Sets the value of the <code>minSdkVersion</code> attribute.
	 * 
	 * @param minSdkVersion
	 *            the string value of the attribute in the manifest.
	 */
	public void setMinSdkVersionString(String minSdkVersion) {
		mMinSdkVersionString = minSdkVersion;
		if (mMinSdkVersionString != null) {
			try {
				mMinSdkVersion = Integer.parseInt(mMinSdkVersionString);
			} catch (NumberFormatException e) {
				mMinSdkVersion = MIN_SDK_CODENAME;
			}
		}
	}

	/**
	 * Returns the <code>minSdkVersion</code> attribute, or 0 if it's not set or
	 * is a codename.
	 * 
	 * @see #getMinSdkVersionString()
	 */
	public int getMinSdkVersion() {
		return mMinSdkVersion;
	}

	/**
	 * Sets the value of the <code>minSdkVersion</code> attribute.
	 * 
	 * @param targetSdkVersion
	 *            the string value of the attribute in the manifest.
	 */
	public void setTargetSdkVersionString(String targetSdkVersion) {
		if (targetSdkVersion != null) {
			try {
				mTargetSdkVersion = Integer.parseInt(targetSdkVersion);
			} catch (NumberFormatException e) {
				// keep the value at 0.
			}
		}
	}

	/**
	 * Returns the <code>targetSdkVersion</code> attribute, or the same value as
	 * {@link #getMinSdkVersion()} if it was not set in the manifest.
	 */
	public int getTargetSdkVersion() {
		if (mTargetSdkVersion == 0) {
			return getMinSdkVersion();
		}

		return mTargetSdkVersion;
	}

	void setApplicationPermissionName(String permissionName) {
		mPermission = permissionName;
	}

	public ArrayList<AndroidManifestComponent> getComponents() {
		return mComponents;
	}

	/**
	 * return the component object from its name
	 * */
	public AndroidManifestComponent getComponents(String name) {
		AndroidManifestComponent component = null;
		for (AndroidManifestComponent comp : mComponents) {
			if (comp.getName().equals(name)) {
				component = comp;
			}
		}
		return component;

	}

	/**
	 * @return the component object from its name and type
	 * */
	public AndroidManifestComponent getComponents(String name, String type) {
		AndroidManifestComponent component = null;

		for (AndroidManifestComponent comp : mComponents) {
			if (comp.getName().equals(name)
					&& comp.getType().equalsIgnoreCase(type)) {
				component = comp;
			}
		}

		return component;
	}

	public void setNull() {
	}

	void addProcessName(String processName) {
		if (mProcesses == null) {
			mProcesses = new TreeSet<String>();
		}

		if (processName.startsWith(":")) {
			mProcesses.add(mPackage + processName);
		} else {
			mProcesses.add(processName);
		}
	}

	/**
	 * Returns the list of process names declared by the manifest.
	 */
	public String[] getProcesses() {
		if (mProcesses != null) {
			return mProcesses.toArray(new String[mProcesses.size()]);
		}

		return new String[0];
	}

	public void addComponent(AndroidManifestComponent comp) {
		if (comp != null)
			mComponents.add(comp);

	}

}