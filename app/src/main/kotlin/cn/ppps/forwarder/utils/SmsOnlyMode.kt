package cn.ppps.forwarder.utils

object SmsOnlyMode {
    const val isEnabled = true

    fun enforceLockedConfig() {
        if (!isEnabled) return

        SettingUtils.autoCheckUpdate = false
        SettingUtils.enableSms = true
        SettingUtils.enablePhone = false
        SettingUtils.enableCallType1 = false
        SettingUtils.enableCallType2 = false
        SettingUtils.enableCallType3 = false
        SettingUtils.enableCallType4 = false
        SettingUtils.enableCallType5 = false
        SettingUtils.enableCallType6 = false
        SettingUtils.enableAppNotify = false
        SettingUtils.enableSmsCommand = false
        SettingUtils.enableCloseToEarpieceTurnOffScreen = false
        SettingUtils.enableCancelAppNotify = false
        SettingUtils.cancelExtraAppNotify = ""
        SettingUtils.enableNotUserPresent = false
        SettingUtils.enableLoadAppList = false
        SettingUtils.enableLoadUserAppList = false
        SettingUtils.enableLoadSystemAppList = false
        SettingUtils.enablePureClientMode = false
        SettingUtils.enablePureTaskMode = false
        SettingUtils.enableLocation = false
        SettingUtils.enableBluetooth = false

        HttpServerUtils.enableServerAutorun = false
        HttpServerUtils.enableApiClone = false
        HttpServerUtils.enableApiSmsSend = false
        HttpServerUtils.enableApiSmsQuery = false
        HttpServerUtils.enableApiCallQuery = false
        HttpServerUtils.enableApiContactQuery = false
        HttpServerUtils.enableApiContactAdd = false
        HttpServerUtils.enableApiBatteryQuery = false
        HttpServerUtils.enableApiWol = false
        HttpServerUtils.enableApiLocation = false
    }
}
