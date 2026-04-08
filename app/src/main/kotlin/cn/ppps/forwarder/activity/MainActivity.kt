package cn.ppps.forwarder.activity

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.tabs.TabLayout
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import cn.ppps.forwarder.App
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.BaseActivity
import cn.ppps.forwarder.databinding.ActivityMainBinding
import cn.ppps.forwarder.fragment.LogsFragment
import cn.ppps.forwarder.fragment.RulesFragment
import cn.ppps.forwarder.fragment.SendersFragment
import cn.ppps.forwarder.fragment.SettingsFragment
import cn.ppps.forwarder.service.ForegroundService
import cn.ppps.forwarder.utils.ACTION_START
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.SmsOnlyMode
import cn.ppps.forwarder.utils.XToastUtils
import cn.ppps.forwarder.widget.GuideTipsDialog.Companion.showTips
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xutil.net.NetworkUtils

@Suppress("PrivatePropertyName", "unused", "DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding?>() {

    private val POS_LOG = 0
    private val POS_RULE = 1
    private val POS_SENDER = 2
    private val POS_SETTING = 3

    private lateinit var mTabLayout: TabLayout
    private var hasShownStartupTips = false

    override fun viewBindingInflate(inflater: LayoutInflater?): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initData()
        initViews()

        //不在最近任务列表中显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && SettingUtils.enableExcludeFromRecents) {
            val am = App.context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.let {
                val tasks = it.appTasks
                if (!tasks.isNullOrEmpty()) {
                    tasks[0].setExcludeFromRecents(true)
                }
            }
        }

        //检查通知权限是否获取
        val permissionRequest = XXPermissions.with(this)
        if (!SmsOnlyMode.isEnabled) {
            permissionRequest.permission(PermissionLists.getNotificationServicePermission())
        }
        permissionRequest
            .permission(PermissionLists.getPostNotificationsPermission())
            .request(object : OnPermissionCallback {
                override fun onResult(grantedList: MutableList<IPermission>, deniedList: MutableList<IPermission>) {
                    val allGranted = deniedList.isEmpty()
                    if (!allGranted) {
                        XToastUtils.error(R.string.tips_notification)
                        return
                    }
                    //启动前台服务
                    if (!ForegroundService.isRunning) {
                        val serviceIntent = Intent(getTopActivity(), ForegroundService::class.java)
                        serviceIntent.action = ACTION_START
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    }
                }
            })
    }

    override val isSupportSlideBack: Boolean
        get() = false

    private fun initViews() {
        WidgetUtils.clearActivityBackground(this)
        initTab()
    }

    private fun initTab() {
        mTabLayout = binding!!.tabs
        WidgetUtils.addTabWithoutRipple(mTabLayout, getString(R.string.menu_logs), R.drawable.selector_icon_tabbar_logs)
        WidgetUtils.addTabWithoutRipple(mTabLayout, getString(R.string.menu_rules), R.drawable.selector_icon_tabbar_rules)
        WidgetUtils.addTabWithoutRipple(mTabLayout, getString(R.string.menu_senders), R.drawable.selector_icon_tabbar_senders)
        WidgetUtils.addTabWithoutRipple(mTabLayout, getString(R.string.menu_settings), R.drawable.selector_icon_tabbar_settings)
        WidgetUtils.setTabLayoutTextFont(mTabLayout)
        switchPage(SettingsFragment::class.java)
        mTabLayout.getTabAt(POS_SETTING)?.select()
        mTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    POS_LOG -> switchPage(LogsFragment::class.java)
                    POS_RULE -> switchPage(RulesFragment::class.java)
                    POS_SENDER -> switchPage(SendersFragment::class.java)
                    POS_SETTING -> switchPage(SettingsFragment::class.java)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun initData() {
        // 短信专版固定显示本地 tips，不进行任何联网更新检查
        if (!hasShownStartupTips) {
            hasShownStartupTips = true
            showTips(this)
        }
    }

    //按返回键不退出回到桌面
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
    }
}
