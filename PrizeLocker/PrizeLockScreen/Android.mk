ifeq ($(strip $(PRIZE_LOCKSCREEN_APP)),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 guava

LOCAL_PACKAGE_NAME := PrizeLockScreen

LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
