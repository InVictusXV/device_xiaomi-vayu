#
# Copyright (C) 2021 Android Open Source Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit device stuff.
$(call inherit-product, device/xiaomi/vayu/device.mk)

# Inherit common Nusantara stuff.
$(call inherit-product, vendor/nusantara/config/common_full_phone.mk)

TARGET_BOOT_ANIMATION_RES := 1080

#GApps
USE_GAPPS := true

# Pixel Charging
USE_PIXEL_CHARGING := true

# Pixel Charging
USE_PIXEL_CHARGING := true

# Device identifier. This must come after all inclusions.
PRODUCT_NAME := nad_vayu
PRODUCT_DEVICE := vayu
PRODUCT_BRAND := POCO
PRODUCT_MODEL := Poco X3 Pro
PRODUCT_MANUFACTURER := Xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    PRIVATE_BUILD_DESC="vayu_global-user 12 SKQ1.211006.001 V13.0.3.0.SJUMIXM release-keys" \
    PRODUCT_NAME="vayu"

BUILD_FINGERPRINT := POCO/vayu_global/vayu:12/SKQ1.211006.001/V13.0.3.0.SJUMIXM:user/release-keys

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi
