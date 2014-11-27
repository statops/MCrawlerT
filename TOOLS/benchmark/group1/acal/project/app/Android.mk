LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += \
	src/com/morphoss/acal/dataservice/DataRequestCallBack.aidl \
	src/com/morphoss/acal/dataservice/DataRequest.aidl \
	src/com/morphoss/acal/service/ServiceRequest.aidl \

LOCAL_PACKAGE_NAME := Acal
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
